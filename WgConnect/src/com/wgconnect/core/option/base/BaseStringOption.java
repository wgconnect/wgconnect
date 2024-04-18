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
import com.wgconnect.core.util.WgConnectLogger;

/**
 * BaseStringOption
 * 
 * The abstract base class for string options.
 * 
 * @author JagornetDhcp version: A. Gregory Rabil
 * @author WgConnect version: wgconnect@proton.me
 */
public abstract class BaseStringOption extends BaseOption {

    private static final WgConnectLogger log = WgConnectLogger.getLogger(BaseStringOption.class);
    
    protected String string;

    public BaseStringOption() {
        this(null);
    }

    public BaseStringOption(String string) {
        this.string = string;
    }

    public String getString() {
        return string;
    }

    public void setString(String string) {
        this.string = string;
    }

    @Override
    public int getLength() {
        int len = 0;
        if (string != null) {
            len = string.length();
        }
        
        return len;
    }

    @Override
    public ByteBuffer encode() throws IOException {
        ByteBuffer buf = super.encodeCodeAndLength();
        if (string != null) {
            buf.put(string.getBytes());
        }
        
        return (ByteBuffer) buf.flip();
    }

    @Override
    public void decode(ByteBuffer buf) throws IOException {
        int len = super.decodeLength(buf);
        if ((len > 0) && (len <= buf.remaining())) {
            byte[] b = new byte[len];
            buf.get(b);
            string = new String(b);
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(Utils.LINE_SEPARATOR);
        sb.append(super.getName()).append(": string = ").append(string);
        
        return sb.toString();
    }
}
