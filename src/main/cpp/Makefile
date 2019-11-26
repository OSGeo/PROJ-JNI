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
# C/C++ compilation is currently not part of Maven build. Compilation shall be done manually like below:
#
#     cd src/main/cpp
#     make
#
# The executable will be created in the "src/resources/…/proj/<platform>/" directory for inclusion in the JAR file.
# Only the executable for the current platform will be updated. If the JNI have been updated, then `make` will need
# to be executed on all supported platforms. The JNI may also need to be recompiled when the PROJ version changes.
#
ifndef JAVA_HOME
    $(error JAVA_HOME is not set.)
endif

ifndef OS
    OS = $(shell uname)
endif

ifeq ($(OS),Windows_NT)
    SUBDIR = window
else ifeq ($(OS),Linux)
    SUBDIR = linux
else ifeq ($(OS),Darwin)
    SUBDIR = darwin
else
    $(error The $(OS) platform is not supported by this makefile.)
endif


#
# GCC options reminder:
#    -fPIC		Generate position-independent code suitable for use in a shared library.
#    -shared		Produce a shared object which can then be linked with other objects to form an executable.
#    -shared-libgcc	Use the shared version of libgcc. Recommended for JNI when exceptions may be thrown from gcc.
#    -O2		Performs nearly all supported optimizations that do not involve a space-speed tradeoff.
#
# If the user specified a PROJ directory, also search for include files and shared library there.
# This is useful when that custom directory contains a more recent version of PROJ than the one
# installed on the system.
#
FLAGS       = -fPIC -shared -shared-libgcc -O2
LIBS        = -lproj
SRC_FILES   = assertions.cpp bindings.cpp
OUT_FILE    = libproj-binding.so
INCLUDE_DIR = -I$(JAVA_HOME)/include -I$(JAVA_HOME)/include/$(SUBDIR)

#
# We want the "local/lib/" directory to be checked first in order to link against the PROJ version compiled locally,
# if any. Otherwise the PROJ version installed by the package manager may have precedence.
#
ifeq ($(OS),Linux)
    LIBS += -L/usr/local/lib
endif
ifeq ($(OS),Darwin)
    LIBS += -L/opt/local/lib
endif

#
# If the user did not executed "make install" and prefer to link the "*.so" files directly in the project directory,
# this configuration should make that possible. Note that in such case, user should also define the following:
#
#     export PROJ_LIB=$PROJ_DIR/data
#
ifdef PROJ_DIR
    INCLUDE_DIR += -I$(PROJ_DIR)/include -I$(PROJ_DIR)/src
    LIBS = -L$(PROJ_DIR)/src/.libs -lproj
endif


#
# The $(LIBS) parameter should be last (it way work anywhere, but putting it last allows more predictable results).
# Source files are encoded in UTF-8.
#
all:
	g++ $(FLAGS) $(INCLUDE_DIR) -o ../resources/org/kortforsyningen/proj/$(SUBDIR)/$(OUT_FILE) $(SRC_FILES) $(LIBS)
