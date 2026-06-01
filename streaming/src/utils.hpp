#pragma once
#include <string>


bool is_path_safe(const std::string& path);
std::string get_content_type(const std::string& path);
std::string extract_ticket(const std::string& url);
bool verify_streaming_ticket(const std::string& ticket, const std::string& client_ip, const std::string& requested_movie_id);