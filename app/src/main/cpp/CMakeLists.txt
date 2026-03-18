cmake_minimum_required(VERSION 3.22.1)

# Enterprise Fibonacci Native Computation Engine
# "Because Kotlin was too slow for adding two numbers together"
project("fibonacci" VERSION 2.1.0 LANGUAGES CXX)

add_library(fibonacci SHARED fibonacci.cpp)

target_link_libraries(fibonacci
        android
        log)
