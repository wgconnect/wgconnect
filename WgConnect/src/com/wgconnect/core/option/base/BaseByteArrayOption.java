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

import com.wgconnect.core.util.Utils;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * BaseByteArrayOption 
 * 
 * The abstract base class for byte array options.
 *  
 * @author: wgconnect@proton.me
 */
public class BaseByteArrayOption extends BaseOption {
    
	private  byte[] hex;

	public BaseByteArrayOption() {}
	
	public BaseByteArrayOption(byte[] hex) {
        this.hex = hex;
	}
	
	public byte[] getHex() {
        return hex;
	}
    
	public void setHex(byte[] hex) {
        this.hex = hex;
	}
    
    @Override
	public int getLength() {
        return hex.length;
	}
    
	@Override
    public ByteBuffer encode() throws IOException {
        ByteBuffer buf = super.encodeCodeAndLength();
        encode(buf);
        
        return (ByteBuffer) buf.flip();
    }

    public void encode(ByteBuffer buf) {
        if (hex != null) {
            buf.put(hex);
        }
	}
    
	public void encodeLengthAndData(ByteBuffer buf) {
        buf.putShort((short)hex.length);
        buf.put(hex);
	}
    
    @Override
    public void decode(ByteBuffer buf) throws IOException {
        int len = super.decodeLength(buf);
        if ((len > 0) && (len <= buf.remaining())) {
            int eof = buf.position() + len;
            if (buf.position() < eof) {
                decode(buf, len);
            }
        }
    }

	public void decode(ByteBuffer buf, int len) {
        if (len > 0) {
            byte[] data = new byte[len];
            buf.get(data);            
            hex = data;
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
        sb.append(Utils.toHexString(hex));

        return sb.toString();
    }
    
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + Arrays.hashCode(hex);

        return result;
    }
	
	@Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        return Arrays.equals(hex, (byte [])obj);
    }
}
