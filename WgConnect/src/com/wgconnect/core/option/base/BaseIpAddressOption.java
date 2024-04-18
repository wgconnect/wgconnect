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
package com.wgconnect.core.option.base;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.nio.ByteBuffer;

import com.wgconnect.core.util.Utils;

/**
 * BaseIpAddressOption 
 * 
 * The abstract base class for IP address options.
 *  
 * @author JagornetDhcp version: A. Gregory Rabil
 * @author WgConnect version: wgconnect@proton.me
 */
public abstract class BaseIpAddressOption extends BaseOption {

    protected String ipAddress;

    public BaseIpAddressOption() {
        this(null);
    }

    public BaseIpAddressOption(String ipAddress) {
        super();
        this.ipAddress = ipAddress;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    @Override
    public int getLength() {
        if (!super.isV4()) {
            return 16;		// 128-bit IPv6 address
        } else {
            return 4;		// 32-bit IPv4 address
        }
    }

    @Override
    public ByteBuffer encode() throws IOException {
        ByteBuffer buf = super.encodeCodeAndLength();
        if (ipAddress != null) {
            InetAddress inetAddr;
            if (!super.isV4()) {
                inetAddr = Inet6Address.getByName(ipAddress);
            } else {
                inetAddr = Inet4Address.getByName(ipAddress);
            }
            buf.put(inetAddr.getAddress());
        }
        return (ByteBuffer) buf.flip();
    }

    @Override
    public void decode(ByteBuffer buf) throws IOException {
        int len = super.decodeLength(buf);
        if ((len > 0) && (len <= buf.remaining())) {
            if (!super.isV4()) {
                ipAddress = decodeIpAddress(buf);
            } else {
                ipAddress = decodeIpV4Address(buf);
            }
        }
    }

    /**
     * Convert an IPv6 address received from the wire to a string.
     *
     * @param buf the ByteBuffer containing the IPv6 address to be decoded from the wire
     * @return the string representation of the IPv6 address
     * @throws IOException
     */
    public static String decodeIpAddress(ByteBuffer buf) throws IOException {
        byte[] b = new byte[16];
        buf.get(b);
        InetAddress inetAddr = InetAddress.getByAddress(b);
        
        return inetAddr.getHostAddress();
    }

    /**
     * Convert an IPv4 address received from the wire to a string.
     *
     * @param buf the ByteBuffer containing the IPv4 address to be decoded from the wire
     * @return the string representation of the IPv4 address
     * @throws IOException
     */
    public static String decodeIpV4Address(ByteBuffer buf) throws IOException {
        byte[] b = new byte[4];
        buf.get(b);
        InetAddress inetAddr = InetAddress.getByAddress(b);
        
        return inetAddr.getHostAddress();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(Utils.LINE_SEPARATOR);
        sb.append(super.getName()).append(": ipAddress = ").append(ipAddress);
        
        return sb.toString();
    }
}
