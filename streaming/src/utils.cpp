#include "utils.hpp"

#include <algorithm>
#include <cctype>
#include <fstream>
#include <iostream>
#include <jwt-cpp/jwt.h>
#include <sstream>

namespace {
std::string public_key;
}

bool initialize_ticket_verifier(const std::string& public_key_path) {
    std::ifstream public_key_file(public_key_path);
    if (!public_key_file.is_open()) {
        std::cerr << "[ERROR] Unable to open streaming public key at "
                  << public_key_path << std::endl;
        return false;
    }

    std::ostringstream content;
    content << public_key_file.rdbuf();
    public_key = content.str();
    if (public_key.empty()) {
        std::cerr << "[ERROR] Streaming public key is empty" << std::endl;
        return false;
    }

    return true;
}

bool is_path_safe(const std::string& path) {
    return !path.empty()
        && path.front() == '/'
        && path.find("..") == std::string::npos
        && path.find('\\') == std::string::npos
        && path.find('\0') == std::string::npos
        && path.find('%') == std::string::npos
        && path.find("//") == std::string::npos;
}

std::string get_content_type(const std::string& path) {
    const std::size_t extension_position = path.find_last_of('.');
    std::string extension = extension_position == std::string::npos
        ? ""
        : path.substr(extension_position);
    std::transform(extension.begin(), extension.end(), extension.begin(),
                   [](unsigned char character) { return static_cast<char>(std::tolower(character)); });

    if (extension == ".m3u8") return "application/vnd.apple.mpegurl";
    if (extension == ".ts") return "video/mp2t";
    if (extension == ".m4s") return "video/iso.segment";
    if (extension == ".mp4") return "video/mp4";
    if (extension == ".vtt") return "text/vtt; charset=utf-8";
    return "application/octet-stream";
}

std::string extract_ticket(const std::string& url) {
    const std::size_t query_position = url.find('?');
    if (query_position == std::string::npos) return "";

    std::size_t parameter_start = query_position + 1;
    while (parameter_start < url.size()) {
        const std::size_t parameter_end = url.find('&', parameter_start);
        const std::string parameter = url.substr(
            parameter_start,
            parameter_end == std::string::npos
                ? std::string::npos
                : parameter_end - parameter_start
        );
        if (parameter.rfind("ticket=", 0) == 0) {
            return parameter.substr(7);
        }
        if (parameter_end == std::string::npos) break;
        parameter_start = parameter_end + 1;
    }

    return "";
}

bool verify_streaming_ticket(
    const std::string& ticket,
    const std::string& requested_movie_id
) {
    if (ticket.empty() || public_key.empty()) return false;

    try {
        const auto decoded = jwt::decode(ticket);
        const auto verifier = jwt::verify()
            .allow_algorithm(jwt::algorithm::rs256(public_key, "", "", ""))
            .with_issuer("netflix-backend")
            .with_subject("streaming-token")
            .leeway(5);
        verifier.verify(decoded);

        if (!decoded.has_payload_claim("movieId")) return false;
        return decoded.get_payload_claim("movieId").as_string() == requested_movie_id;
    } catch (const std::exception& exception) {
        std::cerr << "[SECURITY] Invalid streaming ticket: "
                  << exception.what() << std::endl;
        return false;
    }
}
