cmake_minimum_required(VERSION 3.22.1)

project("bus-tracker")

add_library(
        ${CMAKE_PROJECT_NAME}

        SHARED

        bus-tracker.cpp
        path.cpp
        unzip.cpp
        pkg.cpp
        md5.c

        third/minizip/mz_os.c
        third/minizip/mz_os_posix.c
        third/minizip/mz_strm_zlib.c
        third/minizip/mz_strm.c
        third/minizip/mz_strm_buf.c
        third/minizip/mz_strm_mem.c
        third/minizip/mz_strm_posix.c
        third/minizip/mz_strm_split.c
        third/minizip/mz_zip.c
)

find_library(log-lib
        log )

find_library(
        z-lib
        z)

target_link_libraries(
        ${CMAKE_PROJECT_NAME}
        ${log-lib}
        ${z-lib})