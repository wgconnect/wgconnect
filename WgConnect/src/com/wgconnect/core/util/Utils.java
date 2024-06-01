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

import com.wgconnect.config.WgInterfaceInfo;

import com.wgtools.Wg;

import inet.ipaddr.IPAddressSection;
import inet.ipaddr.IPAddressString;

import java.io.IOException;
import java.math.BigInteger;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.SocketException;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ReadOnlyBufferException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.List;
import java.util.TimeZone;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

/**
 * Utils
 * 
 * Utility class.
 * 
 * @author JagornetDhcp version: A. Gregory Rabil
 * @author WgConnect version: wgconnect@proton.me
 */
public class Utils {

    private static final WgConnectLogger log = WgConnectLogger.getLogger(Utils.class);

    public static String LINE_SEPARATOR = System.getProperty("line.separator");

    public static TimeZone GMT_TIMEZONE = TimeZone.getTimeZone("GMT");
    public static Calendar GMT_CALENDAR = Calendar.getInstance(GMT_TIMEZONE);

    public static DateFormat GMT_DATEFORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

    public static String toHexString(byte[] binary) {
        if (binary != null) {
            StringBuilder str = new StringBuilder(binary.length * 2);
            for (int i = 0; i < binary.length; i++) {
                int v = (binary[i] << 24) >>> 24;
                str.append(v < 0x10 ? "0" : "").append(Integer.toHexString(v));
            }
            
            return str.toString();
        }
        
        return null;
    }

    public static String toHexString(ByteBuffer bb) {
        bb.mark();
        StringBuilder sb = new StringBuilder("\n");
        int i = 0;
        while (bb.hasRemaining()) {
            sb.append(String.format("%02X ", bb.get()));
            if ((++i % 50) == 0) {
                sb.append("\n");
            }
        }
        bb.reset();

        return sb.toString();
    }

    /**
     * Reads one unsigned byte as a short integer.
     *
     * @param buf
     * @return 
     * @see org.apache.mina.core.buffer.IoBuffer.getUnsigned
     */
    public static final short getUnsignedByte(ByteBuffer buf) {
        return (short) (buf.get() & 0xff);
    }

    /**
     * Reads two bytes unsigned integer.
     *
     * @param buf
     * @return 
     * @see org.apache.mina.core.buffer.IoBuffer.getUnsignedShort
     */
    public static final int getUnsignedShort(ByteBuffer buf) {
        return (buf.getShort() & 0xffff);
    }

    /**
     * Reads four bytes unsigned integer.
     *
     * @param buf
     * @return 
     * @see org.apache.mina.core.buffer.IoBuffer.getUnsignedInt
     */
    public static final long getUnsignedInt(ByteBuffer buf) {
        return (buf.getInt() & 0xffffffffL);
    }

    public static final int getSignedInt(ByteBuffer buf) {
        return buf.getInt();
    }
    
    public static final BigInteger getUnsignedLong(ByteBuffer buf) {
        byte[] data = new byte[8];
        buf.get(data);
        return new BigInteger(data);
    }

    /**
     * Relative <i>get</i> method for reading an unsigned medium int value.
     *
     * <p>
     * Reads the next three bytes at this buffer's current position, composing
     * them into an int value according to the current byte order, and then
     * increments the position by three.</p>
     *
     * @param buf
     * @return The unsigned medium int value at the buffer's current position
     * @see org.apache.mina.core.buffer.IoBuffer.getUnsignedMediumInt
     */
    public static final int getUnsignedMediumInt(ByteBuffer buf) {
        int b1 = getUnsignedByte(buf);
        int b2 = getUnsignedByte(buf);
        int b3 = getUnsignedByte(buf);
        if (ByteOrder.BIG_ENDIAN.equals(buf.order())) {
            return b1 << 16 | b2 << 8 | b3;
        } else {
            return b3 << 16 | b2 << 8 | b1;
        }
    }

    /**
     * Relative <i>put</i> method for writing a medium int value.
     *
     * <p>
     * Writes three bytes containing the given int value, in the current byte
     * order, into this buffer at the current position, and then increments the
     * position by three.</p>
     *
     * @param buf
     * @param value The medium int value to be written
     *
     * @return This buffer
     *
     * @throws BufferOverflowException If there are fewer than three bytes
     * remaining in this buffer
     *
     * @throws ReadOnlyBufferException If this buffer is read-only
     * @see org.apache.mina.core.buffer.IoBuffer.putMediumInt
     */
    public static final ByteBuffer putMediumInt(ByteBuffer buf, int value) {
        byte b1 = (byte) (value >> 16);
        byte b2 = (byte) (value >> 8);
        byte b3 = (byte) value;

        if (ByteOrder.BIG_ENDIAN.equals(buf.order())) {
            buf.put(b1).put(b2).put(b3);
        } else {
            buf.put(b3).put(b2).put(b1);
        }

        return buf;
    }

    /**
     * Compare IP addresses to determine order
     *
     * @param ip1
     * @param ip2
     * @return -1 if ip1<ip2, 0 if ip1=ip2, 1 if ip1>ip2
     */
    public static int compareInetAddrs(InetAddress ip1, InetAddress ip2) {
        /*
    	BigInteger bi1 = new BigInteger(ip1.getAddress());
    	BigInteger bi2 = new BigInteger(ip2.getAddress());
    	return bi1.compareTo(bi2);
         */
        byte[] ba1 = ip1.getAddress();
        byte[] ba2 = ip2.getAddress();

        // general ordering: ipv4 before ipv6
        if (ba1.length < ba2.length) {
            return -1;
        }
        if (ba1.length > ba2.length) {
            return 1;
        }

        // we have 2 ips of the same type, so we have to compare each byte
        for (int i = 0; i < ba1.length; i++) {
            int b1 = unsignedByteToInt(ba1[i]);
            int b2 = unsignedByteToInt(ba2[i]);
            if (b1 == b2) {
                continue;
            }
            if (b1 < b2) {
                return -1;
            } else {
                return 1;
            }
        }
        
        return 0;
    }

    private static int unsignedByteToInt(byte b) {
        return (int) b & 0xFF;
    }

    public static boolean inclusiveBetween(InetAddress ip, InetAddress start, InetAddress end) {
        return ((compareInetAddrs(ip, start) >= 0) && (compareInetAddrs(ip, end) <= 0));
    }

    /**
     * Use this instead of InetSocketAddress.toString() to avoid DNS lookup -
     * i.e. faster.
     *
     * @param saddr
     * @return saddr as a string
     */
    public static String socketAddressAsString(InetSocketAddress saddr) {
        if (saddr == null) {
            return null;
        } else {
            return saddr.getAddress().getHostAddress() + ":" + saddr.getPort();
        }
    }

    /**
     * Get the IPv6 link-local address for the given network interface.
     *
     * @param netIf the network interface
     * @return the first IPv6 link-local address recorded on the interface
     */
    public static InetAddress netIfIPv6LinkLocalAddress(NetworkInterface netIf) {
        InetAddress v6Addr = null;
        Enumeration<InetAddress> ifAddrs = netIf.getInetAddresses();
        while (ifAddrs.hasMoreElements()) {
            InetAddress addr = ifAddrs.nextElement();
            if ((addr instanceof Inet6Address) && addr.isLinkLocalAddress()) {
                // choose the link-local IPv6 address, which should be unique
                // because it it must be the EUI-64 auto-configured address
                v6Addr = addr;
            }
        }
        
        return v6Addr;
    }

    /**
     * Get the IPv4 link-local address for the given network interface.
     *
     * @param netIf the network interface
     * @return the first IPv4 link-local address recorded on the interface
     */
    public static InetAddress netIfLinkLocalAddress(NetworkInterface netIf) {
        InetAddress addr = null;
        Enumeration<InetAddress> ifAddrs = netIf.getInetAddresses();
        while (ifAddrs.hasMoreElements()) {
            addr = ifAddrs.nextElement();
            if (addr.isLinkLocalAddress()) {
                break;
            }
        }
        
        return addr;
    }

    public static boolean isInRange(InetAddress inetAddr, InetAddress startAddr, InetAddress endAddr) {
        return ((Utils.compareInetAddrs(inetAddr, startAddr) >= 0) &&
                (Utils.compareInetAddrs(inetAddr, endAddr) <= 0));
    }
    
    public static ServerSocket createServerSocket(String inetAddrStr, int port, int timeout) {
        ServerSocket serverSocket = null;
        try {
            InetAddress inetAddr = InetAddress.getByName(inetAddrStr);
            serverSocket = new ServerSocket(port, 1, inetAddr);
            serverSocket.setSoTimeout(timeout);
            serverSocket.setReuseAddress(true);
        } catch (IOException ex) {
            log.error("Exception \'" + ex + "\' creating socket on address " + inetAddrStr);
        }
        
        return serverSocket;
    }

    public static int getAvailablePort(String inetAddrStr) {
        int port = 0;
		ServerSocket socket = null;
		try {
			socket = new ServerSocket(0, 0, InetAddress.getByName(inetAddrStr));
			socket.setReuseAddress(true);
			port = socket.getLocalPort();
			try {
				socket.close();
			} catch (IOException ex) {
                log.error("Exception \'" + ex + "\' closing ServerSocket for address " + inetAddrStr);
			}
			return port;
		} catch (IOException ex) { 
            log.error("Exception \'" + ex + "\' finding available port for address " + inetAddrStr);
		} finally {
			if (socket != null) {
				try {
					socket.close();
				} catch (IOException ex) {
                    log.error("Exception \'" + ex + "\' closing ServerSocket for address " + inetAddrStr);
				}
			}
		}
		log.error("Could not find a free TCP/IP port");
        
        return port;
	}

    public static String generateIfName(String ifPrefix) {
        long availableNumber = 1;
        
        for (WgInterfaceInfo netIf : getAllWgNetIfs()) {
            if (netIf.getName().startsWith(ifPrefix)) {
                String numStr = StringUtils.removeStart(netIf.getName(), ifPrefix);
                if (StringUtils.isNumeric(numStr)) {
                    long ifNum = Long.parseLong(numStr);
                    if (ifNum >= availableNumber) {
                        availableNumber = ifNum + 1;
                    }
                }
            }
        }

        return ifPrefix + availableNumber;
    }
    
    public static boolean isWgConnectInterface(String ifName) {
        // Find any existing wg interface with the given name
        return getAllWgNetIfs()
            .stream()
            .anyMatch(i -> i.getName().equalsIgnoreCase(ifName));
    }
    
    public static String getWgConnectIfByPrefixAndRemotePublicKey(String ifPrefix, String remotePublicKey) {
        String ifName = null;
        for (WgInterfaceInfo netIf : getAllWgNetIfs()) {
            if (netIf.getName().startsWith(ifPrefix)) {
                for (WgInterfaceInfo.Peer peer : netIf.getPeers()) {
                    if (StringUtils.equals(peer.getPublicKey(), remotePublicKey)) {
                        ifName = netIf.getName();
                    }
                }
            }
        }
        
        return ifName;
    }
    
    public static String getWgConnectIfByPrefixAndEndpointAddr(String ifPrefix, String endpointAddr) {
        String ifName = null;
        for (WgInterfaceInfo netIf : getAllWgNetIfs()) {
            if (netIf.getName().startsWith(ifPrefix)) {
                for (WgInterfaceInfo.Peer peer : netIf.getPeers()) {
                    if (StringUtils.equals(peer.getEndpoint().getAddr(), endpointAddr)) {
                        ifName = netIf.getName();
                        break;
                    }
                }
            }
        }
        
        return ifName;
    }
    
    public static String getAnyExistingWgConnectIfByPrefix(String ifPrefix) {
        WgInterfaceInfo netIf = getAllWgNetIfs()
            .stream()
            .filter(i -> i.getName().startsWith(ifPrefix)) 
            .findFirst()
            .orElse(null);
        
        return netIf != null ? netIf.getName() : null;
    }
    
    public static List<String> getAllExistingNetIfsByPrefix(String ifPrefix) {
        // Find all existing interface with the given prefix
        List<String> ifNames = new ArrayList<>();
        getAllWgNetIfs()
            .stream()
            .filter(i -> i.getName().startsWith(ifPrefix))
            .forEachOrdered(i -> ifNames.add(i.getName()));
        
        return ifNames.stream().sorted().collect(Collectors.toList());

    }
    
    public static String getNextAvailableNetIfName(String ifPrefix, long currentIfaceNum) {
        // Look for an available interface from the existing wg interfaces, if any
        for (WgInterfaceInfo netIf : getAllWgNetIfs()) {
            if (netIf.getName().startsWith(ifPrefix)) {
                String numStr = StringUtils.removeStart(netIf.getName(), ifPrefix);
                if (StringUtils.isNumeric(numStr)) {
                    if (Long.parseLong(numStr) > currentIfaceNum) {
                        currentIfaceNum = Long.parseLong(numStr);
                        break;
                    }
                }
            }
        }

        // Generate a new wg interface name
        return String.format("%s%d", ifPrefix, currentIfaceNum + 1);
    }
    
    public static WgInterfaceInfo getWgNetIfByName(String netIfName) {
        return getAllWgNetIfs()
            .stream()
            .filter(i -> i.getName().equalsIgnoreCase(netIfName)) 
            .findFirst()
            .orElse(null);
    }
    
    public static WgInterfaceInfo getWgIfByName(String wgIfName) {
        return new WgInterfaceInfo(wgIfName).parse(new Wg().getDumpFromInterface(wgIfName));
    }
    
    public static WgInterfaceInfo getWgNetIfByExclusion(String ifPrefix, String remoteInetAddr) {
        // Look for an available existing interface that excludes the given remoteInetAddr
        WgInterfaceInfo wgNetIf = null;
        for (WgInterfaceInfo netIf : getAllWgNetIfs()) {
            if (netIf.getName().startsWith(ifPrefix)) {
                for (WgInterfaceInfo.Peer peer : netIf.getPeers()) {
                    if (!StringUtils.equals(peer.getEndpoint().getAddr(), remoteInetAddr)) {
                        wgNetIf = netIf;
                        break;
                    }
                }
            }
        }
        
        return wgNetIf;
    }
    
    public static WgInterfaceInfo getWgNetIfByLocalAndRemotePublicKeys(String localPublicKey, String remotePublicKey) {
        WgInterfaceInfo wgNetIf = null;
        for (WgInterfaceInfo netIf : getAllWgNetIfs()) {
            if (StringUtils.equals(netIf.getLocalPublicKey(), localPublicKey)) {
                for (WgInterfaceInfo.Peer peer : netIf.getPeers()) {
                    if (StringUtils.equals(peer.getPublicKey(), remotePublicKey)) {
                        wgNetIf = netIf;
                        break;
                    }
                }
            }
        }
       
        return wgNetIf;
    }
    
    public static List<WgInterfaceInfo> getAllV6WgNetIfs() {
        Wg wg = new Wg();
        List<WgInterfaceInfo> wgNetIfs = new ArrayList<>();
        try {
            Enumeration<NetworkInterface> netIfs = NetworkInterface.getNetworkInterfaces();
            while (netIfs.hasMoreElements()) {
                NetworkInterface netIf = netIfs.nextElement();
                if (netIf.getDisplayName().startsWith(Constants.TUNNEL_V6_IF_NAME_PREFIX)) {
                    List<String> netIfDump = wg.getDumpFromInterface(netIf.getDisplayName());
                    wgNetIfs.add(new WgInterfaceInfo(netIf.getDisplayName()).parse(netIfDump));
                }
            }
        } catch (SocketException ex) {
            log.error("SocketException: " + ex);
        }
        
        return wgNetIfs;
    }
    
    public static List<WgInterfaceInfo> getAllV4WgNetIfs() {
        Wg wg = new Wg();
        List<WgInterfaceInfo> wgNetIfs = new ArrayList<>();
        try {
            Enumeration<NetworkInterface> netIfs = NetworkInterface.getNetworkInterfaces();
            while (netIfs.hasMoreElements()) {
                NetworkInterface netIf = netIfs.nextElement();
                if (netIf.getDisplayName().startsWith(Constants.TUNNEL_V4_IF_NAME_PREFIX)) {
                    List<String> netIfDump = wg.getDumpFromInterface(netIf.getDisplayName());
                    wgNetIfs.add(new WgInterfaceInfo(netIf.getDisplayName()).parse(netIfDump));
                }
            }
        } catch (SocketException ex) {
            log.error("SocketException: " + ex);
        }
        
        return wgNetIfs;
    }
    
    public static List<WgInterfaceInfo> getAllWgNetIfs() {
        List<WgInterfaceInfo> wgNetIfs = new ArrayList<>();
        wgNetIfs.addAll(getAllV4WgNetIfs());
        wgNetIfs.addAll(getAllV6WgNetIfs());
        
        return wgNetIfs;
    }
    
    public static boolean isMulticastAddress(String addrStr) {
        return new IPAddressString(addrStr).getAddress().isMulticast();
    }
    
    public static boolean checkAddressMatch(String firstAddr, String secondAddr, int networkMask) {
        IPAddressSection firstAddrSect = new IPAddressString(firstAddr).getAddress().getNetworkSection(networkMask, false);
        IPAddressSection secondAddrSect = new IPAddressString(secondAddr).getAddress().getNetworkSection(networkMask, false);
        
        return firstAddrSect.matchesWithMask(secondAddrSect, firstAddrSect.getNetworkMask());
    }
    
    public static boolean checkAddressMatch(String firstAddr, String secondAddr, String networkMask) {
        return checkAddressMatch(firstAddr, secondAddr, Integer.parseInt(networkMask));
    }
    
    public static KeyPair generateCryptoKeyPair(String algorithm, int keysize) {
        KeyPair pair = null;
        
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance(algorithm);
            generator.initialize(keysize);
            pair = generator.generateKeyPair();
        } catch (NoSuchAlgorithmException  ex) {
            log.info("Exception: {}", ex);
        }
       
        return pair;
    }
}
