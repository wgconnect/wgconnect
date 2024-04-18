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
package com.wgconnect.machine.processor;

import com.wgconnect.core.message.V6Message;
import com.wgconnect.core.util.WgConnectLogger;
import com.wgconnect.core.util.Utils;
import com.wgconnect.config.ConnectConfig;
import com.wgconnect.core.util.Constants;

import java.net.InetAddress;

/**
 * BaseV6Processor
 * 
 * The base class for processing V6 messages.
 * 
 * @author JagornetDhcp version: A. Gregory Rabil
 * @author WgConnect version: wgconnect@proton.me
 */
public abstract class BaseV6Processor implements V6MessageProcessor {

    private static final WgConnectLogger log = WgConnectLogger.getLogger(BaseV6Processor.class);

    protected static ConnectConfig connectConfig = ConnectConfig.getInstance();

    protected final V6Message requestMsg;
    protected V6Message replyMsg;
    protected final InetAddress remoteInetAddr;

    /**
     * Instantiates an BaseV6Processor.  Since this class is
     * abstract, this constructor is protected for implementing classes.
     * 
     * @param requestMsg the received V6Message
     * @param remoteInetAddr the remote inet address
     */
    protected BaseV6Processor(V6Message requestMsg, InetAddress remoteInetAddr) {
        this.requestMsg = requestMsg;
        this.remoteInetAddr = remoteInetAddr;
    }

    /**
     * Process the request.
     * 
     * @return a reply V6Message list
     */
    @Override
    public V6Message processMessage() {
        try {
            if (!preProcess()) {
                log.warn("Message dropped by preProcess");
                return null;
            }

            if (log.isDebugEnabled()) {
                log.info("Processing: {}", requestMsg.toStringWithOptions(Constants.V6_MESSAGE_SENDER_SERVER));
            } else if (log.isInfoEnabled()) {
                log.info("Processing: {}", requestMsg.toString(Constants.V6_MESSAGE_SENDER_SERVER));
            }

            if (!process()) {
                if (!Utils.isMulticastAddress(requestMsg.getRemoteAddress().getAddress().getHostAddress())) {
                    log.warn("Message dropped by processor");
                }
                
                return null;
            }

            if (log.isDebugEnabled()) {
                log.info("Returning: " + replyMsg.toStringWithOptions(Constants.V6_MESSAGE_SENDER_SERVER));
                
            } else if (log.isInfoEnabled()) {
                log.info("Returning: " + replyMsg.toString());
            }
        } finally {
            if (!postProcess()) {
                log.warn("Message dropped by postProcess");
            }
        }

        return replyMsg;
    }

    /**
     * Pre process.
     * 
     * @return true if processing should continue
     */
    public boolean preProcess() {
        return true;
    }

    /**
     * Process.
     * 
     * @return true if a reply should be sent
     */
    public abstract boolean process();

    /**
     * Post process.
     * 
     * @return true if a reply should be sent
     */
    public boolean postProcess() {
        return true;
    }
}
