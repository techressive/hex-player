cmake_minimum_required(VERSION 3.22.1)
project("hex_dsp")

add_library(
    hex_dsp
    SHARED
    biquad_dsp.cpp
)

find_library(
    log-lib
    log
)

target_link_libraries(
    hex_dsp
    ${log-lib}
)
