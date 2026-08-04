#include "httpServer.hpp"
#include "utils.hpp"

#include <algorithm>
#include <cerrno>
#include <condition_variable>
#include <cstdio>
#include <csignal>
#include <cstdlib>
#include <iostream>
#include <mutex>
#include <netinet/in.h>
#include <queue>
#include <sys/socket.h>
#include <thread>
#include <unistd.h>
#include <vector>

namespace {
constexpr int PORT = 8081;
constexpr std::size_t MAX_PENDING_CONNECTIONS = 1024;

class ConnectionPool {
public:
    explicit ConnectionPool(unsigned int worker_count) {
        workers_.reserve(worker_count);
        for (unsigned int index = 0; index < worker_count; ++index) {
            workers_.emplace_back([this] { run_worker(); });
        }
    }

    ~ConnectionPool() {
        {
            std::lock_guard lock(mutex_);
            stopping_ = true;
        }
        condition_.notify_all();
        for (std::thread& worker : workers_) {
            worker.join();
        }
    }

    bool submit(int client_fd) {
        {
            std::lock_guard lock(mutex_);
            if (stopping_ || connections_.size() >= MAX_PENDING_CONNECTIONS) {
                return false;
            }
            connections_.push(client_fd);
        }
        condition_.notify_one();
        return true;
    }

private:
    void run_worker() {
        while (true) {
            int client_fd = -1;
            {
                std::unique_lock lock(mutex_);
                condition_.wait(lock, [this] {
                    return stopping_ || !connections_.empty();
                });
                if (stopping_ && connections_.empty()) return;
                client_fd = connections_.front();
                connections_.pop();
            }

            handle_client_request(client_fd);
        }
    }

    std::vector<std::thread> workers_;
    std::queue<int> connections_;
    std::mutex mutex_;
    std::condition_variable condition_;
    bool stopping_ = false;
};

void reject_overloaded_connection(int client_fd) {
    constexpr char response[] =
        "HTTP/1.1 503 Service Unavailable\r\n"
        "Content-Length: 0\r\n"
        "Connection: close\r\n\r\n";
    send(client_fd, response, sizeof(response) - 1, MSG_NOSIGNAL);
    close(client_fd);
}
}

int main() {
    std::signal(SIGPIPE, SIG_IGN);

    const char* configured_key_path = std::getenv("STREAMING_PUBLIC_KEY_PATH");
    const std::string key_path = configured_key_path != nullptr
        ? configured_key_path
        : "/run/secrets/jwt_public_key";
    if (!initialize_ticket_verifier(key_path)) {
        return 1;
    }

    const char* configured_video_root = std::getenv("VIDEO_ROOT");
    const std::string video_root = configured_video_root != nullptr
        ? configured_video_root
        : "/app/videos";
    if (!initialize_video_root(video_root)) {
        std::cerr << "[Streaming Engine] Invalid video root: " << video_root << std::endl;
        return 1;
    }

    int server_fd = socket(AF_INET, SOCK_STREAM, 0);
    if (server_fd == -1) {
        std::perror("socket");
        return 1;
    }

    int reuse_address = 1;
    if (setsockopt(server_fd, SOL_SOCKET, SO_REUSEADDR, &reuse_address, sizeof(reuse_address)) == -1) {
        std::perror("setsockopt");
        close(server_fd);
        return 1;
    }

    sockaddr_in address{};
    address.sin_family = AF_INET;
    address.sin_addr.s_addr = INADDR_ANY;
    address.sin_port = htons(PORT);

    if (bind(server_fd, reinterpret_cast<sockaddr*>(&address), sizeof(address)) == -1) {
        std::perror("bind");
        close(server_fd);
        return 1;
    }

    if (listen(server_fd, SOMAXCONN) == -1) {
        std::perror("listen");
        close(server_fd);
        return 1;
    }

    const unsigned int hardware_threads = std::thread::hardware_concurrency();
    const unsigned int worker_count = std::clamp(hardware_threads == 0 ? 4U : hardware_threads, 4U, 32U);
    ConnectionPool pool(worker_count);
    std::cout << "[Streaming Engine] Listening on port " << PORT
              << " with " << worker_count << " workers" << std::endl;

    while (true) {
        int client_fd = accept(server_fd, nullptr, nullptr);
        if (client_fd == -1) {
            if (errno == EINTR) continue;
            std::perror("accept");
            break;
        }

        if (!pool.submit(client_fd)) {
            reject_overloaded_connection(client_fd);
        }
    }

    close(server_fd);
    return 1;
}
