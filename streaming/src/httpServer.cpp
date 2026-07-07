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
        if (write(client_fd, response.data(), response.size()) < 0) 
            perror("Erreur d'écriture HTTP");
        close(client_fd);
        return;
    }
    
    if (!is_path_safe(url)) {
        std::string response = "HTTP/1.1 403 Forbidden\r\nContent-Length: 0\r\n\r\n";
        if (write(client_fd, response.data(), response.size()) < 0) 
            perror("Erreur d'écriture HTTP");
        close(client_fd);
        return;
    }

    std::string ticket = extract_ticket(url);
    
    std::string requested_movie_id = "sintel"; 
    size_t video_pos = url.find("/video/");
    if (video_pos != std::string::npos) {
        std::string sub = url.substr(video_pos + 7);
        size_t slash_pos = sub.find('/');
        if (slash_pos != std::string::npos) {
            requested_movie_id = sub.substr(0, slash_pos);
        }
    }

    std::string client_ip = "127.0.0.1"; 

    if (!verify_streaming_ticket(ticket, client_ip, requested_movie_id)) {
        std::cout << "[ACCESS DENIED] Requête rejetée pour le film : " << requested_movie_id << " (Ticket absent ou corrompu)" << std::endl;
        std::string response = "HTTP/1.1 403 Forbidden\r\nContent-Length: 0\r\n\r\n";
        if (write(client_fd, response.data(), response.size()) < 0) 
            perror("Erreur d'écriture HTTP");
        close(client_fd);
        return;
    }

    std::string clean_url = url;
    size_t q_pos = url.find('?');
    if (q_pos != std::string::npos) {
        clean_url = url.substr(0, q_pos); 
    }

    std::string relative_path = clean_url;
    if (clean_url.rfind("/video", 0) == 0) {
        relative_path = clean_url.substr(6); 
    }
    std::string file_path = "./videos" + relative_path;

    
    int file_fd = open(file_path.c_str(), O_RDONLY);
    if (file_fd == -1) {
        std::cerr << "[HTTP 404] Fichier introuvable : " << file_path << std::endl;
        std::string response = "HTTP/1.1 404 Not Found\r\nContent-Length: 0\r\n\r\n";
        if (write(client_fd, response.data(), response.size()) < 0) 
            perror("Erreur d'écriture HTTP");
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
    if (write(client_fd, headers.data(), headers.size()) < 0) 
        perror("Erreur d'écriture HTTP headers");
    
    off_t offset = 0;
    bool send_success = true;

    while (offset < file_stat.st_size) {
        ssize_t sent = sendfile(client_fd, file_fd, &offset, file_stat.st_size - offset);
        
        if (sent <= 0) {
            if (errno == EAGAIN || errno == EWOULDBLOCK) {
        
                usleep(2000); 
                continue;
            } else {
                perror("Erreur critique sendfile");
                send_success = false;
                break;
            }
        }
    }

    if (send_success) {
        std::cout << "[Streaming] Fichier COMPLET envoyé (" << offset << " octets) : " << file_path << std::endl;
    }

    close(file_fd);
    close(client_fd);
}

