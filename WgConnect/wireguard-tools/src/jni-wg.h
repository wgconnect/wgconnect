/* SPDX-License-Identifier: GPL-2.0 */
/*
 * Copyright (C) 2022- All Rights Reserved.
 */

#ifndef JNI_WG_H
#define JNI_WG_H

#include <stdarg.h>
#include <stdio.h>
#include <stdlib.h>
#include <jni.h>

#define JNI_PROG_NAME "wg";

typedef struct {
    char *buf;
    size_t len;
    FILE *stream;
} Stream;

extern void jni_in_printf(char *fmt, ...);
extern size_t jni_in_read(void *ptr, size_t size, size_t nmemb);
extern int jni_in_getc(void);
extern int jni_in_seek(long int offset, int whence);
extern void jni_out_printf(char *fmt, ...);
extern int jni_out_puts(const char *str);
extern int jni_out_vprintf(const char *format, va_list arg);
extern void jni_err_printf(char *fmt, ...);
extern int jni_err_puts(const char *str);
extern void jni_perror(const char *str, int errnum);

#endif
