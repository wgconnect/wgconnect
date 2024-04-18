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

import com.wgconnect.WgConnect;
import com.wgconnect.core.message.V6Message;
import com.wgconnect.core.option.machine.TunnelIdOption;
import com.wgconnect.core.util.Constants;
import com.wgconnect.core.util.WgConnectLogger;
import com.wgconnect.db.persistence.PersistenceTunnel;
import com.wgconnect.gui.Gui;
import com.wgconnect.machine.V6Machine;

import java.net.InetAddress;

/**
 * V6PingProcessor
 * 
 * The main class for processing V6 ping messages.
 * 
 * @author: wgconnect@proton.me
 */
public class V6PingProcessor extends BaseV6Processor {

    private static final WgConnectLogger log = WgConnectLogger.getLogger(V6PingProcessor.class);

    private final V6Machine.ServerMachine v6ServerMachine;
    
    private PersistenceTunnel tunnel;
    
    private TunnelIdOption tunnelIdOption;

    /**
     * Instantiate a V6PingProcessor
     *
     * @param v6ServerMachine
     * @param pingMsg the ping message
     * @param remoteInetAddr the remote inet address
     */
    public V6PingProcessor(V6Machine.ServerMachine v6ServerMachine, V6Message pingMsg, InetAddress remoteInetAddr) {
        super(pingMsg, remoteInetAddr);
        
        this.v6ServerMachine = v6ServerMachine;
    }
    
    /* (non-Javadoc)
     * @see com.wgconnect.machine.processor.BaseV6Processor#preProcess()
     */
    @Override
    public boolean preProcess() {
        boolean process = true;

        tunnelIdOption = (TunnelIdOption) requestMsg.getOption(Constants.OPTION_TUNNEL_ID);

        if (tunnelIdOption != null) {
            tunnel = WgConnect.getTunnelByTunnelId(tunnelIdOption.getString());

            if (tunnel == null) {
                log.warn("Ignoring Ping message: No tunnel was found for the tunnel id {}", tunnelIdOption.getString());
                process = false;
            }
        } else {
            log.warn("Ignoring Ping message: The tunnel id was not sent");
            process = false;
        }

        return process;
    }
    
    /* (non-Javadoc)
     * @see com.wgconnect.machine.processor.BaseV6Processor#process()
     */
    @Override
    public boolean process() {

        tunnel.setState(Constants.TUNNEL_STATUS_UP);
        WgConnect.printTunnelCompleteMessage(tunnel);
        Gui.refreshTunnelRowColumns(tunnel, Gui.COLUMN_INDEX_STATUS);
        
        connectConfig.updatePersistenceDatabase(tunnel);
      
        replyMsg = new V6Message(requestMsg.getLocalAddress(), requestMsg.getRemoteAddress());
        replyMsg.setMessageType(Constants.V6_MESSAGE_TYPE_TUNNEL_PING_REPLY);
        replyMsg.setMessageSender(Constants.V6_MESSAGE_SENDER_SERVER);
        replyMsg.setTransactionId(requestMsg.getTransactionId());

        replyMsg.putOption(new TunnelIdOption(tunnel.getId().toString(), false));

        return true;
    }
}
