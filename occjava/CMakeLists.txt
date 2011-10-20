cmake_minimum_required(VERSION 2.8)
find_package(SWIG REQUIRED)
find_package(JNI REQUIRED)
list(APPEND CMAKE_MODULE_PATH "${CMAKE_CURRENT_SOURCE_DIR}/cmake")
find_package(OCE REQUIRED)

include(${SWIG_USE_FILE})
add_definitions(${OCE_DEFINITIONS})
set_source_files_properties(src/OccJava.i PROPERTIES CPLUSPLUS ON)
include_directories(${JAVA_INCLUDE_PATH} ${JAVA_INCLUDE_PATH2} ${OCE_INCLUDE_DIR} src)
set(CMAKE_SWIG_OUTDIR ${CMAKE_CURRENT_SOURCE_DIR}/src-java/org/jcae/opencascade/jni)
set(CMAKE_CXX_FLAGS -fpermissive)
set(CMAKE_MODULE_LINKER_FLAGS ${CMAKE_LIBRARY_PATH_FLAG}${OCE_LIB_DIR})
if(WIN32)
  set(CMAKE_MODULE_LINKER_FLAGS "-Wl,--add-stdcall-alias ${CMAKE_MODULE_LINKER_FLAGS}")
endif()
set(CMAKE_SWIG_FLAGS -package org.jcae.opencascade.jni)
#CMake won't create this directory it self. A bug ?
file(MAKE_DIRECTORY ${CMAKE_BINARY_DIR}/src)

SWIG_ADD_MODULE(OccJava java src/OccJava.i src/jnistream.cxx)
SWIG_LINK_LIBRARIES(OccJava TKBRep TKBool TKG2d TKG3d TKGeomBase TKIGES TKMath TKOffset TKXSBase TKernel TKSTEP TKPrim TKTopAlgo TKFillet TKMesh TKBO TKGeomAlgo TKShHealing TKSTEPBase)

