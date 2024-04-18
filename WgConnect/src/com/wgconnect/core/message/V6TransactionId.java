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
 */
package com.wgconnect.core.message;

import java.nio.ByteBuffer;

import com.wgconnect.core.util.Utils;

/**
 * V6TransactionId
 * 
 * A class representing the Transaction Id of a V6Message.
 * 
 * @author JagornetDhcp version: A. Gregory Rabil
 * @author WgConnect version: wgconnect@proton.me
 */
public class V6TransactionId {

    /**
     * Encode.
     * 
     * @param xId the transaction id to be encoded
     * 
     * @return the byte buffer
     */
    public static ByteBuffer encode(int xId) {
        ByteBuffer buf = ByteBuffer.allocate(3);
        Utils.putMediumInt(buf, xId);
        return (ByteBuffer) buf.flip();
    }

    /**
     * Decode.
     * 
     * @param buf the byte buffer
     * 
     * @return the int as the decoded transaction id
     */
    public static int decode(ByteBuffer buf) {
        return Utils.getUnsignedMediumInt(buf);
    }
}
