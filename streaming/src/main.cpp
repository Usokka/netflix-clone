#include "httpServer.hpp"
#include <cstring>
#include <fcntl.h>
#include <iostream>
#include <netinet/in.h>
#include <sys/epoll.h>
#include <sys/socket.h>
#include <unistd.h>
#include <vector>
const int PORT = 8081;
const int MAX_EVENTS = 64;

bool make_socket_non_blocking(int sfd) {
  int flags = fcntl(sfd, F_GETFL, 0);
  if (flags == -1) {
    perror("fcntl F_GETFL");
    return false;
  }
  flags |= O_NONBLOCK;
  if (fcntl(sfd, F_SETFL, flags) == -1) {
    perror("fcntl F_SETFL");
    return false;
  }
  return true;
}

int main() {
  std::cout << "[Streaming Engine] Initialisation du serveur sur le port "
            << PORT << "..." << std::endl;

  int server_fd = socket(AF_INET, SOCK_STREAM, 0);
  if (server_fd == -1) {
    perror("Erreur création socket");
    return 1;
  }

  int opt = 1;
  setsockopt(server_fd, SOL_SOCKET, SO_REUSEADDR, &opt, sizeof(opt));

  sockaddr_in address{};
  address.sin_family = AF_INET;
  address.sin_addr.s_addr = INADDR_ANY;
  address.sin_port = htons(PORT);

  if (bind(server_fd, (struct sockaddr *)&address, sizeof(address)) < 0) {
    perror("Erreur bind");
    close(server_fd);
    return 1;
  }

  if (listen(server_fd, SOMAXCONN) < 0) {
    perror("Erreur listen");
    close(server_fd);
    return 1;
  }

  if (!make_socket_non_blocking(server_fd)) {
    close(server_fd);
    return 1;
  }

  int epoll_fd = epoll_create1(0);
  if (epoll_fd == -1) {
    perror("Erreur epoll_create1");
    close(server_fd);
    return 1;
  }

  epoll_event ev{};
  ev.events = EPOLLIN | EPOLLET;
  ev.data.fd = server_fd;

  if (epoll_ctl(epoll_fd, EPOLL_CTL_ADD, server_fd, &ev) == -1) {
    perror("Erreur epoll_ctl ADD");
    close(server_fd);
    close(epoll_fd);
    return 1;
  }

  std::cout << "[Streaming Engine] Serveur d'écoute prêt. En attente de "
               "connexions (Boucle epoll)..."
            << std::endl;

  std::vector<epoll_event> events(MAX_EVENTS);

  while (true) {
    int num_events = epoll_wait(epoll_fd, events.data(), MAX_EVENTS, -1);
    if (num_events == -1) {
      if (errno == EINTR)
        continue;
      perror("Erreur epoll_wait");
      break;
    }

    for (int i = 0; i < num_events; ++i) {
      if ((events[i].events & EPOLLERR) || (events[i].events & EPOLLHUP) ||
          (!(events[i].events & EPOLLIN))) {
        std::cerr << "[System] Erreur ou fermeture sur le socket FD: "
                  << events[i].data.fd << std::endl;
        close(events[i].data.fd);
        continue;
      }

      if (events[i].data.fd == server_fd) {
        handle_new_connection(server_fd, epoll_fd);
      } else {
        handle_client_request(events[i].data.fd, epoll_fd);
      }
    }
  }

  close(server_fd);
  close(epoll_fd);
  return 0;
  return 0;
}