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

import java.nio.ByteBuffer;
import java.util.Arrays;

import com.wgconnect.core.util.Utils;

import org.apache.commons.lang3.StringUtils;

/**
 * BaseOpaqueData.
 *
 * @author JagornetDhcp version:
 * @author WgConnect version: wgconnect@proton.me
 */
public class BaseOpaqueData {

    private String ascii;
    private byte[] hex;

    public BaseOpaqueData() {
    }

    public BaseOpaqueData(String ascii) {
        this.ascii = ascii;
    }

    public BaseOpaqueData(byte[] hex) {
        this.hex = hex;
    }

    public String getAscii() {
        return ascii;
    }

    public void setAscii(String ascii) {
        this.ascii = ascii;
    }

    public byte[] getHex() {
        return hex;
    }

    public void setHex(byte[] hex) {
        this.hex = hex;
    }

    public int getLength() {
        if (ascii != null) {
            return ascii.length();
        } else {
            return hex.length;
        }
    }

    public void encode(ByteBuffer buf) {
        if (ascii != null) {
            buf.put(ascii.getBytes());
        } else if (hex != null) {
            buf.put(hex);
        }
    }

    public void encodeLengthAndData(ByteBuffer buf) {
        if (ascii != null) {
            buf.putShort((short) ascii.length());
            buf.put(ascii.getBytes());
        } else {
            buf.putShort((short) hex.length);
            buf.put(hex);
        }
    }

    public void decode(ByteBuffer buf, int len) {
        if (len > 0) {
            byte[] data = new byte[len];
            buf.get(data);
            String str = new String(data);
            if (str.matches("\\p{Print}+")) {
                ascii = str;
            } else {
                hex = data;
            }
        }
    }

    public void decodeLengthAndData(ByteBuffer buf) {
        int len = buf.getInt();
        if (len > 0) {
            decode(buf, len);
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (ascii != null) {
            sb.append(ascii);
        } else {
            sb.append(Utils.toHexString(hex));
        }

        return sb.toString();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((ascii == null) ? 0 : ascii.hashCode());
        result = prime * result + Arrays.hashCode(hex);

        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null) {
            return false;
        }

        if (getClass() != obj.getClass()) {
            return false;
        }

        BaseOpaqueData other = (BaseOpaqueData) obj;
        if (ascii == null) {
            if (other.ascii != null) {
                return false;
            }
        } else if (!StringUtils.equals(ascii, other.ascii)) {
            return false;
        }

        return Arrays.equals(hex, other.hex);
    }
}
