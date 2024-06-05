# SPDX-License-Identifier: GPL-2.0
#
# Copyright (C) 2015-2020 Jason A. Donenfeld <Jason@zx2c4.com>. All Rights Reserved.
# Copyright (C) 2024 wgconnect@proton.me.

DESTDIR ?=
#LIBDIR ?= ../../src/com/wgtools/
NATIVESBASEDIR ?= ./natives/
NATIVESDIR ?= 

PLATFORM ?= $(shell uname -s | tr '[:upper:]' '[:lower:]')
ARCH ?= $(shell uname -m)

CFLAGS ?= -O3 -fPIC
ifneq ($(wildcard uapi/$(PLATFORM)/.),)
CFLAGS += -idirafter uapi/$(PLATFORM)
endif
CFLAGS += -std=gnu99 -D_GNU_SOURCE
CFLAGS += -Wall -Wextra -Wunused
CFLAGS += -MMD -MP
CFLAGS += -DRUNSTATEDIR="\"$(RUNSTATEDIR)\""
CFLAGS += -I../../jni/include -DJNI_LIBRARY
LDFLAGS ?= -shared
ifeq ($(DEBUG),yes)
CFLAGS += -g
endif
WIREGUARD_TOOLS_VERSION = $(patsubst v%,%,$(shell GIT_DIR="$(PWD)/../.git" git describe --dirty 2>/dev/null))
ifneq ($(WIREGUARD_TOOLS_VERSION),)
CFLAGS += -D'WIREGUARD_TOOLS_VERSION="$(WIREGUARD_TOOLS_VERSION)"'
endif
ifeq ($(PLATFORM),linux)
CFLAGS += -I/usr/lib/jvm/default-runtime/include -I/usr/lib/jvm/default-runtime/include/linux
NATIVESDIR ?= $(NATIVESBASEDIR)linux_64
ifeq ($(ARCH),x86_64)
NATIVESDIR ?= $(NATIVESBASEDIR)linux_64
endif
endif
ifeq ($(PLATFORM),freebsd)
CFLAGS += -I/usr/local/openjdk11/include -I/usr/local/openjdk11/include/freebsd
LDLIBS += -lnv
ifeq ($(ARCH),x86_64)
NATIVESDIR ?= $(NATIVESBASEDIR)linux_64
endif
endif
ifeq ($(PLATFORM),haiku)
LDLIBS += -lnetwork -lbsd
endif
ifeq ($(PLATFORM),windows)
CC := x86_64-w64-mingw32-clang
WINDRES := $(shell $(CC) $(CFLAGS) -print-prog-name=windres 2>/dev/null)
CFLAGS += -Iwincompat/include -include wincompat/compat.h -DWINVER=0x0601 -D_WIN32_WINNT=0x0601 -flto
LDLIBS += -lws2_32 -lsetupapi -lole32 -ladvapi32 -Lwincompat
LDFLAGS += -flto -Wl,--dynamicbase -Wl,--nxcompat -Wl,--tsaware -mconsole
LDFLAGS += -Wl,--major-os-version=6 -Wl,--minor-os-version=1 -Wl,--major-subsystem-version=6 -Wl,--minor-subsystem-version=1
# The use of -Wl,/delayload: here implies we're using llvm-mingw
LDFLAGS += -Wl,/delayload:ws2_32.dll -Wl,/delayload:setupapi.dll -Wl,/delayload:ole32.dll -Wl,/delayload:advapi32.dll
VERSION := $(patsubst "%",%,$(filter "%",$(file < version.h)))
wg: wincompat/libc.o wincompat/init.o wincompat/loader.o wincompat/resources.o
wincompat/resources.o: wincompat/resources.rc wincompat/manifest.xml
	$(WINDRES) -DVERSION_STR=$(VERSION) -O coff -c 65001 -i $< -o $@
endif

ifneq ($(V),1)
BUILT_IN_LINK.o := $(LINK.o)
LINK.o = @echo "  LD      $@";
LINK.o += $(BUILT_IN_LINK.o)
BUILT_IN_COMPILE.c := $(COMPILE.c)
COMPILE.c = @echo "  CC      $@";
COMPILE.c += $(BUILT_IN_COMPILE.c)
BUILT_IN_RM := $(RM)
RM := @a() { echo "  CLEAN   $$@"; $(BUILT_IN_RM) "$$@"; }; a
WINDRES := @a() { echo "  WINDRES $${@: -1}"; $(WINDRES) "$$@"; }; a
CP := @a() { echo  "  CP      $$@"; cp "$$@"; }; a
MV := @a() { echo  "  MV      $$@"; mv "$$@"; }; a
JAR := @a() { echo "  JAR     $$@"; jar "$$@"; }; a
endif

libwg.so: wg
	$(CP) wg ./natives/linux_64/libwg.so
	$(JAR) cf ../../lib/natives-libraries.jar ./natives

wg: $(sort $(patsubst %.c,%.o,$(wildcard *.c)))

clean:
	$(RM) *.o *.d *.so wg $(wildcard wincompat/*.o wincompat/*.lib wincompat/*.dll)

check: clean
	scan-build --html-title=wgtools -maxloop 100 --view --keep-going $(MAKE) wg

all: libwg.so
.DEFAULT_GOAL: all
.PHONY: clean install check

-include *.d
