// SPDX-License-Identifier: GPL-2.0
/*
 * Copyright (C) 2015-2020 Jason A. Donenfeld <Jason@zx2c4.com>. All Rights Reserved.
 */

#include <arpa/inet.h>
#include <netinet/in.h>
#include <sys/socket.h>
#include <net/if.h>
#include <stddef.h>
#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#include <netdb.h>
#include <errno.h>

#include "containers.h"
#include "encoding.h"
#include "ipc.h"
#include "subcommands.h"
#include "jni-wg.h"

int showconf_main(int argc, const char *argv[])
{
	char base64[WG_KEY_LEN_BASE64];
	char ip[INET6_ADDRSTRLEN];
	struct wgdevice *device = NULL;
	struct wgpeer *peer;
	struct wgallowedip *allowedip;
	int ret = 1;

	if (argc != 2) {
		jni_err_printf("Usage: %s %s <interface>\n", PROG_NAME, argv[0]);
		return 1;
	}

	if (ipc_get_device(&device, argv[1])) {
		jni_perror("Unable to access interface", errno);
		goto cleanup;
	}

	jni_out_printf("[Interface]\n");
	if (device->listen_port)
		jni_out_printf("ListenPort = %u\n", device->listen_port);
	if (device->fwmark)
		jni_out_printf("FwMark = 0x%x\n", device->fwmark);
	if (device->flags & WGDEVICE_HAS_PRIVATE_KEY) {
		key_to_base64(base64, device->private_key);
		jni_out_printf("PrivateKey = %s\n", base64);
	}
	jni_out_printf("\n");
	for_each_wgpeer(device, peer) {
		key_to_base64(base64, peer->public_key);
		jni_out_printf("[Peer]\nPublicKey = %s\n", base64);
		if (peer->flags & WGPEER_HAS_PRESHARED_KEY) {
			key_to_base64(base64, peer->preshared_key);
			jni_out_printf("PresharedKey = %s\n", base64);
		}
		if (peer->first_allowedip)
			jni_out_printf("AllowedIPs = ");
		for_each_wgallowedip(peer, allowedip) {
			if (allowedip->family == AF_INET) {
				if (!inet_ntop(AF_INET, &allowedip->ip4, ip, INET6_ADDRSTRLEN))
					continue;
			} else if (allowedip->family == AF_INET6) {
				if (!inet_ntop(AF_INET6, &allowedip->ip6, ip, INET6_ADDRSTRLEN))
					continue;
			} else
				continue;
			jni_out_printf("%s/%d", ip, allowedip->cidr);
			if (allowedip->next_allowedip)
				jni_out_printf(", ");
		}
		if (peer->first_allowedip)
			jni_out_printf("\n");

		if (peer->endpoint.addr.sa_family == AF_INET || peer->endpoint.addr.sa_family == AF_INET6) {
			char host[4096 + 1];
			char service[512 + 1];
			socklen_t addr_len = 0;

			if (peer->endpoint.addr.sa_family == AF_INET)
				addr_len = sizeof(struct sockaddr_in);
			else if (peer->endpoint.addr.sa_family == AF_INET6)
				addr_len = sizeof(struct sockaddr_in6);
			if (!getnameinfo(&peer->endpoint.addr, addr_len, host, sizeof(host), service, sizeof(service), NI_DGRAM | NI_NUMERICSERV | NI_NUMERICHOST)) {
				if (peer->endpoint.addr.sa_family == AF_INET6 && strchr(host, ':'))
					jni_out_printf("Endpoint = [%s]:%s\n", host, service);
				else
					jni_out_printf("Endpoint = %s:%s\n", host, service);
			}
		}

		if (peer->persistent_keepalive_interval)
			jni_out_printf("PersistentKeepalive = %u\n", peer->persistent_keepalive_interval);

		if (peer->next_peer)
			jni_out_printf("\n");
	}
	ret = 0;

cleanup:
	free_wgdevice(device);
	return ret;
}
