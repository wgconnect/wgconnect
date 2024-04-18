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

import com.wgconnect.core.util.Constants;

import java.io.IOException;
import java.nio.ByteBuffer;

import com.wgconnect.core.util.WgConnectLogger;

/**
 * BaseDhcpOption
 * 
 * The abstract base class for options.
 * 
 * @author JagornetDhcp version: A. Gregory Rabil
 * @author WgConnect version: wgconnect@proton.me
 */
public abstract class BaseOption implements Option {

    private static final WgConnectLogger log = WgConnectLogger.getLogger(BaseOption.class);

    protected String name;
    protected int code;
    protected boolean v4;	// true only if this is a V4 option

    /**
     * Encode the option code and length fields of any option.
     *
     * @return the ByteBuffer containing the encoded code and length fields
     *
     * @throws IOException Signals that an I/O exception has occurred.
     */
    protected ByteBuffer encodeCodeAndLength() throws IOException {
        ByteBuffer buf;

        buf = ByteBuffer.allocate(2 * Constants.INTEGER_SIZE + getLength());
        buf.putInt(getCode());
        buf.putInt(getLength());

        return buf;
    }

    /**
     * Decode the WgConnect option length field of any WgConnect option. Because we have a
     * OptionFactory to build the option based on the code, then the code is
     * already decoded, so this method is invoked by the concrete class to decode the length.
     *
     * @param buf the ByteBuffer containing the opaqueData to be decoded
     * 
     * @return the length of the option, or zero if there is no opaqueData for
     *         the option
     * 
     * @throws IOException Signals that an I/O exception has occurred.
     */
    protected int decodeLength(ByteBuffer buf) throws IOException {
        if ((buf != null) && buf.hasRemaining()) {
            // already have the code, so length is next
            int len = buf.getInt();
            
            if (log.isDebugEnabled()) {
                log.info(getName() + " reports length = {}: bytes remaining in buffer = {}",
                    len, buf.remaining());
            }
            
            return len;
        }
        
        return 0;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        if (name != null) {
            return name;
        }

        String className = this.getClass().getSimpleName();
        if (className.startsWith("WgConnect")) {
            return className;
        }

        return "Option-" + this.getCode();
    }

    public void setCode(int code) {
        this.code = code;
    }

    @Override
    public int getCode() {
        return code;
    }

    @Override
    public boolean isV4() {
        return v4;
    }

    public void setV4(boolean v4) {
        this.v4 = v4;
    }
}
