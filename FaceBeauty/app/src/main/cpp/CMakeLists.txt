cmake_minimum_required(VERSION 3.4.1)

add_library(
        native-lib
        SHARED
        secret_key.c
)

find_library(
        log-lib
        log
)

target_link_libraries(
        native-lib
        ${log-lib}
)