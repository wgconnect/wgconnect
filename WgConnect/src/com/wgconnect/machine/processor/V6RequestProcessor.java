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
import java.net.InetAddress;

import com.wgconnect.core.message.V6Message;
import com.wgconnect.core.option.machine.GenericIdOption;
import com.wgconnect.core.option.machine.InterfaceNameOption;
import com.wgconnect.core.option.machine.LocalTunnelInetAddrOption;
import com.wgconnect.core.option.machine.GenericResponseOption;
import com.wgconnect.core.option.machine.RemotePhysInetAddrOption;
import com.wgconnect.core.option.machine.RemotePhysInetComPortOption;
import com.wgconnect.core.option.machine.RemotePhysInetListenPortOption;
import com.wgconnect.core.option.machine.RemoteTunnelInetAddrOption;
import com.wgconnect.core.option.machine.RemoteWgPublicKeyOption;
import com.wgconnect.core.option.machine.RemoteTunnelInetComPortOption;
import com.wgconnect.core.option.machine.TunnelIdOption;
import com.wgconnect.core.option.machine.TunnelNetworkOption;
import com.wgconnect.core.util.Constants;
import com.wgconnect.core.util.WgConnectLogger;
import com.wgconnect.db.persistence.PersistenceTunnel;
import com.wgconnect.gui.Gui;
import com.wgconnect.machine.V6Machine;

import com.wgtools.Set;
import com.wgtools.Wg;

import inet.ipaddr.ipv6.IPv6Address;

import org.apache.commons.lang3.StringUtils;

/**
 * V6RequestProcessor
 * 
 * The main class for processing V6 request messages.
 * 
 * @author JagornetDhcp version: A. Gregory Rabil
 * @author WgConnect version: wgconnect@proton.me
 */
public class V6RequestProcessor extends BaseV6Processor {

    private static final WgConnectLogger log = WgConnectLogger.getLogger(V6RequestProcessor.class);

    protected V6Machine.ServerMachine v6ServerMachine;
    
    protected PersistenceTunnel tunnel;
    
    protected GenericIdOption remoteIdOption;
    protected TunnelIdOption tunnelIdOption;
    protected GenericResponseOption offerResponseOption;
    protected RemotePhysInetListenPortOption remoteListenPortOption;
    protected RemoteTunnelInetComPortOption remoteTunnelComPortOption;
    protected RemoteWgPublicKeyOption remoteWgPublicKeyOption;
    protected InterfaceNameOption remoteInterfaceNameOption;
    protected TunnelNetworkOption tunnelNetworkOption;

    /**
     * Instantiate a V6RequestProcessor
     * 
     * @param v6ServerMachine
     * @param requestMsg the request message
     * @param remoteInetAddress the remote inet address
     */
    public V6RequestProcessor(V6Machine.ServerMachine v6ServerMachine, V6Message requestMsg, InetAddress remoteInetAddress) {
        super(requestMsg, remoteInetAddress);
        
        this.v6ServerMachine = v6ServerMachine;
    }

    /* (non-Javadoc)
     * @see com.wgconnect.machine.processor.BaseV6Processor#preProcess()
     */
    @Override
    public boolean preProcess() {
        boolean process = true;

        remoteIdOption = (GenericIdOption) requestMsg.getOption(Constants.OPTION_GENERIC_ID);
        tunnelIdOption = (TunnelIdOption) requestMsg.getOption(Constants.OPTION_TUNNEL_ID);
        offerResponseOption = (GenericResponseOption) requestMsg.getOption(Constants.OPTION_GENERIC_RESPONSE);
        remoteListenPortOption = (RemotePhysInetListenPortOption) requestMsg.getOption(Constants.OPTION_REMOTE_PHYS_INET_LISTEN_PORT);
        remoteTunnelComPortOption = (RemoteTunnelInetComPortOption) requestMsg.getOption(Constants.OPTION_REMOTE_TUNNEL_INET_COM_PORT);
        remoteWgPublicKeyOption = (RemoteWgPublicKeyOption) requestMsg.getOption(Constants.OPTION_REMOTE_WG_PUBLIC_KEY);
        remoteInterfaceNameOption = (InterfaceNameOption) requestMsg.getOption(Constants.OPTION_INTERFACE_NAME);
        tunnelNetworkOption = (TunnelNetworkOption) requestMsg.getOption(Constants.OPTION_TUNNEL_NETWORK);
        
        if (remoteIdOption != null && tunnelIdOption != null && offerResponseOption != null && remoteListenPortOption != null &&
            remoteTunnelComPortOption != null && remoteWgPublicKeyOption != null && remoteInterfaceNameOption != null &&
            tunnelNetworkOption != null) {
            tunnel = WgConnect.getTunnelByTunnelId(tunnelIdOption.getString());
            
            if (tunnel == null) {
                log.warn("Ignoring Request message: No tunnel was found for the tunnel id {}", tunnelIdOption.getString());
                process = false;
            }
        } else {
            log.warn("Ignoring Request message: A required option was not sent");
            process = false;
        }
        
        return process;
    }

    /* (non-Javadoc)
     * @see com.wgconnect.machine.processor.BaseV6Processor#process()
     */
    @Override
    public boolean process() {
        boolean sendReply = false;
        
        if (offerResponseOption.getUnsignedInt() == Constants.RESPONSE_ACCEPT) {
            tunnel.setRemotePhysInetListenPort(remoteListenPortOption.getUnsignedInt());
            tunnel.setRemoteTunnelInetComPort(remoteTunnelComPortOption.getUnsignedInt());
            tunnel.setRemotePublicKey(remoteWgPublicKeyOption.getString());
            tunnel.setRemoteInterfaceName(remoteInterfaceNameOption.getString());

            Wg wg = new Wg();
            wg.executeSubcommand(Set.COMMAND, tunnel.getLocalInterfaceName(),
                Wg.OPTION_PEER, tunnel.getRemotePublicKey(),
                Wg.OPTION_ALLOWED_IPS, tunnel.getRemoteTunnelInetAddr() + "/" + Constants.V6_SUBNET_MASK_128,
                Wg.OPTION_ENDPOINT, tunnel.getRemotePhysInetAddr() + ":" + tunnel.getRemotePhysInetListenPort(),
                WgConnect.getPersistentKeepalive() > 0 ? Wg.OPTION_PERSISTENT_KEEPALIVE : "",
                WgConnect.getPersistentKeepalive() > 0 ? Integer.toString(WgConnect.getPersistentKeepalive()) : ""
            );

            if (wg.getCommandExitCode() == Wg.getCommandSuccessCode()) {
                try {
                    tunnel.setState(Constants.V6_TUNNEL_STATUS_REQUEST);
                    Gui.refreshTunnelRowColumns(tunnel, Gui.COLUMN_INDEX_STATUS, Gui.COLUMN_INDEX_ENDPOINTS, Gui.COLUMN_INDEX_PUBLIC_KEYS);

                    connectConfig.updatePersistenceDatabase(tunnel);

                    replyMsg = new V6Message(requestMsg.getLocalAddress(), requestMsg.getRemoteAddress());
                    replyMsg.setMessageType(Constants.V6_MESSAGE_TYPE_REPLY);
                    replyMsg.setMessageSender(Constants.V6_MESSAGE_SENDER_SERVER);
                    
                    replyMsg.setTransactionId(requestMsg.getTransactionId());
                    
                    replyMsg.putOption(new TunnelIdOption(tunnel.getId().toString(), false));
                    
                    sendReply = true;
                } catch (Exception ex) {
                    log.error("Exception: " + ex);
                }
                
                // Add the tunnel to the GUI
                Gui.addTunnel(tunnel);
            }
        } else {
            switch ((int) offerResponseOption.getUnsignedInt()) {
                case Constants.RESPONSE_DECLINE_REDUNDANT_PUBLIC_KEY:
                    try {
                        WgConnect.getAllTunnels().remove(tunnel);

                        if (!StringUtils.equals(v6ServerMachine.getTunnelInetNet(), tunnelNetworkOption.getString())) {
                            v6ServerMachine.configureLocalTunnelAddr(tunnelNetworkOption.getString());
                            
                            v6ServerMachine.setRemoteTunnelInetAddr(StringUtils.substringBefore(v6ServerMachine.getV6Machine().applyTunnelNet(
                                v6ServerMachine.getTunnelInetNet(), tunnel.getRemotePhysInetAddr()).toInetAddress().getHostAddress(),
                                IPv6Address.PREFIX_LEN_SEPARATOR));
                        }
                        
                        tunnel = v6ServerMachine.getV6Machine().createTunnelAsServer(
                            v6ServerMachine,
                            Integer.parseInt(remoteIdOption.getString()),
                            Constants.TUNNEL_ENDPOINT_TYPE_SERVER,
                            Constants.TUNNEL_ENDPOINT_TYPE_CLIENT,
                            v6ServerMachine.getLocalPhysInetAddr(),
                            v6ServerMachine.getRemotePhysInetAddr(),
                            tunnel.getRemotePhysInetComPort(),
                            v6ServerMachine.getRemoteTunnelInetAddr(),
                            v6ServerMachine.getLocalTunnelInetAddr(),
                            v6ServerMachine.getTunnelInetNet(),
                            true);
                        
                        if (tunnel != null) {
                            tunnel.setState(Constants.V6_TUNNEL_STATUS_SOLICIT);
                            Gui.refreshTunnelRowColumns(tunnel, Gui.COLUMN_INDEX_STATUS);

                            v6ServerMachine.setPersistenceTunnel(tunnel);

                            replyMsg = new V6Message(requestMsg.getLocalAddress(), requestMsg.getRemoteAddress());
                            replyMsg.setTransactionId(requestMsg.getTransactionId());

                            replyMsg.setMessageType((short) Constants.V6_MESSAGE_TYPE_REQUEST);
                            replyMsg.setMessageSender((short) Constants.V6_MESSAGE_SENDER_SERVER);

                            replyMsg.putOption(remoteIdOption);
                            replyMsg.putOption(new RemoteWgPublicKeyOption(tunnel.getLocalPublicKey(), false));
                            replyMsg.putOption(new RemotePhysInetAddrOption(tunnel.getLocalPhysInetAddr(), false));
                            replyMsg.putOption(new RemotePhysInetComPortOption(tunnel.getLocalPhysInetComPort(), false));
                            replyMsg.putOption(new RemotePhysInetListenPortOption(tunnel.getLocalPhysInetListenPort(), false));
                            replyMsg.putOption(new RemoteTunnelInetAddrOption(tunnel.getLocalTunnelInetAddr(), false));
                            replyMsg.putOption(new RemoteTunnelInetComPortOption(tunnel.getLocalTunnelInetComPort(), false));
                            replyMsg.putOption(new LocalTunnelInetAddrOption(tunnel.getRemoteTunnelInetAddr(), false));
                            replyMsg.putOption(new InterfaceNameOption(tunnel.getLocalInterfaceName(), false));
                            replyMsg.putOption(new TunnelIdOption(tunnel.getId().toString(), false));
                            replyMsg.putOption(new TunnelNetworkOption(v6ServerMachine.getTunnelInetNet(), false));
                            
                            v6ServerMachine.setRemoteTunnelInetAddr(tunnel.getRemoteTunnelInetAddr());

                            connectConfig.updatePersistenceDatabase(tunnel);
                            
                            tunnel.setState(Constants.V6_TUNNEL_STATUS_REQUEST);
                            Gui.refreshTunnelRowColumns(tunnel, Gui.COLUMN_INDEX_STATUS);
                        }
                    } catch (Exception ex) {
                    log.error("Unable to process V6 Discover: " + ex);
                    }

                    break;
                    
                case Constants.RESPONSE_DECLINE_TUNNEL_NETWORK:
                    break;
                    
                default:
                    log.info("Received unhandled offer response type: " + offerResponseOption.getUnsignedInt());
                    break;
            }
        }
        
        return sendReply;
    }
}
