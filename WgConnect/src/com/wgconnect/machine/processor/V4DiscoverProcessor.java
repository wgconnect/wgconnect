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
import com.wgconnect.machine.V4Machine;
import com.wgconnect.core.message.V4Message;
import com.wgconnect.core.option.machine.GenericIdOption;
import com.wgconnect.core.option.machine.GenericResponseOption;
import com.wgconnect.core.option.machine.SpecificInfoOption;
import com.wgconnect.core.option.machine.InterfaceNameOption;
import com.wgconnect.core.option.machine.LocalTunnelInetAddrOption;
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
import static com.wgconnect.machine.processor.BaseV4Processor.connectConfig;

import inet.ipaddr.IPAddressString;
import inet.ipaddr.ipv4.IPv4Address;

import java.net.InetAddress;
import java.net.InetSocketAddress;

import org.apache.commons.lang3.StringUtils;

/**
 * V4DiscoverProcessor
 * 
 * The main class for processing V4 discover messages.
 * 
 * @author JagornetDhcp version: A. Gregory Rabil
 * @author WgConnect version: wgconnect@proton.me
 */
public class V4DiscoverProcessor extends BaseV4Processor {
    
    private static final WgConnectLogger log = WgConnectLogger.getLogger(V4DiscoverProcessor.class);

    protected V4Machine.ServerMachine v4ServerMachine;
    
    protected GenericIdOption remoteIdOption;
    protected SpecificInfoOption remoteInfoOption;
    protected RemotePhysInetAddrOption remotePhysInetAddrOption;
    protected RemotePhysInetComPortOption remotePhysInetComPortOption;
    protected RemoteTunnelInetAddrOption remoteTunnelInetAddrOption;
    protected TunnelNetworkOption tunnelNetworkOption;
    
    /**
     * Instantiate a V4DiscoverProcessor.
     * 
     * @param v4ServerMachine the V4 server machine
     * @param discoverMsg the discover message
     * @param remoteInetAddr the remote inet address
     */
    public V4DiscoverProcessor(V4Machine.ServerMachine v4ServerMachine, V4Message discoverMsg, InetAddress remoteInetAddr) {
        super(discoverMsg, remoteInetAddr);
        
        this.v4ServerMachine = v4ServerMachine;
    }

    /* (non-Javadoc)
     * @see com.wgconnect.machine.processor.BaseV4Processor#preProcess()
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
                remotePhysInetComPortOption == null || remoteTunnelInetAddrOption == null ||
                tunnelNetworkOption == null) {
                log.warn("Ignoring Discover message: A configuration option was not sent");
                process = false;
            }
        }
        
        return process;
    }

    /* (non-Javadoc)
     * @see com.wgconnect.machine.processor.BaseV4Processor#process()
     */
    @Override
    public boolean process() {
        boolean sendReply = false;

        // Validate the client's network options
        String tunnelId = StringUtils.SPACE;
        String remoteWgPublicKey = StringUtils.SPACE;
        String remotePhysInetAddr = StringUtils.SPACE;
        long remotePhysInetComPort = 0;
        long remotePhysInetListenPort = 0;
        String remoteTunnelInetAddr = StringUtils.SPACE;
        long remoteTunnelInetComPort = 0;
        String localTunnelInetAddr = StringUtils.SPACE;
        String interfaceName = StringUtils.SPACE;
        String tunnelNetwork = StringUtils.SPACE;
        int discoverResponse = Constants.RESPONSE_ACCEPT;

        try {
            IPv4Address ipLocalTunnelInetNet = new IPAddressString(v4ServerMachine.getTunnelInetNet()).getAddress().toIPv4();
            IPv4Address ipRemoteTunnelInetNet = new IPAddressString(tunnelNetworkOption.getString()).getAddress().toIPv4();
            IPv4Address ipRemoteTunnelInetAddr = new IPAddressString(remoteTunnelInetAddrOption.getIpAddress()).getAddress().toIPv4();
            
            // Check for an existing connection to the specified remotePhysInetAddr and remoteTunnelInetAddr
            if (WgConnect.getTunnelByRemotePhysAndRemoteTunnelInetAddrs(remotePhysInetAddrOption.getIpAddress(),
                ipRemoteTunnelInetAddr.toCanonicalString()) != null) {
                discoverResponse = Constants.RESPONSE_DECLINE_REDUNDANT_TUNNEL;
            }
            // Check for an existing connection to the remotePublicKey
            else if (WgConnect.getTunnelByRemotePublicKey(remotePhysInetAddrOption.getIpAddress()) != null) {
                discoverResponse = Constants.RESPONSE_DECLINE_REDUNDANT_PUBLIC_KEY;
            }
            // Check if the remoteTunnelInetNet differs from the current server tunnelInetNet
            else if (ipRemoteTunnelInetNet.compareTo(ipLocalTunnelInetNet) != 0) {
                log.info("The remote tunnel network {} from physical address {} is differs from the local tunnel network {}",
                    tunnelNetworkOption.getString(), remotePhysInetAddrOption.getIpAddress(), v4ServerMachine.getTunnelInetNet());

                v4ServerMachine.configureLocalTunnelAddr(tunnelNetworkOption.getString());
                v4ServerMachine.setTunnelInetNet(tunnelNetworkOption.getString());
            }
            
            // The Discover options are valid
            if (discoverResponse == Constants.RESPONSE_ACCEPT) {
                
                PersistenceTunnel tunnel = v4ServerMachine.getV4Machine().createTunnelAsServer(
                    v4ServerMachine,
                    Integer.parseInt(remoteIdOption.getString()),
                    Constants.TUNNEL_ENDPOINT_TYPE_SERVER,
                    Constants.TUNNEL_ENDPOINT_TYPE_CLIENT,
                    v4ServerMachine.getLocalPhysInetAddr(),
                    remotePhysInetAddrOption.getIpAddress(),
                    remotePhysInetComPortOption.getUnsignedInt(),
                    StringUtils.substringBefore(ipRemoteTunnelInetAddr.toInetAddress().getHostAddress(), IPv4Address.PREFIX_LEN_SEPARATOR),
                    v4ServerMachine.getLocalTunnelInetAddr(),
                    v4ServerMachine.getTunnelInetNet(),
                    false);

                if (tunnel != null && tunnel.getLocalTunnelInetAddr() != null && tunnel.getRemoteTunnelInetAddr() != null) {

                    v4ServerMachine.setPersistenceTunnel(tunnel);

                    replyMsg = new V4Message(requestMsg.getLocalAddress(),
                        new InetSocketAddress(InetAddress.getByName(remotePhysInetAddrOption.getIpAddress()),
                            (int) remotePhysInetComPortOption.getUnsignedInt()));
                    replyMsg.setOp((short) Constants.V4_OP_REPLY);
                    replyMsg.setHtype(requestMsg.getHtype());
                    replyMsg.setTransactionId(requestMsg.getTransactionId());
                    replyMsg.setClientAddr(requestMsg.getClientAddr());
                    replyMsg.setClientPort(requestMsg.getClientPort());
                    replyMsg.setServerAddr(v4ServerMachine.getLocalPhysInetSockAddr().getAddress());
                    replyMsg.setServerPort(v4ServerMachine.getLocalPhysInetSockAddr().getPort());

                    replyMsg.setMessageType((short) Constants.V4_MESSAGE_TYPE_OFFER);
                    replyMsg.setMessageSender((short) Constants.V4_MESSAGE_SENDER_SERVER);

                    tunnelId = tunnel.getId().toString();
                    remoteWgPublicKey = tunnel.getLocalPublicKey();
                    remotePhysInetAddr = tunnel.getLocalPhysInetAddr();
                    remotePhysInetComPort = tunnel.getLocalPhysInetComPort();
                    remotePhysInetListenPort = tunnel.getLocalPhysInetListenPort();
                    remoteTunnelInetAddr = tunnel.getLocalTunnelInetAddr();
                    remoteTunnelInetComPort = tunnel.getLocalTunnelInetComPort();
                    localTunnelInetAddr = tunnel.getRemoteTunnelInetAddr();
                    interfaceName = tunnel.getLocalInterfaceName();
                    tunnelNetwork = v4ServerMachine.getTunnelInetNet();

                    v4ServerMachine.setRemoteTunnelInetAddr(tunnel.getRemoteTunnelInetAddr());

                    connectConfig.updatePersistenceDatabase(tunnel);

                    tunnel.setState(Constants.V4_TUNNEL_STATUS_OFFER);
                    Gui.refreshTunnelRowColumns(tunnel, Gui.COLUMN_INDEX_STATUS);
                }
            }
            
            replyMsg.putOption(remoteIdOption);
            replyMsg.putOption(new TunnelIdOption(tunnelId, true));
            replyMsg.putOption(new RemoteWgPublicKeyOption(remoteWgPublicKey, true));
            replyMsg.putOption(new RemotePhysInetAddrOption(remotePhysInetAddr, true));
            replyMsg.putOption(new RemotePhysInetComPortOption(remotePhysInetComPort, true));
            replyMsg.putOption(new RemotePhysInetListenPortOption(remotePhysInetListenPort, true));
            replyMsg.putOption(new RemoteTunnelInetAddrOption(remoteTunnelInetAddr, true));
            replyMsg.putOption(new RemoteTunnelInetComPortOption(remoteTunnelInetComPort, true));
            replyMsg.putOption(new LocalTunnelInetAddrOption(localTunnelInetAddr, true));
            replyMsg.putOption(new InterfaceNameOption(interfaceName, true));
            replyMsg.putOption(new TunnelNetworkOption(tunnelNetwork, true));
            replyMsg.putOption(new GenericResponseOption(discoverResponse, true));

            sendReply = true;
        } catch (Exception ex) {
            log.error("Unable to process V4 Discover: " + ex);
        }

        return sendReply;
    }
}
