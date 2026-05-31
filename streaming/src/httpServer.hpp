#pragma once
#include <sys/epoll.h>


void handle_new_connection(int server_fd, int epoll_fd);
void handle_client_request(int client_fd,[[maybe_unused]] int epoll_fd);