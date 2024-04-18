/*
 * Copyright 2024 wgconnect@proton.me. All Rights Reserved.
 *
 * WgConnect is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * WgConnect is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with WgConnect.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.wgconnect.config;

import com.wgconnect.core.util.WgConnectLogger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

import org.apache.commons.lang3.StringUtils;

/**
 * WgInterfaceInfo
 * 
 * The Wg interface info.
 * 
 * @author: wgconnect@proton.me
 */
public class WgInterfaceInfo {

    private static final WgConnectLogger log = WgConnectLogger.getLogger(WgInterfaceInfo.class);

    public static final String CIDR_SEPARATOR = "/";

    String name;    
    
    String localPrivateKey;
    String localPublicKey;

    long listenPort;

    String fwmark;

    public static class Endpoint {
        static String SEPARATOR = ":";
        static String V6_ADDR_START = "[";
        static String V6_ADDR_END = "]";

        String addr;
        long port;

        public Endpoint(String endpoint) {
            if (endpoint.startsWith(V6_ADDR_START)) {
                addr = StringUtils.substringBetween(endpoint, V6_ADDR_START, V6_ADDR_END);
            } else {
                addr = StringUtils.substringBeforeLast(endpoint, SEPARATOR);
            }
    
            port = Long.parseLong(StringUtils.substringAfterLast(endpoint, SEPARATOR));
        }

        public String getAddr() {
            return addr;
        }

        public long getPort() {
            return port;
        }
        
        @Override
        public String toString() {
            return addr + StringUtils.SPACE + port;
        }
    }

    public static class Peer {        
        String publicKey;
        String presharedKey;
        Endpoint endpoint;
        List<String> allowedIps = new ArrayList<>();
        long latestHandshake;
        long receivedBandwidth;
        long sendBandwidth;
        String keepAlive;

        public String getPublicKey() {
            return publicKey;
        }

        public String getPresharedKey() {
            return presharedKey;
        }

        public Endpoint getEndpoint() {
            return endpoint;
        }

        public List<String> getAllowedIps() {
            return allowedIps;
        }

        public long getLatestHandshake() {
            return latestHandshake;
        }

        public long getReceivedBandwidth() {
            return receivedBandwidth;
        }

        public long getSentBandwidth() {
            return sendBandwidth;
        }

        public String getKeepAlive() {
            return keepAlive;
        }
    }

    List<Peer> peers = new ArrayList<>();

    public WgInterfaceInfo(String displayName) {
        this.name = displayName;
    }

    public WgInterfaceInfo parse(List<String> dumpLines) {
        if (dumpLines != null && !dumpLines.isEmpty()) {
            Scanner s = new Scanner(dumpLines.get(0));
            localPrivateKey = s.next();
            localPublicKey = s.next();
            listenPort = s.nextLong();
            fwmark = s.next();
            s.close();

            if (dumpLines.size() > 1) {
                for (String peerLine : dumpLines.subList(1, dumpLines.size())) {
                    s = new Scanner(peerLine);
                    Peer peer = new Peer();
                    peer.publicKey = s.next();
                    peer.presharedKey = s.next();
                    
                    peer.endpoint = new Endpoint(s.next());
                    
                    String token;
                    while ((token = s.next()).contains(CIDR_SEPARATOR)) { 
                        peer.allowedIps.add(token);
                    }
                    
                    peer.latestHandshake = Long.parseLong(token);
                    peer.receivedBandwidth = s.nextLong();
                    peer.sendBandwidth = s.nextLong();
                    peer.keepAlive = s.next();
                
                    s.close();
                    
                    peers.add(peer);
                }
            }
        }
        
        return this;
    }
    
    public String getName() {
        return name;
    }
    
    public String getLocalPrivateKey() {
        return localPrivateKey;
    }
    
    public String getLocalPublicKey() {
        return localPublicKey;
    }
    
    public long getLocalListenPort() {
        return listenPort;
    }
    
    public String getLocalFwmark() {
        return fwmark;
    }
    
    public List<Peer> getPeers() {
        return peers;
    }
}
