#pragma once

#include <string>

bool initialize_video_root(const std::string& video_root_path);
void handle_client_request(int client_fd);
