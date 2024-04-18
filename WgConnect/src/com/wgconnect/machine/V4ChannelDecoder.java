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

import java.net.InetSocketAddress;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelHandler;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.handler.codec.oneone.OneToOneDecoder;

import com.wgconnect.core.message.V4Message;
import com.wgconnect.core.util.Constants;
import com.wgconnect.core.util.WgConnectLogger;

/**
 * V4ChannelDecoder
 * 
 * The protocol decoder used by when receiving V4 packets.
 * 
 * @author JagornetDhcp version: A. Gregory Rabil
 * @author WgConnect version: wgconnect@proton.me
 */
@ChannelHandler.Sharable
public class V4ChannelDecoder extends OneToOneDecoder {

    private static final WgConnectLogger log = WgConnectLogger.getLogger(V4ChannelDecoder.class);

    protected InetSocketAddress localSocketAddress = null;

    protected InetSocketAddress remoteSocketAddress = null;

    protected boolean ignoreSelfPackets;

    public V4ChannelDecoder(InetSocketAddress localSocketAddress, boolean ignoreSelfPackets) {
        this.localSocketAddress = localSocketAddress;
        this.ignoreSelfPackets = ignoreSelfPackets;
    }

    /*
     * Decodes a received ChannelBuffer into a V4Message.
     * (non-Javadoc)
     * @see org.jboss.netty.handler.codec.oneone.OneToOneDecoder#decode(org.jboss.netty.channel.ChannelHandlerContext,
     * org.jboss.netty.channel.Channel, java.lang.Object)
     */
    @Override
    protected Object decode(ChannelHandlerContext ctx, Channel channel, Object msg) throws Exception {
        if (msg instanceof ChannelBuffer) {
            ChannelBuffer buf = (ChannelBuffer) msg;
            V4Message dhcpMessage = V4Message.decode(buf.toByteBuffer(), localSocketAddress, remoteSocketAddress);

            return dhcpMessage;
        } else {
            log.error("Unknown message object class: " + (msg != null ? msg.getClass() : ""));
            return msg;
        }
    }

    /* (non-Javadoc)
     * @see org.jboss.netty.handler.codec.oneone.OneToOneDecoder#handleUpstream(org.jboss.netty.channel.ChannelHandlerContext,
     * org.jboss.netty.channel.ChannelEvent)
     */
    @Override
    public void handleUpstream(ChannelHandlerContext ctx, ChannelEvent evt) throws Exception {
        if (evt instanceof MessageEvent) {
            remoteSocketAddress = (InetSocketAddress) ((MessageEvent) evt).getRemoteAddress();
        }
        
        super.handleUpstream(ctx, evt);
    }
}
