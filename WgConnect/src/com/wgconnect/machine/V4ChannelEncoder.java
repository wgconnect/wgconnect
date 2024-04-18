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
package com.wgconnect.machine;

import java.nio.ByteBuffer;

import org.jboss.netty.buffer.ByteBufferBackedChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandler;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.oneone.OneToOneEncoder;

import com.wgconnect.core.message.V4Message;
import com.wgconnect.core.util.WgConnectLogger;

/**
 * V4ChannelEncoder
 * 
 * The protocol encoder used when sending V4 packets.
 * 
 * @author JagornetDhcp version: A. Gregory Rabil
 * @author WgConnect version: wgconnect@proton.me
 */
@ChannelHandler.Sharable
public class V4ChannelEncoder extends OneToOneEncoder {

    private static final WgConnectLogger log = WgConnectLogger.getLogger(V4ChannelEncoder.class);

    /*  
     * Encode the requested V4Message into a ChannelBuffer.
     * (non-Javadoc)
     * @see org.jboss.netty.handler.codec.oneone.OneToOneEncoder#encode(org.jboss.netty.channel.ChannelHandlerContext,
     * org.jboss.netty.channel.Channel, java.lang.Object)
     */
    @Override
    public Object encode(ChannelHandlerContext ctx, Channel channel, Object msg) throws Exception {
        if (msg instanceof V4Message) {
            V4Message dhcpMessage = (V4Message) msg;
            ByteBuffer buf = dhcpMessage.encode();
            if (log.isDebugEnabled()) {
                log.info("Encoded message buffer limit=" + buf.limit());
            }
       
            return new ByteBufferBackedChannelBuffer(buf);
        } else {
            log.error("Unknown message object class: " + (msg != null ? msg.getClass() : ""));
            return msg;
        }
    }
}
