#include "httpServer.hpp"
#include "utils.hpp"

#include <algorithm>
#include <cerrno>
#include <charconv>
#include <cctype>
#include <cstdint>
#include <cstdlib>
#include <filesystem>
#include <fstream>
#include <limits>
#include <map>
#include <optional>
#include <sstream>
#include <string>
#include <string_view>
#include <system_error>
#include <sys/sendfile.h>
#include <sys/socket.h>
#include <sys/stat.h>
#include <sys/time.h>
#include <unistd.h>
#include <fcntl.h>

namespace {
constexpr std::size_t MAX_REQUEST_SIZE = 16 * 1024;
constexpr std::size_t MAX_MANIFEST_SIZE = 1024 * 1024;
constexpr int SOCKET_TIMEOUT_SECONDS = 10;
std::filesystem::path canonical_video_root;

struct HttpRequest {
    std::string method;
    std::string url;
    std::string version;
    std::map<std::string, std::string> headers;
};

struct ByteRange {
    off_t start;
    off_t end;
};

void configure_socket_timeouts(int client_fd) {
    timeval timeout{};
    timeout.tv_sec = SOCKET_TIMEOUT_SECONDS;
    setsockopt(client_fd, SOL_SOCKET, SO_RCVTIMEO, &timeout, sizeof(timeout));
    setsockopt(client_fd, SOL_SOCKET, SO_SNDTIMEO, &timeout, sizeof(timeout));
}

bool send_all(int client_fd, const std::string& content) {
    std::size_t sent_total = 0;
    while (sent_total < content.size()) {
        const ssize_t sent = send(
            client_fd,
            content.data() + sent_total,
            content.size() - sent_total,
            MSG_NOSIGNAL
        );
        if (sent > 0) {
            sent_total += static_cast<std::size_t>(sent);
            continue;
        }
        if (sent == -1 && errno == EINTR) continue;
        return false;
    }
    return true;
}

bool send_headers(
    int client_fd,
    int status,
    const std::string& reason,
    std::uintmax_t content_length,
    const std::string& content_type,
    const std::map<std::string, std::string>& additional_headers = {}
) {
    std::ostringstream response;
    response << "HTTP/1.1 " << status << ' ' << reason << "\r\n"
             << "Content-Length: " << content_length << "\r\n"
             << "Content-Type: " << content_type << "\r\n"
             << "Connection: close\r\n"
             << "X-Content-Type-Options: nosniff\r\n";
    for (const auto& [name, value] : additional_headers) {
        response << name << ": " << value << "\r\n";
    }
    response << "\r\n";
    return send_all(client_fd, response.str());
}

void send_empty_response(int client_fd, int status, const std::string& reason) {
    send_headers(client_fd, status, reason, 0, "text/plain; charset=utf-8");
}

bool read_request(int client_fd, std::string& request) {
    char buffer[4096];
    while (request.find("\r\n\r\n") == std::string::npos) {
        const ssize_t received = recv(client_fd, buffer, sizeof(buffer), 0);
        if (received > 0) {
            request.append(buffer, static_cast<std::size_t>(received));
            if (request.size() > MAX_REQUEST_SIZE) return false;
            continue;
        }
        if (received == -1 && errno == EINTR) continue;
        return false;
    }
    return true;
}

std::string trim(std::string value) {
    const auto not_space = [](unsigned char character) {
        return !std::isspace(character);
    };
    value.erase(value.begin(), std::find_if(value.begin(), value.end(), not_space));
    value.erase(std::find_if(value.rbegin(), value.rend(), not_space).base(), value.end());
    return value;
}

bool parse_request(const std::string& raw_request, HttpRequest& request) {
    std::istringstream stream(raw_request);
    std::string line;
    if (!std::getline(stream, line)) return false;
    if (!line.empty() && line.back() == '\r') line.pop_back();

    std::istringstream request_line(line);
    std::string trailing_value;
    if (!(request_line >> request.method >> request.url >> request.version)
        || (request_line >> trailing_value)
        || (request.version != "HTTP/1.1" && request.version != "HTTP/1.0")) {
        return false;
    }

    while (std::getline(stream, line)) {
        if (!line.empty() && line.back() == '\r') line.pop_back();
        if (line.empty()) break;

        const std::size_t separator = line.find(':');
        if (separator == std::string::npos) return false;
        std::string name = line.substr(0, separator);
        std::transform(name.begin(), name.end(), name.begin(),
                       [](unsigned char character) { return static_cast<char>(std::tolower(character)); });
        request.headers[trim(name)] = trim(line.substr(separator + 1));
    }
    return true;
}

bool is_safe_folder_name(const std::string& folder) {
    if (folder.empty() || folder.size() > 128) return false;
    return std::all_of(folder.begin(), folder.end(), [](unsigned char character) {
        return std::isalnum(character) || character == '_' || character == '-';
    });
}

bool parse_video_path(
    const std::string& clean_url,
    std::string& video_folder,
    std::filesystem::path& relative_file
) {
    constexpr std::string_view prefix = "/video/";
    if (!is_path_safe(clean_url) || clean_url.rfind(prefix, 0) != 0) return false;

    const std::string path_after_prefix = clean_url.substr(prefix.size());
    const std::size_t slash_position = path_after_prefix.find('/');
    if (slash_position == std::string::npos || slash_position == path_after_prefix.size() - 1) {
        return false;
    }

    video_folder = path_after_prefix.substr(0, slash_position);
    if (!is_safe_folder_name(video_folder)) return false;

    relative_file = path_after_prefix.substr(slash_position + 1);
    return !relative_file.empty();
}

std::optional<std::uintmax_t> parse_unsigned(const std::string& value) {
    if (value.empty()) return std::nullopt;
    std::uintmax_t parsed = 0;
    const auto [end, error] = std::from_chars(
        value.data(),
        value.data() + value.size(),
        parsed
    );
    if (error != std::errc{} || end != value.data() + value.size()) return std::nullopt;
    return parsed;
}

std::optional<ByteRange> parse_range(
    const std::string& value,
    std::uintmax_t file_size
) {
    constexpr std::string_view prefix = "bytes=";
    if (file_size == 0 || value.rfind(prefix, 0) != 0 || value.find(',') != std::string::npos) {
        return std::nullopt;
    }

    const std::string range_value = value.substr(prefix.size());
    const std::size_t separator = range_value.find('-');
    if (separator == std::string::npos) return std::nullopt;

    const std::string start_value = range_value.substr(0, separator);
    const std::string end_value = range_value.substr(separator + 1);
    std::uintmax_t start = 0;
    std::uintmax_t end = file_size - 1;

    if (start_value.empty()) {
        const auto suffix_length = parse_unsigned(end_value);
        if (!suffix_length || *suffix_length == 0) return std::nullopt;
        start = *suffix_length >= file_size ? 0 : file_size - *suffix_length;
    } else {
        const auto parsed_start = parse_unsigned(start_value);
        if (!parsed_start || *parsed_start >= file_size) return std::nullopt;
        start = *parsed_start;
        if (!end_value.empty()) {
            const auto parsed_end = parse_unsigned(end_value);
            if (!parsed_end || *parsed_end < start) return std::nullopt;
            end = std::min(*parsed_end, file_size - 1);
        }
    }

    if (start > static_cast<std::uintmax_t>(std::numeric_limits<off_t>::max())
        || end > static_cast<std::uintmax_t>(std::numeric_limits<off_t>::max())) {
        return std::nullopt;
    }
    return ByteRange{static_cast<off_t>(start), static_cast<off_t>(end)};
}

std::string append_ticket_to_local_uri(const std::string& uri, const std::string& ticket) {
    if (uri.empty()
        || uri.find("ticket=") != std::string::npos
        || uri.rfind("http://", 0) == 0
        || uri.rfind("https://", 0) == 0
        || uri.rfind("//", 0) == 0
        || uri.rfind("data:", 0) == 0) {
        return uri;
    }

    const std::size_t fragment_position = uri.find('#');
    std::string result = uri.substr(0, fragment_position);
    result += result.find('?') == std::string::npos ? "?ticket=" : "&ticket=";
    result += ticket;
    if (fragment_position != std::string::npos) {
        result += uri.substr(fragment_position);
    }
    return result;
}

void rewrite_uri_attributes(std::string& line, const std::string& ticket) {
    constexpr std::string_view attribute = "URI=\"";
    std::size_t search_position = 0;
    while ((search_position = line.find(attribute, search_position)) != std::string::npos) {
        const std::size_t uri_start = search_position + attribute.size();
        const std::size_t uri_end = line.find('"', uri_start);
        if (uri_end == std::string::npos) return;

        const std::string uri = line.substr(uri_start, uri_end - uri_start);
        const std::string rewritten_uri = append_ticket_to_local_uri(uri, ticket);
        line.replace(uri_start, uri_end - uri_start, rewritten_uri);
        search_position = uri_start + rewritten_uri.size() + 1;
    }
}

std::string rewrite_manifest(const std::filesystem::path& file_path, const std::string& ticket) {
    std::ifstream manifest(file_path, std::ios::binary);
    if (!manifest.is_open()) return "";

    std::ostringstream rewritten;
    std::string line;
    while (std::getline(manifest, line)) {
        if (!line.empty() && line.back() == '\r') line.pop_back();
        const std::size_t first_character = line.find_first_not_of(" \t");
        if (first_character != std::string::npos && line[first_character] == '#') {
            rewrite_uri_attributes(line, ticket);
        } else if (first_character != std::string::npos) {
            const std::size_t last_character = line.find_last_not_of(" \t");
            const std::string uri = line.substr(first_character, last_character - first_character + 1);
            const std::string rewritten_uri = append_ticket_to_local_uri(uri, ticket);
            line.replace(first_character, last_character - first_character + 1, rewritten_uri);
        }
        rewritten << line << '\n';
        if (rewritten.tellp() > static_cast<std::streamoff>(MAX_MANIFEST_SIZE)) return "";
    }
    return rewritten.str();
}

bool is_within_root(
    const std::filesystem::path& candidate,
    const std::filesystem::path& root
) {
    std::error_code error;
    const std::filesystem::path relative = std::filesystem::relative(candidate, root, error);
    if (error || relative.empty() || relative.is_absolute()) return false;
    for (const auto& component : relative) {
        if (component == "..") return false;
    }
    return true;
}

void serve_file(
    int client_fd,
    const HttpRequest& request,
    const std::filesystem::path& file_path,
    const std::string& ticket
) {
    std::error_code error;
    const std::uintmax_t file_size = std::filesystem::file_size(file_path, error);
    if (error) {
        send_empty_response(client_fd, 404, "Not Found");
        return;
    }

    const std::string content_type = get_content_type(file_path.string());
    if (content_type == "application/vnd.apple.mpegurl") {
        const std::string manifest = rewrite_manifest(file_path, ticket);
        if (manifest.empty()) {
            send_empty_response(client_fd, 500, "Internal Server Error");
            return;
        }
        if (send_headers(
                client_fd,
                200,
                "OK",
                manifest.size(),
                content_type,
                {{"Cache-Control", "private, no-store"}}
            ) && request.method != "HEAD") {
            send_all(client_fd, manifest);
        }
        return;
    }

    ByteRange range{0, static_cast<off_t>(file_size == 0 ? 0 : file_size - 1)};
    bool partial_response = false;
    const auto range_header = request.headers.find("range");
    if (range_header != request.headers.end()) {
        const auto parsed_range = parse_range(range_header->second, file_size);
        if (!parsed_range) {
            send_headers(
                client_fd,
                416,
                "Range Not Satisfiable",
                0,
                "text/plain; charset=utf-8",
                {{"Content-Range", "bytes */" + std::to_string(file_size)}}
            );
            return;
        }
        range = *parsed_range;
        partial_response = true;
    }

    const std::uintmax_t response_size = file_size == 0
        ? 0
        : static_cast<std::uintmax_t>(range.end - range.start + 1);
    std::map<std::string, std::string> headers{
        {"Accept-Ranges", "bytes"},
        {"Cache-Control", "private, max-age=3600"}
    };
    if (partial_response) {
        headers["Content-Range"] = "bytes " + std::to_string(range.start)
            + '-' + std::to_string(range.end)
            + '/' + std::to_string(file_size);
    }

    if (!send_headers(
            client_fd,
            partial_response ? 206 : 200,
            partial_response ? "Partial Content" : "OK",
            response_size,
            content_type,
            headers
        ) || request.method == "HEAD" || response_size == 0) {
        return;
    }

    const int file_fd = open(file_path.c_str(), O_RDONLY | O_CLOEXEC);
    if (file_fd == -1) return;

    off_t offset = range.start;
    off_t remaining = range.end - range.start + 1;
    while (remaining > 0) {
        const std::size_t chunk_size = static_cast<std::size_t>(
            std::min<off_t>(remaining, 1024 * 1024 * 1024)
        );
        const ssize_t sent = sendfile(client_fd, file_fd, &offset, chunk_size);
        if (sent > 0) {
            remaining -= sent;
            continue;
        }
        if (sent == -1 && errno == EINTR) continue;
        break;
    }
    close(file_fd);
}
}

bool initialize_video_root(const std::string& video_root_path) {
    std::error_code error;
    canonical_video_root = std::filesystem::canonical(video_root_path, error);
    return !error && std::filesystem::is_directory(canonical_video_root, error) && !error;
}

void handle_client_request(int client_fd) {
    configure_socket_timeouts(client_fd);

    std::string raw_request;
    if (!read_request(client_fd, raw_request)) {
        send_empty_response(client_fd, 400, "Bad Request");
        close(client_fd);
        return;
    }

    HttpRequest request;
    if (!parse_request(raw_request, request)) {
        send_empty_response(client_fd, 400, "Bad Request");
        close(client_fd);
        return;
    }

    if (request.method != "GET" && request.method != "HEAD") {
        send_empty_response(client_fd, 405, "Method Not Allowed");
        close(client_fd);
        return;
    }

    if (request.url == "/health") {
        constexpr std::string_view body = "OK\n";
        if (send_headers(client_fd, 200, "OK", body.size(), "text/plain; charset=utf-8")
            && request.method != "HEAD") {
            send_all(client_fd, std::string(body));
        }
        close(client_fd);
        return;
    }

    const std::size_t query_position = request.url.find('?');
    const std::string clean_url = request.url.substr(0, query_position);
    std::string video_folder;
    std::filesystem::path relative_file;
    if (!parse_video_path(clean_url, video_folder, relative_file)) {
        send_empty_response(client_fd, 404, "Not Found");
        close(client_fd);
        return;
    }

    const std::string ticket = extract_ticket(request.url);
    if (!verify_streaming_ticket(ticket, video_folder)) {
        send_empty_response(client_fd, 403, "Forbidden");
        close(client_fd);
        return;
    }

    std::error_code error;
    const std::filesystem::path requested_file = std::filesystem::canonical(
        canonical_video_root / video_folder / relative_file,
        error
    );
    if (error
        || !is_within_root(requested_file, canonical_video_root)
        || !std::filesystem::is_regular_file(requested_file, error)
        || error) {
        send_empty_response(client_fd, 404, "Not Found");
        close(client_fd);
        return;
    }

    serve_file(client_fd, request, requested_file, ticket);
    close(client_fd);
}
