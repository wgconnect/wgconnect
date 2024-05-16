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
package com.wgconnect.core.util;

import inet.ipaddr.ipv4.IPv4Address;
import inet.ipaddr.ipv6.IPv6Address;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

/**
 * Constants
 * 
 * Defines the constants
 * 
 * @author JagornetDhcp version: A. Gregory Rabil
 * @author WgConnect version: wgconnect@proton.me
 */
public class Constants {
    
    private static final WgConnectLogger log = WgConnectLogger.getLogger(Constants.class);
	
    public static final String APP_NAME = "WgConnect";
    public static final String APP_VERSION = "1.0";

    public static final int INTEGER_SIZE = Integer.SIZE / Byte.SIZE;
    
    public static final String V4_CLIENT = "v4client";
    public static final String V6_CLIENT = "v6client";

    public static String HOME = "/opt/wgconnect";
	
    // Generic machine channel definitions
    public static final int MAX_CHANNEL_THREADS = 32;
    public static final int DEFAULT_RECEIVE_BUFFER_SIZE = 51200;
    public static final int DEFAULT_SEND_BUFFER_SIZE    = 51200;
    
    // Generic Cryptography definitions
    public static String GENERIC_CRYPTO_ALGORITHM = "RSA";
    public static int GENERIC_CRYPTO_KEYSIZE = 2048;
    
    // Database and Logging
    public static final String DATABASE_TYPE = "h2";
    public static final String DATABASE_DIRECTORY = "/var/lib/wgconnect/";
    public static final String DATABASE_FILENAME = "database." + DATABASE_TYPE;
    public static final String DATABASE_USERNAME = "wgconnect";
    public static final String DATABASE_PASSWORD = "wgconnect";
    
    public static final String LOG_DIRECTORY = "/var/log/wgconnect/";
    public static final String LOG_FILE = LOG_DIRECTORY + "log";

    public static final String DEFAULT_CONFIG_FILENAME =
        "file:" + (Constants.HOME + "/config/dhcpserver.config");
    
    public static final int DEFAULT_PERSISTENT_KEEPALIVE = 0; // in seconds
    
    public static final int V4_SUBNET_MASK_8 = IPv4Address.BITS_PER_SEGMENT;
    public static final int V4_SUBNET_MASK_16 = IPv4Address.BITS_PER_SEGMENT * 2;
    public static final int V4_SUBNET_MASK_24 = IPv4Address.BITS_PER_SEGMENT * 3;
    public static final int V4_SUBNET_MASK_32 = IPv4Address.BITS_PER_SEGMENT * 4;
    
    public static final int V6_SUBNET_MASK_16 = IPv6Address.BITS_PER_SEGMENT;
    public static final int V6_SUBNET_MASK_32 = IPv6Address.BITS_PER_SEGMENT * 2;
    public static final int V6_SUBNET_MASK_48 = IPv6Address.BITS_PER_SEGMENT * 3;
    public static final int V6_SUBNET_MASK_64 = IPv6Address.BITS_PER_SEGMENT * 4;
    public static final int V6_SUBNET_MASK_80 = IPv6Address.BITS_PER_SEGMENT * 5;
    public static final int V6_SUBNET_MASK_96 = IPv6Address.BITS_PER_SEGMENT * 6;
    public static final int V6_SUBNET_MASK_112 = IPv6Address.BITS_PER_SEGMENT * 7;
    public static final int V6_SUBNET_MASK_128 = IPv4Address.BITS_PER_SEGMENT * 8;
    
    public static enum IPVersion {
        V4("V4"),
        V6("V6");
        
        private final String type;
        
        IPVersion(String type) {
            this.type = type;
        }
        
        public boolean isV4() {
            return type.equalsIgnoreCase(V4.type);
        }
        
        public boolean isV6() {
            return type.equalsIgnoreCase(V6.type);
        }
        
        @Override
        public String toString() {
            return type;
        }
    }
    
    public static final String V4_TUNNEL_STATUS_DISCOVER = "Discover";
    public static final String V4_TUNNEL_STATUS_OFFER = "Offer";
    public static final String V4_TUNNEL_STATUS_REQUEST = "Request";
    public static final String V4_TUNNEL_STATUS_ACK = "Acknowledge";
    
    public static final String V6_TUNNEL_STATUS_SOLICIT = "Solicit";
    public static final String V6_TUNNEL_STATUS_ADVERTISE = "Advertise";
    public static final String V6_TUNNEL_STATUS_REQUEST = "Request";
    public static final String V6_TUNNEL_STATUS_REPLY = "Reply";

    public static final String TUNNEL_STATUS_TUNNEL_PING = "Tunnel Ping";
    public static final String TUNNEL_STATUS_TUNNEL_PING_REPLY = "Tunnel Ping Reply";
   
    public static final String TUNNEL_STATUS_PEER_CONFIG_ERROR = "Peer Config Error";
    public static final String TUNNEL_STATUS_UP = "Up";
    public static final String TUNNEL_STATUS_DOWN = "Down";
    
    public static final String TUNNEL_IF_BASE_NAME = "wgconnect";
    public static final String TUNNEL_V4_IF_NAME_PREFIX = "v4" + TUNNEL_IF_BASE_NAME;
    public static final String TUNNEL_V6_IF_NAME_PREFIX = "v6" + TUNNEL_IF_BASE_NAME;
    
    public static final String TUNNEL_ENDPOINT_TYPE_SERVER = "Server";
    public static final String TUNNEL_ENDPOINT_TYPE_CLIENT = "Client";
    
    public static final int TUNNEL_COM_PORT_ACCEPT_TIMEOUT = 30000;  // ms
    
    public static InetAddress WILDCARD_ADDR = null;
    public static InetAddress V4_ZEROADDR = null;
    public static InetAddress V6_ZEROADDR = null;
    public static InetAddress V6_MAX_ADDR = null;
    
    public static int V4_ZEROADDR_SUBNET_LENGTH = 0;
    public static int V6_ZEROADDR_SUBNET_LENGTH = 0;
    
    public static InetAddress V4_LOCALHOST_INET_ADDR = null;
    public static InetAddress V6_LOCALHOST_INET_ADDR = null;
    
    public static InetAddress V4_BROADCAST = null;

    public static InetAddress V4_MULTICAST_INET_ADDR = null;
    public static InetAddress V6_MULTICAST_INET_ADDR = null;

    public static final String V4_DEFAULT_INET_ADDR_STR = "0.0.0.0";
    public static final String V6_DEFAULT_INET_ADDR_STR = "::";
    public static final String V4_LOCALHOST_INET_ADDR_STR = "127.0.0.1";
    public static final String V6_LOCALHOST_INET_ADDR_STR = "::1";
    public static final String V4_DEFAULT_MULTICAST_INET_ADDR_STR = "224.0.0.1";
    public static final String V6_DEFAULT_MULTICAST_INET_ADDR_STR = "ff7E:230::1234";
    public static final String V6_MAX_INET_ADDR_STR = "ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff";

    public static final int V4_DEFAULT_TUNNEL_NETWORK_PREFIX_LEN = V4_SUBNET_MASK_8;
    public static final String V4_DEFAULT_TUNNEL_NETWORK = "10.0.0.0" + IPv4Address.PREFIX_LEN_SEPARATOR + V4_DEFAULT_TUNNEL_NETWORK_PREFIX_LEN;
    public static final int V4_MIN_TUNNEL_NETWORK_PREFIX_LEN = IPv4Address.BITS_PER_SEGMENT;
    public static final int V4_MAX_TUNNEL_NETWORK_PREFIX_LEN = V4_SUBNET_MASK_24;
    
    public static final int V6_DEFAULT_TUNNEL_NETWORK_PREFIX_LEN = V6_SUBNET_MASK_64;
    public static final String V6_DEFAULT_TUNNEL_NETWORK = "fc00::0" + IPv6Address.PREFIX_LEN_SEPARATOR + V6_DEFAULT_TUNNEL_NETWORK_PREFIX_LEN;
    public static final int V6_MIN_TUNNEL_NETWORK_PREFIX_LEN = IPv6Address.BITS_PER_SEGMENT;
    public static final int V6_MAX_TUNNEL_NETWORK_PREFIX_LEN = V6_SUBNET_MASK_112;
    
    static {
        try {
            WILDCARD_ADDR = (new InetSocketAddress(0)).getAddress();
            V4_ZEROADDR = InetAddress.getByName(V4_DEFAULT_INET_ADDR_STR);
            V6_ZEROADDR = InetAddress.getByName(V6_DEFAULT_INET_ADDR_STR);
            V6_MAX_ADDR = InetAddress.getByName(V6_MAX_INET_ADDR_STR);
            V4_LOCALHOST_INET_ADDR = InetAddress.getByName(V4_LOCALHOST_INET_ADDR_STR);
            V6_LOCALHOST_INET_ADDR = InetAddress.getByName(V6_LOCALHOST_INET_ADDR_STR);
            V4_MULTICAST_INET_ADDR = InetAddress.getByName(V4_DEFAULT_MULTICAST_INET_ADDR_STR);
            V6_MULTICAST_INET_ADDR = InetAddress.getByName(V6_DEFAULT_MULTICAST_INET_ADDR_STR);
        }
        catch (UnknownHostException ex) { 
			log.error("Failed to initialize Inet constants: " + ex);
		}
	}

    // Options
    public static final int OPTION_MESSAGE_TYPE = 153;
    public static final int OPTION_MESSAGE_SENDER = 154;
    
    public static final int OPTION_REMOTE_PHYS_INET_ADDR = 155;
    public static final int OPTION_LOCAL_PHYS_INET_ADDR = 156;
    
    public static final int OPTION_LOCAL_TUNNEL_INET_ADDR = 157;
    public static final int OPTION_REMOTE_TUNNEL_INET_ADDR = 158;
    
    public static final int OPTION_REMOTE_TUNNEL_INET_COM_PORT = 159;
    
    public static final int OPTION_INTERFACE_NAME = 160;
    
    public static final int OPTION_REMOTE_WG_PUBLIC_KEY = 161;
    public static final int OPTION_LOCAL_WG_PUBLIC_KEY = 162;
    
    public static final int OPTION_REMOTE_ENDPOINT_TYPE = 163;
    public static final int OPTION_LOCAL_ENDPOINT_TYPE = 164;
    
    public static final int OPTION_REMOTE_PHYS_INET_LISTEN_PORT = 165;
    public static final int OPTION_LOCAL_PHYS_INET_LISTEN_PORT = 166;
    
    public static final int OPTION_REMOTE_PHYS_INET_COM_PORT = 167;
    public static final int OPTION_LOCAL_PHYS_INET_COM_PORT = 168;
    
    public static final int OPTION_TUNNEL_ID = 169;
    public static final int OPTION_GENERIC_ID = 170;
    
    public static final int OPTION_KEEPALIVE_POLICY = 171;
    
    public static final int OPTION_TUNNEL_STATUS = 172;
    
    public static final int OPTION_GENERIC_RESPONSE = 173;

    public static final int OPTION_SPECIFIC_INFO = 174;
    
    public static final int OPTION_CLIENT_OTHER_PHYS_INET_ADDRS = 175;

    public static final int OPTION_GENERIC_PUBLIC_KEY = 176;
    public static final int OPTION_LOCAL_GENERIC_PUBLIC_KEY = 177;
    
    public static final int OPTION_PING_INET_ADDR = 178;
    public static final int OPTION_PING_INET_PORT = 179;
    
    public static final int OPTION_TUNNEL_NETWORK = 180;
    
    public static final int OPTION_EOF = 255;

    // Generic responses
    public static final int RESPONSE_ACCEPT = 0;
    public static final int RESPONSE_DECLINE_REDUNDANT_PUBLIC_KEY = 1;
    public static final int RESPONSE_DECLINE_TUNNEL_NETWORK = 2;
 
    // V6 Constants
    public static final int V6_PORT = 547;
    public static final int V6_MCAST_PORT = 548;

    // V6 Message Types - use short to support unsigned byte
    public static final short V6_MESSAGE_SENDER_SERVER = 1;
    public static final short V6_MESSAGE_SENDER_CLIENT = 2;
    
    public static final short V6_MESSAGE_TYPE_SOLICIT = 3;
    public static final short V6_MESSAGE_TYPE_ADVERTISE = 4;
    public static final short V6_MESSAGE_TYPE_REQUEST = 5;
    public static final short V6_MESSAGE_TYPE_REPLY = 6;
    
    public static final short V6_MESSAGE_TYPE_TUNNEL_PING = 7;
    public static final short V6_MESSAGE_TYPE_TUNNEL_PING_REPLY = 8;

    public static final short V6_MESSAGE_TYPE_INFO_REQUEST = 9;
    public static final short V6_MESSAGE_TYPE_INFO_REQUEST_REPLY = 10;
    
    public static final short V6_MESSAGE_TYPE_END = 11;
    
    public static final String[] V6_MESSAGE_STRING = {
        "Server",
        "Client",
        
        "Solicit",
        "Advertise",
        "Request",
        "Reply",
    
        "Tunnel Ping",
        "Tunnel Ping Reply",
        
        "Info Request",
        "Info Request Reply",
    };
    
    /**
     * Get the string representation of a V6 message type given the message type.
     * 
     * Note that the type is an unsigned byte, so we need a short to store it properly.
     * 
     * @param msg  the message type as a short (i.e. unsigned byte)
     * @return the message string
     */
    public static final String getV6MsgTypeString(short msg) {
        if ((msg >= V6_MESSAGE_SENDER_SERVER) && (msg < V6_MESSAGE_TYPE_END)) {
            return V6_MESSAGE_STRING[msg - V6_MESSAGE_SENDER_SERVER];
        }
        
        return "Invalid";
    }
    
    /**
     * Get the string representation of a V4 message sender given the message sender.
     * 
     * Note that the sender is an unsigned byte, so we need a short to store it properly.
     * 
     * @param msgSender  the message sender as a short (i.e. unsigned byte)
     * @return the message string
     */
    public static final String getV6MsgSenderString(short msgSender) {
        if ((msgSender >= V6_MESSAGE_SENDER_SERVER) && (msgSender <= V6_MESSAGE_SENDER_CLIENT)) {
            return V6_MESSAGE_STRING[msgSender - V6_MESSAGE_SENDER_SERVER];
        }
        
        return "Unknown";
    }
    
    // V4 Constants
    public static final int V4_PORT = 268;

    public static final int V4_OP_REQUEST = 101;
    public static final int V4_OP_REPLY = 102;

    // V4 Message Types

    public static final int V4_MESSAGE_SENDER_SERVER = 101;
    public static final int V4_MESSAGE_SENDER_CLIENT = 102;
    
    public static final int V4_MESSAGE_TYPE_DISCOVER = 103;
    public static final int V4_MESSAGE_TYPE_OFFER = 104;
    public static final int V4_MESSAGE_TYPE_REQUEST = 105;
    public static final int V4_MESSAGE_TYPE_ACK = 106;
    
    public static final int V4_MESSAGE_TYPE_TUNNEL_PING = 107;
    public static final int V4_MESSAGE_TYPE_TUNNEL_PING_REPLY = 108;
    
    public static final int V4_MESSAGE_TYPE_INFO_REQUEST = 109;
    public static final int V4_MESSAGE_TYPE_INFO_REQUEST_REPLY = 110;

    public static final int V4_MESSAGE_TYPE_END = 111;
    
    public static final String[] V4_MESSAGE_STRING = {
        "Server",
        "Client",
        
        "Discover",
        "Offer",
        "Request",
        "Ack",
        
        "Tunnel Ping",
        "Tunnel Ping Reply",
                
        "Info Request",
        "Info Request Reply",
    };
    
    /**
     * Get the string representation of a V4 message type given the message type.
     * 
     * Note that the type is an unsigned byte, so we need a short to store it properly.
     * 
     * @param msgType  the message type as a short (i.e. unsigned byte)
     * @return the message string
     */
    public static final String getV4MsgTypeString(short msgType) {
        if ((msgType >= V4_MESSAGE_SENDER_SERVER) && (msgType < V4_MESSAGE_TYPE_END)) {
            return V4_MESSAGE_STRING[msgType - V4_MESSAGE_SENDER_SERVER];
        }
        
        return "Unknown";
    }

    /**
     * Get the string representation of a V4 message sender given the message sender.
     * 
     * Note that the sender is an unsigned byte, so we need a short to store it properly.
     * 
     * @param msgSender  the message sender as a short (i.e. unsigned byte)
     * @return the message string
     */
    public static final String getV4MsgSenderString(short msgSender) {
        if ((msgSender >= V4_MESSAGE_SENDER_SERVER) && (msgSender <= V4_MESSAGE_SENDER_CLIENT)) {
            return V4_MESSAGE_STRING[msgSender - V4_MESSAGE_SENDER_SERVER];
        }
        
        return "Unknown";
    }
    
    // Response strings
    public static final String TUNNEL_STATUS_PEER_INFO_REQUEST_ACCEPT = "Peer Info Request Accept";
    public static final String TUNNEL_STATUS_PEER_INFO_REQUEST_DECLINE = "Peer Info Request Decline";
    
    // Machine thread message queue constants
    public static final int THREAD_MESSAGE_STOP = 1;
    
    public static final String[] THREAD_MESSAGE_STRING = {
        "Stop"
    };
    
    public static final String getThreadMsgString(int msg) {
        if ((msg >= THREAD_MESSAGE_STOP) && (msg <= THREAD_MESSAGE_STOP)) {
            return THREAD_MESSAGE_STRING[msg - THREAD_MESSAGE_STOP];
        }
        
        return "Unknown";
    }
}
