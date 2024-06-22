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
import com.wgconnect.core.option.machine.GenericIdOption;
import com.wgconnect.core.option.machine.InterfaceNameOption;
import com.wgconnect.core.option.machine.LocalTunnelInetAddrOption;
import com.wgconnect.core.option.machine.GenericResponseOption;
import com.wgconnect.core.option.machine.RemotePhysInetAddrOption;
import com.wgconnect.core.option.machine.RemotePhysInetComPortOption;
import com.wgconnect.core.option.machine.RemotePhysInetListenPortOption;
import com.wgconnect.core.option.machine.RemoteWgPublicKeyOption;
import com.wgconnect.core.option.machine.RemoteTunnelInetComPortOption;
import com.wgconnect.core.option.machine.RemoteTunnelInetAddrOption;
import com.wgconnect.core.option.machine.TunnelIdOption;
import com.wgconnect.core.option.machine.TunnelNetworkOption;
import com.wgconnect.core.util.Constants;
import com.wgconnect.core.util.WgConnectLogger;
import com.wgconnect.db.persistence.PersistenceTunnel;
import com.wgconnect.gui.Gui;
import com.wgconnect.machine.V4Machine;
import static com.wgconnect.machine.processor.BaseV4Processor.connectConfig;

import com.wgtools.Wg;

import inet.ipaddr.ipv4.IPv4Address;

import java.net.InetAddress;

import org.apache.commons.lang3.StringUtils;

/**
 * V4RequestProcessor
 * 
 * The main class for processing V4 request messages.
 * 
 * @author JagornetDhcp version: A. Gregory Rabil
 * @author WgConnect version: wgconnect@proton.me
 */
public class V4RequestProcessor extends BaseV4Processor {

    private static final WgConnectLogger log = WgConnectLogger.getLogger(V4RequestProcessor.class);

    protected V4Machine.ServerMachine v4ServerMachine;

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
     * Instantiate a V4RequestProcessor.
     * 
     * @param v4ServerMachine
     * @param requestMsg the request message
     * @param remoteInetAddress the remote inet address
     */
    public V4RequestProcessor(V4Machine.ServerMachine v4ServerMachine, V4Message requestMsg, InetAddress remoteInetAddress) {
        super(requestMsg, remoteInetAddress);
        
        this.v4ServerMachine = v4ServerMachine;
    }

    /* (non-Javadoc)
     * @see com.wgconnect.machine.processor.BaseV4Processor#preProcess()
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
     * @see com.wgconnect.machine.processor.BaseV4Processor#process()
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
            wg.setInterfaceConfigParameters(tunnel.getLocalInterfaceName(),
                Wg.OPTION_PEER, tunnel.getRemotePublicKey(),
                Wg.OPTION_ALLOWED_IPS, tunnel.getRemoteTunnelInetAddr() + IPv4Address.PREFIX_LEN_SEPARATOR + Constants.V4_SUBNET_MASK_32,
                Wg.OPTION_ENDPOINT, tunnel.getRemotePhysInetAddr() + ":" + tunnel.getRemotePhysInetListenPort(),
                Wg.OPTION_PERSISTENT_KEEPALIVE,
                Integer.toString(WgConnect.getPersistentKeepalive())
            );
            
            if (wg.getCommandExitCode() == Wg.getCommandSuccessCode()) {
                try {
                    tunnel.setState(Constants.V4_TUNNEL_STATUS_REQUEST);
                    Gui.refreshTunnelRowColumns(tunnel, Gui.COLUMN_INDEX_STATUS, Gui.COLUMN_INDEX_ENDPOINTS, Gui.COLUMN_INDEX_PUBLIC_KEYS);

                    connectConfig.updatePersistenceDatabase(tunnel);

                    replyMsg = new V4Message(requestMsg.getLocalAddress(), requestMsg.getRemoteAddress());
                    replyMsg.setOp((short) Constants.V4_OP_REPLY);
                    replyMsg.setHtype(requestMsg.getHtype());
                    replyMsg.setTransactionId(requestMsg.getTransactionId());
                    replyMsg.setClientAddr(requestMsg.getClientAddr());
                    replyMsg.setClientPort(requestMsg.getClientPort());
                    replyMsg.setServerAddr(requestMsg.getServerAddr());
                    replyMsg.setServerPort(requestMsg.getServerPort());
                    
                    replyMsg.setMessageType((short) Constants.V4_MESSAGE_TYPE_ACK);
                    replyMsg.setMessageSender((short) Constants.V4_MESSAGE_SENDER_SERVER);

                    replyMsg.putOption(new TunnelIdOption(tunnel.getId().toString(), true));
                    
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
                        WgConnect.removeTunnel(tunnel);

                        if (!StringUtils.equals(v4ServerMachine.getTunnelInetNet(), tunnelNetworkOption.getString())) {
                            v4ServerMachine.configureLocalTunnelAddr(tunnelNetworkOption.getString());
                            
                            v4ServerMachine.setRemoteTunnelInetAddr(StringUtils.substringBefore(v4ServerMachine.getV4Machine().applyTunnelNet(
                                v4ServerMachine.getTunnelInetNet(), tunnel.getRemotePhysInetAddr()).toInetAddress().getHostAddress(),
                                IPv4Address.PREFIX_LEN_SEPARATOR));
                        }
                        
                        tunnel = v4ServerMachine.getV4Machine().createTunnelAsServer(
                            v4ServerMachine,
                            Integer.parseInt(remoteIdOption.getString()),
                            Constants.TUNNEL_ENDPOINT_TYPE_SERVER,
                            Constants.TUNNEL_ENDPOINT_TYPE_CLIENT,
                            v4ServerMachine.getLocalPhysInetAddr(),
                            v4ServerMachine.getRemotePhysInetAddr(),
                            tunnel.getRemotePhysInetComPort(),
                            v4ServerMachine.getRemoteTunnelInetAddr(),
                            v4ServerMachine.getLocalTunnelInetAddr(),
                            v4ServerMachine.getTunnelInetNet(),
                            true);
                        
                        if (tunnel != null) {
                            tunnel.setState(Constants.V4_TUNNEL_STATUS_DISCOVER);
                            Gui.refreshTunnelRowColumns(tunnel, Gui.COLUMN_INDEX_STATUS);

                            v4ServerMachine.setPersistenceTunnel(tunnel);

                            replyMsg = new V4Message(requestMsg.getLocalAddress(), requestMsg.getRemoteAddress());
                            replyMsg.setOp((short) Constants.V4_OP_REPLY);
                            replyMsg.setHtype(requestMsg.getHtype());
                            replyMsg.setTransactionId(requestMsg.getTransactionId());
                            replyMsg.setClientAddr(requestMsg.getClientAddr());
                            replyMsg.setClientPort(requestMsg.getClientPort());
                            replyMsg.setServerAddr(v4ServerMachine.getLocalPhysInetSockAddr().getAddress());
                            replyMsg.setServerPort(v4ServerMachine.getLocalPhysInetSockAddr().getPort());

                            replyMsg.setMessageType((short) Constants.V4_MESSAGE_TYPE_OFFER);
                            replyMsg.setMessageSender((short) Constants.V4_MESSAGE_SENDER_SERVER);

                            replyMsg.putOption(remoteIdOption);
                            replyMsg.putOption(new RemoteWgPublicKeyOption(tunnel.getLocalPublicKey(), true));
                            replyMsg.putOption(new RemotePhysInetAddrOption(tunnel.getLocalPhysInetAddr(), true));
                            replyMsg.putOption(new RemotePhysInetComPortOption(tunnel.getLocalPhysInetComPort(), true));
                            replyMsg.putOption(new RemotePhysInetListenPortOption(tunnel.getLocalPhysInetListenPort(), true));
                            replyMsg.putOption(new RemoteTunnelInetAddrOption(tunnel.getLocalTunnelInetAddr(), true));
                            replyMsg.putOption(new RemoteTunnelInetComPortOption(tunnel.getLocalTunnelInetComPort(), true));
                            replyMsg.putOption(new LocalTunnelInetAddrOption(tunnel.getRemoteTunnelInetAddr(), true));
                            replyMsg.putOption(new InterfaceNameOption(tunnel.getLocalInterfaceName(), true));
                            replyMsg.putOption(new TunnelIdOption(tunnel.getId().toString(), true));
                            replyMsg.putOption(new TunnelNetworkOption(v4ServerMachine.getTunnelInetNet(), true));
                            
                            v4ServerMachine.setRemoteTunnelInetAddr(tunnel.getRemoteTunnelInetAddr());

                            connectConfig.updatePersistenceDatabase(tunnel);
                            
                            tunnel.setState(Constants.V4_TUNNEL_STATUS_OFFER);
                            Gui.refreshTunnelRowColumns(tunnel, Gui.COLUMN_INDEX_STATUS);
                        }
                    } catch (Exception ex) {
                    log.error("Unable to process V4 Discover: " + ex);
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
