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
# the current platform. The Proj header files are expected to be found either in the "/usr/local/include" directory
# (for example on Linux distributions), or "/opt/local/include" on some other platforms.
#
# HOW TO BUILD:
# C/C++ compilation is currently not part of Maven build. Compilation shall be done manually like below:
#
#     cd src/main/cpp
#     make
#
# The executable will be created in the "src/resources/native/<platform>/" directory for inclusion in the JAR file.
# Only the executable for the current platform will be updated. If the JNI have been updated, then `make` will need
# to be executed on all supported platforms.
#
# GCC options reminder:
#    -shared		Produce a shared object which can then be linked with other objects to form an executable.
#    -shared-libgcc	Use the shared version of libgcc. Recommended for JNI when exceptions may be thrown from gcc.
#    -fpic		Generate position-independent code suitable for use in a shared library.
#    -O2		Performs nearly all supported optimizations that do not involve a space-speed tradeoff.
#
FLAG   = -shared -shared-libgcc -fpic -O2
LIB    = -lm -ldl -lproj
SRC    = org_kortforsyningen_proj_Context.cpp
OUT    = libproj-binding.so

ifndef JAVA_HOME
    $(error JAVA_HOME is not set.)
endif

INCLUDE_DIR = -I$(JAVA_HOME)/include -I/usr/local/include -I/opt/local/include

ifndef OS
    OS = $(shell uname)
endif

ifeq ($(OS),Windows_NT)
    OUTDIR = window
endif

ifeq ($(OS),Linux)
    INCLUDE_DIR += -I$(JAVA_HOME)/include/linux
    OUTDIR = linux
endif

ifeq ($(OS),Darwin)
    INCLUDE_DIR += -I$(JAVA_HOME)/include/darwin
    LIB += -L/opt/local/lib
    OUTDIR = darwin
endif

ifndef OUTDIR
    $(error The $(OS) platform is not supported by this makefile.)
endif

# The $(LIB) parameter should be last (it way work anywhere,
# but putting it last allows more predictable results).
all:
	g++ $(FLAG) $(INCLUDE_DIR) -o ../resources/native/$(OUTDIR)/$(OUT) $(SRC) $(LIB)
