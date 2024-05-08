// SPDX-License-Identifier: GPL-2.0
/*
 * Copyright (C) 2015-2020 Jason A. Donenfeld <Jason@zx2c4.com>. All Rights Reserved.
 */

#include <errno.h>
#include <stdio.h>
#include <string.h>

#include "curve25519.h"
#include "encoding.h"
#include "subcommands.h"
#include "ctype.h"
#include "jni-wg.h"

int pubkey_main(int argc, const char *argv[])
{
	uint8_t key[WG_KEY_LEN] __attribute__((aligned(sizeof(uintptr_t))));
	char base64[WG_KEY_LEN_BASE64];

	if (argc != 2) {
		jni_err_printf("Usage: %s %s <private-key>\n", PROG_NAME, argv[0]);
		return 1;
	}

    strcpy(base64, argv[1]);

	base64[WG_KEY_LEN_BASE64 - 1] = '\0';
	if (!key_from_base64(key, base64)) {
		jni_err_printf("%s: Key is not the correct length or format\n", PROG_NAME);
		return 1;
	}
	curve25519_generate_public(key, key);
	key_to_base64(base64, key);
	jni_out_puts(base64);
	return 0;
}
