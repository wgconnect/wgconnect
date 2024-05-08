// SPDX-License-Identifier: GPL-2.0
/*
 * Copyright (C) 2015-2020 Jason A. Donenfeld <Jason@zx2c4.com>. All Rights Reserved.
 */

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <errno.h>

#include "containers.h"
#include "config.h"
#include "ipc.h"
#include "subcommands.h"
#include "jni-wg.h"

int set_main(int argc, const char *argv[])
{
	struct wgdevice *device = NULL;
	int ret = 1;

	if (argc < 3) {
		jni_err_printf("Usage: %s %s <interface> [listen-port <port>] [fwmark <mark>] [private-key <base64 private-key>] [peer <base64 public key> [remove] [preshared-key <base64 key>] [endpoint <ip>:<port>] [persistent-keepalive <interval seconds>] [allowed-ips <ip1>/<cidr1>[,<ip2>/<cidr2>]...] ]...\n", PROG_NAME, argv[0]);
		return 1;
	}

	device = config_read_cmd(argv + 2, argc - 2);
	if (!device)
		goto cleanup;
	strncpy(device->name, argv[1], IFNAMSIZ -  1);
	device->name[IFNAMSIZ - 1] = '\0';

	if (ipc_set_device(device) != 0) {
		jni_perror("Unable to modify interface", errno);
		goto cleanup;
	}

	ret = 0;

cleanup:
	free_wgdevice(device);
	return ret;
}
