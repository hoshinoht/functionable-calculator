cmake_minimum_required(VERSION 3.22.1)

# Enterprise Fibonacci Native Computation Engine
# "Because Kotlin was too slow for adding two numbers together"
project("fibonacci" VERSION 2.1.0 LANGUAGES CXX)

add_library(fibonacci SHARED fibonacci.cpp)

target_link_options(fibonacci PRIVATE "-Wl,-z,max-page-size=16384")

target_link_libraries(fibonacci
        android
        log)

# ── Vulkan GPU Compute Engine ──────────────────────────────────────────────
# Because the CPU was insufficiently dramatic for adding two numbers.
add_library(gpu_compute SHARED gpu_compute.cpp)

target_link_options(gpu_compute PRIVATE "-Wl,-z,max-page-size=16384")

target_link_libraries(gpu_compute
        android
        log
        vulkan)
