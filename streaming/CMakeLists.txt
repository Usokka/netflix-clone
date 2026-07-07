cmake_minimum_required(VERSION 3.5)
project(netflix_streaming_engine CXX)

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

add_compile_options(-Wall -Wextra -Wpedantic -Werror -O3)

find_package(OpenSSL REQUIRED)

include(FetchContent)
FetchContent_Declare(
    nlohmann_json
    GIT_REPOSITORY https://github.com/nlohmann/json.git
    GIT_TAG v3.11.3
)
FetchContent_MakeAvailable(nlohmann_json)

FetchContent_Declare(
    jwt-cpp
    GIT_REPOSITORY https://github.com/thalhammer/jwt-cpp.git
    GIT_TAG v0.7.0
)
# Dire à jwt-cpp de ne pas fetch nlohmann lui-même
set(JWT_EXTERNAL_NLOHMANN_JSON ON)
FetchContent_MakeAvailable(jwt-cpp)

add_executable(streaming_engine
    src/main.cpp
    src/httpServer.cpp
    src/utils.cpp
)

target_include_directories(streaming_engine PRIVATE src)

target_link_libraries(streaming_engine PRIVATE 
    OpenSSL::SSL 
    OpenSSL::Crypto 
    jwt-cpp::jwt-cpp
)
