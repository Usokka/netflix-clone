#include "utils.hpp"
#include <algorithm>

bool is_path_safe(const std::string &path) {
  if (path.find("..") != std::string::npos) {
    return false;
  }
  if (path.find('\\') != std::string::npos ||
      path.find('\0') != std::string::npos) {
    return false;
  }
  return true;
}

std::string get_content_type(const std::string &path) {
  if (path.rfind(".m3u8") != std::string::npos) {
    return "application/x-mpegURL";
  }
  if (path.rfind(".ts") != std::string::npos) {
    return "video/MP2T";
  }
  return "application/octet-stream";
}