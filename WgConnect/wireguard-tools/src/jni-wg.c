// SPDX-License-Identifier: GPL-2.0
/*
 * Copyright (C) 2022- All Rights Reserved.
 */

#include <stdarg.h>
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <jni.h>

#include "com_wgtools_WgSubcommand.h"
#include "subcommands.h"
#include "jni-wg.h"

static Stream *jniIn;
static Stream *jniOut;
static Stream *jniErr;
static int exitCode;

static void show_usage(FILE *stream) {
	fprintf(stream, "Usage: %s <cmd> [<args>]\n\n", PROG_NAME);
	fprintf(stream, "Available commands:\n");
	for (size_t i = 0; i < subcommands_count; ++i)
		fprintf(stream, "  %s: %s\n", subcommands[i].subcommand, subcommands[i].description);
	fprintf(stream, "You may pass `--help' to any of these commands to view usage.\n");
}

void streams_alloc(void) {
    jniIn = (Stream *)calloc(1, sizeof(Stream)); 
    jniIn->stream = open_memstream(&jniIn->buf, &jniIn->len);

    jniOut = (Stream *)calloc(1, sizeof(Stream)); 
    jniOut->stream = open_memstream(&jniOut->buf, &jniOut->len);

    jniErr = (Stream *)calloc(1, sizeof(Stream)); 
    jniErr->stream = open_memstream(&jniErr->buf, &jniErr->len);
}

void streams_dealloc(void) {
    if (jniIn != NULL) {
        fclose(jniIn->stream); free(jniIn->buf);
        free(jniIn);
    }

    if (jniOut != NULL) {
        fclose(jniOut->stream); free(jniOut->buf);
        free(jniOut);
    }

    if (jniErr != NULL) {
        fclose(jniErr->stream); free(jniErr->buf);
        free(jniErr);
    }
}

jobjectArray streams_to_array(JNIEnv *env) {
    // flush the output and error streams
    fflush(jniOut->stream);
    fflush(jniErr->stream);

    // get a class reference for java.lang.Integer
    jclass classInteger = (*env)->FindClass(env, "java/lang/Integer");

    // get the method id of the constructor which takes an int
    jmethodID midInit = (*env)->GetMethodID(env, classInteger, "<init>", "(I)V");
    if (midInit == NULL) {
        return NULL;
    }

    // call the constructor to allocate a new integer
    jobject exitCodeObj = (*env)->NewObject(env, classInteger, midInit, exitCode);

    // convert the output and error bufs to a jstring
    jstring outStr = (*env)->NewStringUTF(env, jniOut->buf);
    jstring errStr = (*env)->NewStringUTF(env, jniErr->buf);

    // get a class reference for java.lang.Object
    jclass classObject = (*env)->FindClass(env, "java/lang/Object");

    // allocate a jobjectArray of 3 java.lang.Object
    jobjectArray streamsJniArray = (*env)->NewObjectArray(env, 3, classObject, NULL);

    // set the jobjectArray
    (*env)->SetObjectArrayElement(env, streamsJniArray, 0, exitCodeObj);
    (*env)->SetObjectArrayElement(env, streamsJniArray, 1, outStr);
    (*env)->SetObjectArrayElement(env, streamsJniArray, 2, errStr);

    return streamsJniArray;
}

void jni_in_printf(char *fmt, ...) {
    va_list ap; 
    char *p, *sval;
    int ival;
    double dval;

    va_start(ap, fmt);
    for (p = fmt; *p; p++) {
        if (*p != '%') {
            fputc(*p, jniIn->stream);
            continue;
        }
        switch (*++p) {
            case 'd':
                ival = va_arg(ap, int);
                fprintf(jniIn->stream, "%d", ival);
                break;
            case 'f':
                dval  = va_arg(ap, double);
                fprintf(jniIn->stream, "%f", dval);
                break;
            case 's':
                for(sval = va_arg(ap, char *); *sval; sval++)
                    fputc(*sval, jniIn->stream);
                break;
            default:
                fputc(*p, jniIn->stream);
                break;
        }
    }
    va_end(ap); 

    fflush(jniIn->stream);
}

size_t jni_in_read(void *ptr, size_t size, size_t nmemb) {
    fwrite(jniIn->buf , 1, jniIn->len, stdout);
    return fread(ptr, size, nmemb, jniIn->stream);
}

int jni_in_getc(void) {
    return getc(jniIn->stream);
}

int jni_in_seek(long int offset, int whence) {
    return fseek(jniIn->stream, offset, whence);
}

void jni_out_printf(char *fmt, ...) {
    va_list ap; 
    char *p, *sval;
    int ival;
    unsigned int uval;
    unsigned long int luval;
    unsigned long long int lluval;
    double dval;

    va_start(ap, fmt);
    for (p = fmt; *p; p++) {
        if (*p != '%') {
            fputc(*p, jniOut->stream);
            continue;
        }
        switch (*++p) {
            case 'c':
                ival = va_arg(ap, int);
                fprintf(jniOut->stream, "%c", ival);
                break;
            case 'd':
            case 'i':
                ival = va_arg(ap, int);
                fprintf(jniOut->stream, "%d", ival);
                break;
            case 'f':
                dval = va_arg(ap, double);
                fprintf(jniOut->stream, "%f", dval);
                break;
            case 'l':
                if (*(p + 1) == 'l' && *(p + 2) == 'u') {
                    lluval = va_arg(ap, unsigned long long int);
                    fprintf(jniOut->stream, "%llu", lluval);
                    p += 2;
                } else if (*(p + 1) == 'u') {
                    luval = va_arg(ap, unsigned long int);
                    fprintf(jniOut->stream, "%lu", luval);
                    p += 1;
                }
                break;
            case 'o':
                ival = va_arg(ap, int);
                fprintf(jniOut->stream, "%o", ival);
                break;
            case 's':
                for(sval = va_arg(ap, char *); *sval; sval++)
                    fputc(*sval, jniOut->stream);
                break;
            case 'u':
                uval = va_arg(ap, unsigned int);
                fprintf(jniOut->stream, "%u", uval);
                break;
            case 'x':
                uval = va_arg(ap, uint);
                fprintf(jniOut->stream, "%x", uval);
                break;
            default:
                fputc(*p, jniOut->stream);
                break;
        }
    }
    va_end(ap); 

    fflush(jniOut->stream);
}

int jni_out_puts(const char *str) {
    int i = fputs(str, jniOut->stream);
    fflush(jniOut->stream);
    return i;
}

int jni_out_vprintf(const char *format, va_list arg) {
    int i = vfprintf(jniOut->stream, format, arg);
    fflush(jniOut->stream);
    return i;
}

void jni_err_printf(char *fmt, ...) {
    va_list ap; 
    char *p, *sval;
    int ival;
    double dval;

    va_start(ap, fmt);
    for (p = fmt; *p; p++) {
        if (*p != '%') {
            fputc(*p, jniErr->stream);
            continue;
        }
        switch (*++p) {
            case 'd':
                ival = va_arg(ap, int);
                fprintf(jniErr->stream, "%d", ival);
                break;
            case 'f':
                dval  = va_arg(ap, double);
                fprintf(jniErr->stream, "%f", dval);
                break;
            case 's':
                for(sval = va_arg(ap, char *); *sval; sval++)
                    fputc(*sval, jniErr->stream);
                break;
            default:
                fputc(*p, jniErr->stream);
                break;
        }
    }
    va_end(ap); 

    fflush(jniErr->stream);
}

int jni_err_puts(const char *str) {
    int i = fputs(str, jniErr->stream);
    fflush(jniErr->stream);
    return i;
}

void jni_perror(const char *str, int errnum) {
    fprintf(jniErr->stream, "%s: %s\n", str, strerror(errnum));
    fflush(jniErr->stream);
}

JNIEXPORT jobjectArray JNICALL Java_com_wgtools_WgSubcommand_wgCommand
  (JNIEnv *env, __attribute__((unused))jobject thisObj, jobjectArray args) {

    PROG_NAME = JNI_PROG_NAME;

    streams_alloc();
    exitCode = 1;
    if (args != NULL) {
        int argc = (*env)->GetArrayLength(env, args);
        const char *argv[argc];
        jstring *temp[argc];
        for (int i = 0; i < argc; i++) {
            temp[i] = (jstring *)((*env)->GetObjectArrayElement(env, args, i));
            argv[i] = (*env)->GetStringUTFChars(env, (jstring)temp[i], NULL);
        }

	    size_t i = 0;
	    for (; i < subcommands_count; ++i) {
		    if (!strcmp(argv[0], subcommands[i].subcommand)) {
			    exitCode = subcommands[i].function(argc, argv);
                break;
            }
        }

        if (i == subcommands_count) {
	        fprintf(jniErr->stream, "Invalid subcommand: `%s'\n", argv[1]);
	        show_usage(jniErr->stream);
        }

        for (int i = 0; i < argc; i++)
            (*env)->ReleaseStringUTFChars(env, (jstring)temp[i], argv[i]);

    }

    jobjectArray array = streams_to_array(env);
    streams_dealloc();

    return array;
}
