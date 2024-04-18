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

import com.wgconnect.core.message.V4Message;
import com.wgconnect.core.util.Constants;
import com.wgconnect.core.util.WgConnectLogger;
import com.wgconnect.config.ConnectConfig;

import java.net.InetAddress;
import java.util.Collections;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.Timer;

/**
 * BaseV4Processor
 * 
 * The base class for processing V4 connect messages.
 * 
 * @author JagornetDhcp version: A. Gregory Rabil
 * @author WgConnect version: wgconnect@proton.me
 */
public abstract class BaseV4Processor implements V4MessageProcessor {

    private static final WgConnectLogger log = WgConnectLogger.getLogger(BaseV4Processor.class);

    protected static ConnectConfig connectConfig = ConnectConfig.getInstance();

    protected final V4Message requestMsg;
    protected V4Message replyMsg;
    protected final InetAddress remoteInetAddr;
    protected static Set<V4Message> recentMsgs = Collections.synchronizedSet(new HashSet<>());
    protected static Timer recentMsgPruner = new Timer("RecentMsgPruner");
    protected Random random = new Random();

    /**
     * Instantiates an BaseV4Processor. Since this class is abstract,
     * this constructor is protected for implementing classes.
     *
     * @param requestMsg the received request message
     * @param remoteInetAddr the remote inet address
     */
    protected BaseV4Processor(V4Message requestMsg, InetAddress remoteInetAddr) {
        this.requestMsg = requestMsg;
        this.remoteInetAddr = remoteInetAddr;
    }

    /**
     * Process the request.
     *
     * @return a reply V4Message list
     */
    @Override
    public V4Message processMessage() {
        try {
            if (!preProcess()) {
                log.warn("Message dropped by preProcess");
                return null;
            }

            if (log.isDebugEnabled()) {
                log.info("Processing: " + requestMsg.toStringWithOptions(Constants.V4_MESSAGE_SENDER_SERVER));
            } else if (log.isInfoEnabled()) {
                log.info("Processing: " + requestMsg.toString(Constants.V4_MESSAGE_SENDER_SERVER));
            }

            if (!process()) {
                // don't log a warning for release, which has no reply message
                log.warn("Message dropped by processor");
            }

            if (log.isDebugEnabled()) {
                log.info("Returning: " + replyMsg.toStringWithOptions(Constants.V4_MESSAGE_SENDER_SERVER));
            } else if (log.isInfoEnabled()) {
                log.info("Returning: " + replyMsg.toString(Constants.V4_MESSAGE_SENDER_SERVER));
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
