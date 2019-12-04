#
# Copyright © 2019 Agency for Data Supply and Efficiency
#
# Permission is hereby granted, free of charge, to any person obtaining a copy
# of this software and associated documentation files (the "Software"), to deal
# in the Software without restriction, including without limitation the rights
# to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
# copies of the Software, and to permit persons to whom the Software is
# furnished to do so, subject to the following conditions:
#
# The above copyright notice and this permission notice shall be included in all
# copies or substantial portions of the Software.
#
# THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
# IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
# FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
# AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
# LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
# OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
# SOFTWARE.
#

#
# Compiles the native part of the bindings to PROJ. This build process requires the PROJ library to be installed on
# the current platform. The PROJ header files shall exist in the compiler default include directory ("/usr/include"
# or "/usr/local/include" or "/opt/local/include", etc). On Linux distributions, those header files are obtained by
# installing the "proj-devel" package.
#
# HOW TO BUILD:
# C/C++ compilation is currently not part of Maven build. Compilation shall be done manually like below.
# The following commands need to be executed only once:
#
#     mvn compile
#     mkdir target/cmake
#     cd target/cmake
#     cmake ../../src/main/cpp
#     cd ../..
#
# Then the following command need to be run from the project root directory, and once again every time
# the native C/C++ code changed:
#
#     cmake --build target/cmake
#
# The executable will be created in the "target/classes/…/proj/" directory for inclusion in the JAR file.
# Only the executable for the current platform will be updated. If the JNI have been updated, then `cmake`
# will need to be executed again. The JNI may also need to be recompiled when the PROJ version changes.
#
cmake_minimum_required(VERSION 3.5)

# Set the project name and version.
project(PROJ-JNI VERSION 1.0 LANGUAGES CXX)

# Specify the C++ standard.
set(CMAKE_CXX_STANDARD 11)

# Omit debugging information.
set(CMAKE_BUILD_TYPE Release)

# The binary will be put in the directory of Java classes package.
SET(CMAKE_LIBRARY_OUTPUT_DIRECTORY ../classes/org/kortforsyningen/proj)

# Find Java Native Interfaces (JNI). The location is system-dependent. In particular there is a "win32",
# "linux" or "darwin" directory to append to the path. CMake adds automatically the right sub-directory.
find_package(JNI REQUIRED)
include_directories(${JNI_INCLUDE_DIRS})

# We want the "local/lib/" directory to be checked first in order to link against the PROJ version compiled
# locally. Otherwise the PROJ version installed by the package manager may have precedence on Linux systems.
# CMake searches the path in the desired order when using this directive.
find_library(PROJLIB proj)

# GCC options reminder:
#    -fPIC		Generate position-independent code suitable for use in a shared library.
#    -shared		Produce a shared object which can then be linked with other objects to form an executable.
#    -shared-libgcc	Use the shared version of libgcc. Recommended for JNI when exceptions may be thrown from gcc.
#    -O2		Performs nearly all supported optimizations that do not involve a space-speed tradeoff.
#
# Those options could be specified with add_compile_options(-fPIC -shared -shared-libgcc -O2)
# but cmake seems to add them automatically (as seen in CMakeFiles/proj-binding.dir/link.txt).

# Compile all C++ files in the source directory.
file(GLOB SOURCES "*.cpp")
add_library(proj-binding SHARED ${SOURCES})

# Specify some C++ features required by source code.
target_compile_features(proj-binding PUBLIC cxx_nullptr cxx_static_assert)

# Dependencies
target_link_libraries(proj-binding ${PROJLIB})
