cmake_minimum_required(VERSION 3.16)
project(netflix_streaming_engine CXX)

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

add_compile_options(-Wall -Wextra -Wpedantic -Werror -O3)

find_package(OpenSSL REQUIRED)
find_package(Threads REQUIRED)

include(FetchContent)

set(JWT_BUILD_EXAMPLES OFF CACHE BOOL "Disable jwt-cpp examples" FORCE)
set(JWT_BUILD_TESTS OFF CACHE BOOL "Disable jwt-cpp tests" FORCE)
FetchContent_Declare(
    jwt-cpp
    GIT_REPOSITORY https://github.com/thalhammer/jwt-cpp.git
    GIT_TAG v0.7.2
    GIT_SHALLOW TRUE
)
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
    Threads::Threads
    jwt-cpp::jwt-cpp
)

enable_testing()
add_executable(streaming_utils_tests
    tests/utils_test.cpp
    src/utils.cpp
)
target_include_directories(streaming_utils_tests PRIVATE src)
target_link_libraries(streaming_utils_tests PRIVATE
    OpenSSL::SSL
    OpenSSL::Crypto
    jwt-cpp::jwt-cpp
)
add_test(NAME streaming_utils_tests COMMAND streaming_utils_tests)
