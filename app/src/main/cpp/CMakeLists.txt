cmake_minimum_required(VERSION 3.22.1)
project(feather_dsd LANGUAGES C CXX)

set(CMAKE_C_STANDARD 11)
set(CMAKE_C_STANDARD_REQUIRED ON)
set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_POSITION_INDEPENDENT_CODE ON)

# Keep the bundled LGPL DSD-to-PCM engine self-contained: use its built-in
# sequential backend and the portable FIR implementation (no TBB or IPP).
set(DSDPCM_USE_TBB OFF CACHE BOOL "" FORCE)
set(DSDPCM_USE_STL OFF CACHE BOOL "" FORCE)
set(DSDPCM_USE_IPP OFF CACHE BOOL "" FORCE)

set(DSD_NEXUS_ROOT "${CMAKE_CURRENT_SOURCE_DIR}/third_party/dsd-nexus")
add_subdirectory("${DSD_NEXUS_ROOT}/libs/libsautil" "${CMAKE_CURRENT_BINARY_DIR}/libsautil")
add_subdirectory("${DSD_NEXUS_ROOT}/libs/libdsdpcm" "${CMAKE_CURRENT_BINARY_DIR}/libdsdpcm")

add_library(feather_dsd SHARED dsd_jni.cpp)
target_include_directories(feather_dsd PRIVATE
    "${DSD_NEXUS_ROOT}/libs/libdsdpcm/include"
)
target_compile_options(feather_dsd PRIVATE -Wall -Wextra)
target_link_libraries(feather_dsd PRIVATE libdsdpcm dsdpcm_core sautil log)
