#include "httpServer.hpp"
#include <iostream>
#include <sys/socket.h>
#include <netinet/in.h>
#include <unistd.h>
#include <fcntl.h>
#include <cstring>
#include <vector>
#include <errno.h>


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

        std::cout << "[Network] Nouveau client connecté, FD: " << client_fd << std::endl;

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

void handle_client_request(int client_fd,[[maybe_unused]] int epoll_fd) {
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
            
            std::cout << "[Network] Client déconnecté, FD: " << client_fd << std::endl;
            close(client_fd);
            return;
        }

        request_str.append(buffer.data(), count);
    }

    
    std::cout << "\n--- Requête HTTP reçue (FD: " << client_fd << ") ---\n" 
              << request_str 
              << "----------------------------------------\n" << std::endl;

    std::string response = "HTTP/1.1 200 OK\r\nContent-Length: 22\r\nContent-Type: text/plain\r\n\r\nStreaming Engine Test\n";
    write(client_fd, response.data(), response.size());

    close(client_fd);
}