cmake_minimum_required(VERSION 3.20)
project(netflix_streaming_engine CXX)

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

add_compile_options(-Wall -Wextra -Wpedantic -Werror -O3)

add_executable(streaming_engine
    src/main.cpp
    src/httpServer.cpp
    src/utils.cpp
)

target_include_directories(streaming_engine PRIVATE src)