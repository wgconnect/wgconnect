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
import com.wgconnect.machine.V6Machine;
import com.wgconnect.core.message.V6Message;
import com.wgconnect.core.option.machine.InterfaceNameOption;
import com.wgconnect.core.option.machine.LocalTunnelInetAddrOption;
import com.wgconnect.core.option.machine.RemotePhysInetAddrOption;
import com.wgconnect.core.option.machine.RemoteTunnelInetComPortOption;
import com.wgconnect.core.option.machine.TunnelIdOption;
import com.wgconnect.core.option.machine.GenericIdOption;
import com.wgconnect.core.option.machine.GenericResponseOption;
import com.wgconnect.core.option.machine.SpecificInfoOption;
import com.wgconnect.core.option.machine.RemotePhysInetComPortOption;
import com.wgconnect.core.option.machine.RemotePhysInetListenPortOption;
import com.wgconnect.core.option.machine.RemoteTunnelInetAddrOption;
import com.wgconnect.core.option.machine.RemoteWgPublicKeyOption;
import com.wgconnect.core.option.machine.TunnelNetworkOption;
import com.wgconnect.core.util.Constants;
import com.wgconnect.core.util.WgConnectLogger;
import com.wgconnect.db.persistence.PersistenceTunnel;
import com.wgconnect.gui.Gui;

import inet.ipaddr.IPAddressString;
import inet.ipaddr.ipv6.IPv6Address;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

/**
 * V6SolicitProcessor
 * 
 * The main class for processing V6 solicit messages.
 * 
 * @author JagornetDhcp version: A. Gregory Rabil
 * @author WgConnect version: wgconnect@proton.me
 */
public class V6SolicitProcessor extends BaseV6Processor {

    private static final WgConnectLogger log = WgConnectLogger.getLogger(V6SolicitProcessor.class);

    protected V6Machine.ServerMachine v6ServerMachine;
    
    protected GenericIdOption remoteIdOption;
    protected SpecificInfoOption remoteInfoOption;
    protected RemotePhysInetAddrOption remotePhysInetAddrOption;
    protected RemotePhysInetComPortOption remotePhysInetComPortOption;
    protected RemoteTunnelInetAddrOption remoteTunnelInetAddrOption;
    protected TunnelNetworkOption tunnelNetworkOption;
    
    /**
     * Instantiate a V6SolicitProcessor
     *
     * @param v6ServerMachine the V6 server machine
     * @param solicitMsg the solicit message
     * @param remoteInetAddr the remote inet address
     */
    public V6SolicitProcessor(V6Machine.ServerMachine v6ServerMachine, V6Message solicitMsg, InetAddress remoteInetAddr) {
        super(solicitMsg, remoteInetAddr);
        
        this.v6ServerMachine = v6ServerMachine;
    }

    /* (non-Javadoc)
     * @see com.wgconnect.machine.processor.BaseV6Processor#preProcess()
     */
    @Override
    public boolean preProcess() {
        boolean process = super.preProcess();

        if (process) {
            remoteIdOption = (GenericIdOption) requestMsg.getOption(Constants.OPTION_GENERIC_ID);
            remoteInfoOption = (SpecificInfoOption) requestMsg.getOption(Constants.OPTION_SPECIFIC_INFO);
            remotePhysInetAddrOption = (RemotePhysInetAddrOption) requestMsg.getOption(Constants.OPTION_REMOTE_PHYS_INET_ADDR);
            remotePhysInetComPortOption = (RemotePhysInetComPortOption) requestMsg.getOption(Constants.OPTION_REMOTE_PHYS_INET_COM_PORT);
            remoteTunnelInetAddrOption = (RemoteTunnelInetAddrOption) requestMsg.getOption(Constants.OPTION_REMOTE_TUNNEL_INET_ADDR);
            tunnelNetworkOption = (TunnelNetworkOption) requestMsg.getOption(Constants.OPTION_TUNNEL_NETWORK);

            if (remoteIdOption == null || remoteInfoOption == null || remotePhysInetAddrOption == null ||
                remotePhysInetComPortOption == null || remoteTunnelInetAddrOption == null || tunnelNetworkOption == null) {
                log.warn("Ignoring Solicit message: A configuration option was not sent");
                process = false;
            }
        }
        
        return process;
    }

    /* (non-Javadoc)
     * @see com.wgconnect.machine.processor.BaseV6Processor#process()
     */
    @Override
    public boolean process() {
        boolean sendReply = false;
        int solicitResponse = Constants.RESPONSE_ACCEPT;
        
        try {
            // Check the compatibility of the remoteTunnelNetwork and the localTunnelNetwork
            IPv6Address localTunnelInetNet = new IPAddressString(v6ServerMachine.getTunnelInetNet()).getAddress().toIPv6();
            String localTunnelInetNetPrefix = StringUtils.substringAfterLast(v6ServerMachine.getTunnelInetNet(), IPv6Address.PREFIX_LEN_SEPARATOR);
            IPv6Address remoteTunnelInetNet = new IPAddressString(tunnelNetworkOption.getString()).getAddress().toIPv6();
            String remoteTunnelInetNetPrefix = StringUtils.substringAfterLast(tunnelNetworkOption.getString(), IPv6Address.PREFIX_LEN_SEPARATOR);
            IPv6Address remoteTunnelInetAddr = new IPAddressString(remoteTunnelInetAddrOption.getIpAddress()).getAddress().toIPv6();
            
            // Check for an existing connection to the remotePhysInetAddr
            List<PersistenceTunnel> currentTunnels = WgConnect.getTunnelsByRemotePhysInetAddr(remotePhysInetAddrOption.getIpAddress());
            if (!currentTunnels.isEmpty()) {
                for (PersistenceTunnel t : currentTunnels) {
                    IPv6Address tInetNet = new IPAddressString(t.getTunnelInetNet()).getAddress().toIPv6();
                    if (tInetNet.compareTo(remoteTunnelInetNet) > 0) {
                        localTunnelInetNet = tInetNet;
                    }
                    
                    solicitResponse = Constants.RESPONSE_DECLINE_REDUNDANT_PUBLIC_KEY;
                }
               
                localTunnelInetNetPrefix = Integer.toString(Math.min(Integer.parseInt(localTunnelInetNetPrefix), Integer.parseInt(remoteTunnelInetNetPrefix)));
                v6ServerMachine.setTunnelInetNet(localTunnelInetNet.toInetAddress().getHostAddress() + IPv6Address.PREFIX_LEN_SEPARATOR +
                    localTunnelInetNetPrefix);
                v6ServerMachine.generateNextTunnelNet();
                
                remoteTunnelInetAddr = v6ServerMachine.getV6Machine().applyTunnelNet(v6ServerMachine.getTunnelInetNet(),
                    remotePhysInetAddrOption.getIpAddress());
            } else if (remoteTunnelInetNet.compareTo(localTunnelInetNet) != 0) {
                log.info("The remote tunnel network {} from physical address {} is on a different from the local tunnel network {}", 
                    tunnelNetworkOption.getString(), remotePhysInetAddrOption.getIpAddress(), v6ServerMachine.getTunnelInetNet());

                if (v6ServerMachine.getReferenceTunnel() == null) {
                    v6ServerMachine.configureLocalTunnelAddr(tunnelNetworkOption.getString());
                    v6ServerMachine.setTunnelInetNet(tunnelNetworkOption.getString());
                } else {
                    remoteTunnelInetAddr = v6ServerMachine.getV6Machine().applyTunnelNet(v6ServerMachine.getTunnelInetNet(),
                        remotePhysInetAddrOption.getIpAddress());
                }
            }
            
            PersistenceTunnel tunnel = v6ServerMachine.getV6Machine().createTunnelAsServer(
                v6ServerMachine,
                Integer.parseInt(remoteIdOption.getString()),
                Constants.TUNNEL_ENDPOINT_TYPE_SERVER,
                Constants.TUNNEL_ENDPOINT_TYPE_CLIENT,
                v6ServerMachine.getLocalPhysInetAddr(),
                remotePhysInetAddrOption.getIpAddress(),
                remotePhysInetComPortOption.getUnsignedInt(),
                StringUtils.substringBefore(remoteTunnelInetAddr.toInetAddress().getHostAddress(), IPv6Address.PREFIX_LEN_SEPARATOR),
                v6ServerMachine.getLocalTunnelInetAddr(),
                v6ServerMachine.getTunnelInetNet(),
                false);

            if (tunnel != null && tunnel.getLocalTunnelInetAddr() != null && tunnel.getRemoteTunnelInetAddr() != null) {
                
                v6ServerMachine.setPersistenceTunnel(tunnel);
                
                replyMsg = new V6Message(requestMsg.getLocalAddress(),
                    new InetSocketAddress(InetAddress.getByName(remotePhysInetAddrOption.getIpAddress()),
                    (int) remotePhysInetComPortOption.getUnsignedInt()));
                replyMsg.setMessageType(Constants.V6_MESSAGE_TYPE_ADVERTISE);
                replyMsg.setMessageSender(Constants.V6_MESSAGE_SENDER_SERVER);
                replyMsg.setTransactionId(requestMsg.getTransactionId());

                replyMsg.putOption(remoteIdOption);
                replyMsg.putOption(new TunnelIdOption(tunnel.getId().toString(), false));
                replyMsg.putOption(new RemoteWgPublicKeyOption(tunnel.getLocalPublicKey(), false));
                replyMsg.putOption(new RemotePhysInetAddrOption(tunnel.getLocalPhysInetAddr(), false));
                replyMsg.putOption(new RemotePhysInetComPortOption(tunnel.getLocalPhysInetComPort(), false));
                replyMsg.putOption(new RemotePhysInetListenPortOption(tunnel.getLocalPhysInetListenPort(), false));
                replyMsg.putOption(new RemoteTunnelInetAddrOption(tunnel.getLocalTunnelInetAddr(), false));
                replyMsg.putOption(new RemoteTunnelInetComPortOption(tunnel.getLocalTunnelInetComPort(), false));
                replyMsg.putOption(new LocalTunnelInetAddrOption(tunnel.getRemoteTunnelInetAddr(), false));
                replyMsg.putOption(new InterfaceNameOption(tunnel.getLocalInterfaceName(), false));
                replyMsg.putOption(new TunnelNetworkOption(v6ServerMachine.getTunnelInetNet(), false));
                replyMsg.putOption(new GenericResponseOption(solicitResponse, false));

                tunnel.setState(Constants.V6_TUNNEL_STATUS_SOLICIT);
                Gui.refreshTunnelRowColumns(tunnel, Gui.COLUMN_INDEX_STATUS);
                
                v6ServerMachine.setPersistenceTunnel(tunnel);
                
                connectConfig.updatePersistenceDatabase(tunnel);
                
                tunnel.setState(Constants.V6_TUNNEL_STATUS_SOLICIT);
                Gui.refreshTunnelRowColumns(tunnel, Gui.COLUMN_INDEX_STATUS);

                sendReply = true;
            }
        } catch (Exception ex) {
            log.error("Unable to process V6 Solicit: " + ex);
        }

        return sendReply;
    }
}
