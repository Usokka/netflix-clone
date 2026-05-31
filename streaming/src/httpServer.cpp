#include "httpServer.hpp"
#include "utils.hpp"
#include <iostream>
#include <sys/socket.h>
#include <netinet/in.h>
#include <unistd.h>
#include <fcntl.h>
#include <cstring>
#include <vector>
#include <errno.h>
#include <sstream>
#include <sys/stat.h>
#include <sys/sendfile.h>

extern bool make_socket_non_blocking(int sfd);

void handle_new_connection(int server_fd, int epoll_fd) {
    while (true) {
        sockaddr_in in_addr{};
        socklen_t in_len = sizeof(in_addr);
        int client_fd = accept(server_fd, (struct sockaddr*)&in_addr, &in_len);
        
        if (client_fd == -1) {
            if ((errno == EAGAIN) || (errno == EWOULDBLOCK)) {
                break; 
            } else {
                perror("accept");
                break;
            }
        }

        if (!make_socket_non_blocking(client_fd)) {
            close(client_fd);
            continue;
        }

        epoll_event ev{};
        ev.events = EPOLLIN | EPOLLET | EPOLLONESHOT;
        ev.data.fd = client_fd;
        
        if (epoll_ctl(epoll_fd, EPOLL_CTL_ADD, client_fd, &ev) == -1) {
            perror("epoll_ctl ADD client");
            close(client_fd);
        }
    }
}

void handle_client_request(int client_fd, [[maybe_unused]] int epoll_fd) {
    std::vector<char> buffer(1024);
    std::string request_str;
    
    while (true) {
        ssize_t count = read(client_fd, buffer.data(), buffer.size());
        if (count == -1) {
            if (errno == EAGAIN || errno == EWOULDBLOCK) {
                break;
            }
            perror("read error");
            close(client_fd);
            return;
        } else if (count == 0) {
            close(client_fd);
            return;
        }
        request_str.append(buffer.data(), count);
    }

    std::istringstream request_stream(request_str);
    std::string method, url, version;
    request_stream >> method >> url >> version;

    
    if (method != "GET") {
        std::string response = "HTTP/1.1 405 Method Not Allowed\r\nContent-Length: 0\r\n\r\n";
        write(client_fd, response.data(), response.size());
        close(client_fd);
        return;
    }
    
    if (!is_path_safe(url)) {
        std::cerr << "[Security Warning] Tentative de Directory Traversal bloquée : " << url << std::endl;
        std::string response = "HTTP/1.1 403 Forbidden\r\nContent-Length: 0\r\n\r\n";
        write(client_fd, response.data(), response.size());
        close(client_fd);
        return;
    }

    std::string relative_path = url;
    if (url.rfind("/video", 0) == 0) {
        relative_path = url.substr(6); 
    }

    std::string file_path = "./videos" + relative_path;

    
    int file_fd = open(file_path.c_str(), O_RDONLY);
    if (file_fd == -1) {
        std::cerr << "[HTTP 404] Fichier introuvable : " << file_path << std::endl;
        std::string response = "HTTP/1.1 404 Not Found\r\nContent-Length: 0\r\n\r\n";
        write(client_fd, response.data(), response.size());
        close(client_fd);
        return;
    }

    struct stat file_stat;
    if (fstat(file_fd, &file_stat) == -1) {
        perror("fstat");
        close(file_fd);
        close(client_fd);
        return;
    }

    std::string content_type = get_content_type(file_path);
    std::ostringstream header_stream;
    header_stream << "HTTP/1.1 200 OK\r\n"
                  << "Content-Length: " << file_stat.st_size << "\r\n"
                  << "Content-Type: " << content_type << "\r\n"
                  << "Access-Control-Allow-Origin: *\r\n" 
                  << "Connection: close\r\n\r\n";
    
    std::string headers = header_stream.str();
    write(client_fd, headers.data(), headers.size());

    off_t offset = 0;
    ssize_t bytes_sent = sendfile(client_fd, file_fd, &offset, file_stat.st_size);
    if (bytes_sent == -1) {
        if (errno != EAGAIN && errno != EWOULDBLOCK) {
            perror("sendfile error");
        }
    } else {
        std::cout << "[Streaming] " << bytes_sent << " octets envoyés avec succès pour : " << file_path << std::endl;
    }

    close(file_fd);
    close(client_fd);
}