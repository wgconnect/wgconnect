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
import com.wgconnect.core.message.V4Message;
import com.wgconnect.core.option.machine.PingInetAddrOption;
import com.wgconnect.core.option.machine.PingInetPortOption;
import com.wgconnect.core.option.machine.TunnelIdOption;
import com.wgconnect.core.util.Constants;
import com.wgconnect.core.util.WgConnectLogger;
import com.wgconnect.db.persistence.PersistenceTunnel;
import com.wgconnect.gui.Gui;
import com.wgconnect.machine.V4Machine;

import java.net.InetAddress;
import java.net.InetSocketAddress;

/**
 * V4PingProcessor
 * 
 * The main class for processing V4 ping messages.
 * 
 * @author: wgconnect@proton.me
 */
public class V4PingProcessor extends BaseV4Processor {

    private static final WgConnectLogger log = WgConnectLogger.getLogger(V4PingProcessor.class);

    protected final V4Machine.ServerMachine v4ServerMachine;

    private PersistenceTunnel tunnel;
    
    private TunnelIdOption tunnelIdOption;
    protected PingInetAddrOption remotePingInetAddrOption;
    protected PingInetPortOption remotePingInetPortOption;
        
    /**
     * Instantiate a V4PingProcessor.
     * 
     * @param v4ServerMachine
     * @param pingMsg the ping message
     * @param remoteInetAddr the remote inet address
     */
    public V4PingProcessor(V4Machine.ServerMachine v4ServerMachine, V4Message pingMsg, InetAddress remoteInetAddr) {
        super(pingMsg, remoteInetAddr);        

        this.v4ServerMachine = v4ServerMachine;
    }
    
    /* (non-Javadoc)
     * @see com.wgconnect.machine.processor.BaseV4Processor#preProcess()
     */
    @Override
    public boolean preProcess() {
        boolean process = true;

        tunnelIdOption = (TunnelIdOption) requestMsg.getOption(Constants.OPTION_TUNNEL_ID);
        remotePingInetAddrOption = (PingInetAddrOption) requestMsg.getOption(Constants.OPTION_PING_INET_ADDR);
        remotePingInetPortOption = (PingInetPortOption) requestMsg.getOption(Constants.OPTION_PING_INET_PORT);
            
        if (tunnelIdOption != null || remotePingInetAddrOption == null || remotePingInetPortOption == null) {
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
     * @see com.wgconnect.machine.processor.BaseV4Processor#process()
     */
    @Override
    public boolean process() {

        tunnel.setState(Constants.TUNNEL_STATUS_UP);
        WgConnect.printTunnelCompleteMessage(tunnel);
        Gui.refreshTunnelRowColumns(tunnel, Gui.COLUMN_INDEX_STATUS);
        
        connectConfig.updatePersistenceDatabase(tunnel);
       
        replyMsg = new V4Message(requestMsg.getLocalAddress(),
            new InetSocketAddress(remotePingInetAddrOption.getIpAddress(), (int) remotePingInetPortOption.getUnsignedInt()));
        replyMsg.setOp((short) Constants.V4_OP_REPLY);
        replyMsg.setHtype(requestMsg.getHtype());
        replyMsg.setTransactionId(requestMsg.getTransactionId());
        replyMsg.setClientAddr(requestMsg.getClientAddr());
        replyMsg.setClientPort(requestMsg.getClientPort());
        replyMsg.setServerAddr(requestMsg.getServerAddr());
        replyMsg.setServerPort(requestMsg.getServerPort());
        
        replyMsg.setMessageType((short) Constants.V4_MESSAGE_TYPE_TUNNEL_PING_REPLY);
        replyMsg.setMessageSender((short) Constants.V4_MESSAGE_SENDER_SERVER);

        replyMsg.putOption(new TunnelIdOption(tunnel.getId().toString(), true));

        return true;
    }
}
