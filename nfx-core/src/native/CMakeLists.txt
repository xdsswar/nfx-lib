cmake_minimum_required(VERSION 3.28)

set(THIS_LIB "nfx-core")

project(${THIS_LIB})

set(VERSION "1.0.0")

set(CMAKE_BUILD_TYPE Release)
set(CMAKE_CXX_STANDARD 17)


set(JAVA_INCLUDE_PATH "$ENV{JAVA_HOME}/include")
set(JAVA_EXTRAS "")

if (WIN32)
    set(JAVA_EXTRAS "${JAVA_INCLUDE_PATH}/win32")
endif (WIN32)


#Add libs
find_package(JNI REQUIRED)
include_directories(${JAVA_INCLUDE_PATH})
include_directories(${JAVA_EXTRAS})

add_library(${THIS_LIB}-${VERSION} SHARED
        resources.rc
        src/utils/utils.h
        src/utils/utils.cpp
        src/Nfx.cpp
        src/jni_h/xss_it_nfx_NfxUtil.h

)
