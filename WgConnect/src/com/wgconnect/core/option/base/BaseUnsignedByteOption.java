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
import java.nio.ByteBuffer;

import com.wgconnect.core.util.Utils;

/**
 * BaseUnsignedByteOption
 * 
 * The abstract base class for unsigned byte options.
 * 
 * @author JagornetDhcp version: A. Gregory Rabil
 * @author WgConnect version: wgconnect@proton.me
 */
public abstract class BaseUnsignedByteOption extends BaseOption {

    protected short unsignedByte;

    public BaseUnsignedByteOption() {
        this((short) 0);
    }

    public BaseUnsignedByteOption(short unsignedByte) {
        super();
        this.unsignedByte = unsignedByte;
    }

    public short getUnsignedByte() {
        return unsignedByte;
    }

    public void setUnsignedByte(short unsignedByte) {
        this.unsignedByte = unsignedByte;
    }

    @Override
    public int getLength() {
        return 1;   // always one byte
    }

    @Override
    public ByteBuffer encode() throws IOException {
        ByteBuffer buf = super.encodeCodeAndLength();
        buf.put((byte) unsignedByte);

        return (ByteBuffer) buf.flip();
    }

    @Override
    public void decode(ByteBuffer buf) throws IOException {
        int len = super.decodeLength(buf);
        if ((len > 0) && (len <= buf.remaining())) {
            unsignedByte = Utils.getUnsignedByte(buf);
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(Utils.LINE_SEPARATOR);
        sb.append(super.getName()).append(": unsignedByte = ").append(unsignedByte);
        
        return sb.toString();
    }

}
