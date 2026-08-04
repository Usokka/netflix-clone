#include "utils.hpp"

#include <cassert>
#include <iostream>

int main() {
    assert(is_path_safe("/video/sintel/playlist.m3u8"));
    assert(!is_path_safe("/video/../private.pem"));
    assert(!is_path_safe("/video/%2e%2e/private.pem"));
    assert(!is_path_safe("video/sintel/playlist.m3u8"));

    assert(extract_ticket("/video/sintel/playlist.m3u8") == "");
    assert(extract_ticket("/video/sintel/playlist.m3u8?ticket=abc.def.ghi") == "abc.def.ghi");
    assert(extract_ticket("/video/sintel/playlist.m3u8?x=1&ticket=value&y=2") == "value");

    assert(get_content_type("playlist.m3u8") == "application/vnd.apple.mpegurl");
    assert(get_content_type("segment.ts") == "video/mp2t");
    assert(get_content_type("segment.m4s") == "video/iso.segment");

    std::cout << "streaming utils tests passed" << std::endl;
    return 0;
}
