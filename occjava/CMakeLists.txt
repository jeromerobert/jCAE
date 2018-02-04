cmake_minimum_required(VERSION 2.8)
find_package(SWIG REQUIRED)
include(${SWIG_USE_FILE})
find_package(JNI REQUIRED)

#From oce/examples/find_package_components/CMakeLists.txt
set(OCE_TOOLKITS TKBRep TKBool TKG2d TKG3d TKGeomBase TKIGES TKMath TKOffset TKXSBase TKernel TKSTEP TKPrim TKTopAlgo TKFillet TKMesh TKBO TKGeomAlgo TKShHealing TKSTEPBase)
find_package(OCE COMPONENTS ${OCE_TOOLKITS})
if(OCE_FOUND)
    message(STATUS "Found OCE version ${OCE_VERSION}")
    if(NOT OCE_ALL_FOUND)
      set(OCE_FOUND false)
      message(WARNING "Ignoring OCE installation due to missing toolkit(s): ${OCE_MISSING_TOOLKITS}")
    endif(NOT OCE_ALL_FOUND)
endif(OCE_FOUND)

if(OCE_FOUND)
    # Include files reside in ${OCE_INCLUDE_DIRS};
    include_directories(${OCE_INCLUDE_DIRS})
    # We do not need library path, they will be automatically imported.
else(OCE_FOUND)
    # OCE not found; either it is not found and user
    # has to set OCE_DIR to the directory containing
    # OCEConfig.cmake, or OCE is not installed and we
    # try to find OpenCascade files.
    find_path(OCC_INCLUDE_PATH "TopoDS_Shape.hxx" PATHS ENV CASROOT PATH_SUFFIXES inc include opencascade include/opencascade)
    if(NOT OCC_INCLUDE_PATH)
        message(FATAL_ERROR "Header file TopoDS_Shape.hxx not found.  To specify paths of OpenCascade files, you may either define the CASROOT environment variable, or set both OCC_INCLUDE_PATH and OCC_LIB_PATH variables.")
    endif(NOT OCC_INCLUDE_PATH)

    if(WIN32)
        set(OCC_LIB_EXTRA_PATH "win32")
    else(WIN32)
        set(OCC_LIB_EXTRA_PATH "lin")
    endif(WIN32)
    find_library(OCC_LIB_PATH TKernel PATHS ENV CASROOT PATH_SUFFIXES lib64 lib
                 ${OCC_LIB_EXTRA_PATH}/lib ${OCC_LIB_EXTRA_PATH}/vc8/lib)
    if(NOT OCC_LIB_PATH)
        message(FATAL_ERROR "OpenCascade TKernel library not found.  To specify paths of OpenCascade files, you may either define the CASROOT environment variable, or set both OCC_INCLUDE_PATH and OCC_LIB_PATH variables.")
    endif(NOT OCC_LIB_PATH)
    message(STATUS "OCC search path for include files: OCC_INCLUDE_PATH=${OCC_INCLUDE_PATH}")
    include_directories(${OCC_INCLUDE_PATH})
    message(STATUS "OCC search path for libraries: OCC_LIB_PATH=${OCC_LIB_PATH}")
    link_directories(${OCC_LIB_PATH})
endif(OCE_FOUND)

set_source_files_properties(src/OccJava.i PROPERTIES CPLUSPLUS ON)
include_directories(${JAVA_INCLUDE_PATH} ${JAVA_INCLUDE_PATH2} ${OCE_INCLUDE_DIR} src)
set(CMAKE_SWIG_OUTDIR ${CMAKE_CURRENT_SOURCE_DIR}/src-java/org/jcae/opencascade/jni)
set(CMAKE_SWIG_FLAGS -package org.jcae.opencascade.jni)

file(GLOB SWIG_MODULE_OccJava_EXTRA_DEPS src/*.i)
if("${CMAKE_VERSION}" VERSION_GREATER 3.8)
  SWIG_ADD_LIBRARY(OccJava LANGUAGE java TYPE MODULE SOURCES src/OccJava.i src/jnistream.cxx)
else()
  SWIG_ADD_MODULE(OccJava java src/OccJava.i src/jnistream.cxx)
endif()
SWIG_LINK_LIBRARIES(OccJava ${OCE_TOOLKITS})

if(WIN32)
  add_definitions(-DWNT)
  if(MINGW)
      set(CMAKE_MODULE_LINKER_FLAGS "-Wl,--add-stdcall-alias ${CMAKE_MODULE_LINKER_FLAGS}")
      #We want OccJava.dll, not libOccJava.dll
      set_target_properties(${SWIG_MODULE_OccJava_REAL_NAME} PROPERTIES PREFIX "")
  endif(MINGW)
endif(WIN32)
#CMake won't create this directory it self. A bug ?
file(MAKE_DIRECTORY ${CMAKE_BINARY_DIR}/src)


