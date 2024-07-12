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

import com.wgconnect.WgConnect;
import com.wgconnect.config.ConfigException;
import com.wgconnect.config.ConnectConfig;
import com.wgconnect.machine.processor.V6RequestProcessor;
import com.wgconnect.machine.processor.V6SolicitProcessor;
import com.wgconnect.core.message.V6Message;
import com.wgconnect.core.option.machine.*;
import com.wgconnect.core.util.Constants;
import com.wgconnect.core.util.Utils;
import com.wgconnect.core.util.WgConnectLogger;
import com.wgconnect.db.persistence.PersistenceTunnel;
import com.wgconnect.gui.Gui;
import com.wgconnect.machine.processor.V6PingProcessor;

import com.wgtools.InterfaceDeviceManager;
import com.wgtools.Wg;

import inet.ipaddr.AddressStringException;
import inet.ipaddr.IPAddress.IPVersion;
import inet.ipaddr.IPAddressString;
import inet.ipaddr.ipv6.IPv6Address;
import inet.ipaddr.ipv6.IPv6AddressSegment;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.security.KeyPair;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang3.StringUtils;

import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.channel.socket.DatagramChannel;
import org.jboss.netty.channel.socket.DatagramChannelFactory;
import org.jboss.netty.channel.socket.InternetProtocolFamily;
import org.jboss.netty.channel.socket.nio.NioDatagramChannelFactory;
import org.jboss.netty.handler.execution.ExecutionHandler;
import org.jboss.netty.handler.execution.OrderedMemoryAwareThreadPoolExecutor;
import org.jboss.netty.handler.logging.LoggingHandler;

/**
 * V6Machine
 * 
 * A V6 machine that sends and receives configuration messages to and from other V6 machines.
 * 
 * @author JagornetDhcp version: A. Gregory Rabil
 * @author WgConnect version: wgconnect@proton.me
 */
public class V6Machine implements Runnable {

    private static final WgConnectLogger log = WgConnectLogger.getLogger(V6Machine.class);

    private static final String NAME = "V6Machine";
    
    protected static ConnectConfig connectConfig = ConnectConfig.getInstance();
        
    protected final static int WAIT_FOR_REPLY_TIME = 300;
    protected final static int WAIT_FOR_COMPLETION_TIME = 60;
    
    protected KeyPair genericKeyPair = Utils.generateCryptoKeyPair(
        Constants.GENERIC_CRYPTO_ALGORITHM, Constants.GENERIC_CRYPTO_KEYSIZE);
    
    protected Random random = new Random();

    protected MachineDispatcher machineDispatcher = null;
    protected ServerMachine serverMachine = null;
    protected ClientMachine clientMachine = null;
    
    protected static DatagramChannelFactory factory = null;
    
    protected List<InetAddress> localPhysInetAddrs = null;
    protected int localPort = Constants.V6_PORT;

    protected Map<String, DatagramChannel> channels = new LinkedHashMap<>();
    
    protected List<InetAddress> remotePhysInetAddrs = new ArrayList<>();
    protected int remotePort = Constants.V6_PORT;

    protected String tunnelNetwork = null;
    
    LinkedBlockingQueue<Integer> doneQueue = new LinkedBlockingQueue<>();
    
    private static V6Machine v6Machine;
    public static synchronized V6Machine getInstance() {
        return v6Machine;
    }
    
    /**
     * Instantiates new V6 connections
     *
     * @param localPhysInetAddrs
     * @param localPort
     * @param remotePhysInetAddrs
     * @param remotePort
     * @param tunnelNetwork
     */
    public V6Machine(List<InetAddress> localPhysInetAddrs, int localPort, List<InetAddress> remotePhysInetAddrs, int remotePort,
        String tunnelNetwork) {
        v6Machine = this;
        
        this.localPhysInetAddrs = localPhysInetAddrs;
        this.localPort = localPort;
        this.remotePhysInetAddrs = remotePhysInetAddrs;
        this.remotePort = remotePort;
        this.tunnelNetwork = tunnelNetwork;
    }
    
    public static String getName() {
        return NAME;
    }

    /**
     * Start receiving and sending V6 configuration messages
     */
    @Override
    public void run() {
        if (localPhysInetAddrs == null) {
            log.error("A local internet address was not specified");
            return;            
        }
        
        if (remotePhysInetAddrs.isEmpty()) {
            log.error("A remote internet address was not specified");
            return;
        }
        
        log.info("Starting V6 machine on {}-{} at: {}", localPhysInetAddrs, localPort, LocalDateTime.now());

        factory = new NioDatagramChannelFactory(Executors.newCachedThreadPool(), InternetProtocolFamily.IPv6);
        
        machineDispatcher = new MachineDispatcher();
        machineDispatcher.start();
        
        for (InetAddress localInetAddr : localPhysInetAddrs) {
            InetSocketAddress localInetSockAddr = new InetSocketAddress(localInetAddr, localPort);
            DatagramChannel datagramChannel = addDatagramChannel(localInetSockAddr);
            if (datagramChannel == null) {
                log.error("Unable to create a datagram channel for the inet address " +
                    localInetSockAddr.getAddress().getHostAddress());
                continue;
            }
            
            for (InetAddress remoteInetAddr : remotePhysInetAddrs) {
                InetSocketAddress remoteInetSockAddr = new InetSocketAddress(remoteInetAddr, remotePort);
                
                ClientMachine machine = new ClientMachine(null, localInetSockAddr, remoteInetSockAddr,
                    Constants.V6_MESSAGE_TYPE_SOLICIT);
                
                machineDispatcher.dispatch(machine);
            }
        }

        try {
            doneQueue.take();
        } catch (InterruptedException ex) {
            log.info(ex.getMessage());
        }
        
        log.info("Closing channels...");
        channels.forEach((k, v) -> v.close());
        log.info("Done.");
    }

    public void shutdown() {
        doneQueue.add(Constants.THREAD_MESSAGE_STOP);
    }
    
    public void processMulticastSolicitMessage(DatagramPacket packet, byte[] buffer, int len) {
        try {
            for (InetAddress addr : localPhysInetAddrs) {
                if (WgConnect.isLocalV6Addr(packet.getAddress().getHostAddress())) {
                    continue;
                }
                
                InetSocketAddress serverInetSockAddr = new InetSocketAddress(addr, localPort);
                V6Message msg = V6Message.decode(ByteBuffer.wrap(buffer, 0, len),
                    serverInetSockAddr, new InetSocketAddress(packet.getAddress().getHostAddress(), packet.getPort()));
                
                if (WgConnect.isLocalV6Addr(msg.getRemoteAddress().getAddress().getHostAddress())) {
                    continue;
                }
                
                RemotePhysInetAddrOption remotePhysInetAddrOption =
                    (RemotePhysInetAddrOption) msg.getOption(Constants.OPTION_REMOTE_PHYS_INET_ADDR);
                RemotePhysInetComPortOption remotePhysInetComPortOption =
                    (RemotePhysInetComPortOption) msg.getOption(Constants.OPTION_REMOTE_PHYS_INET_COM_PORT);
                if (remotePhysInetAddrOption != null && remotePhysInetComPortOption != null) {
                    if (WgConnect.isLocalV6Addr(msg.getRemoteAddress().getAddress().getHostAddress()) ||
                        WgConnect.isLocalV6Addr(remotePhysInetAddrOption.getIpAddress())) {
                        return;
                    }
                
                    InetSocketAddress remotePhysInetSockAddr = new InetSocketAddress(new IPAddressString(
                        remotePhysInetAddrOption.getIpAddress()).toAddress().toInetAddress(),
                        (int) remotePhysInetComPortOption.getUnsignedInt());
                    msg.setRemoteAddress(remotePhysInetSockAddr);
                } else {
                    log.info("A received multicast message is missing required options");
                    return;
                }
                
                msg.setLocalAddress(serverInetSockAddr);
                
                if (WgConnect.getTunnelByLocalAndRemotePhysInetAddr(msg.getLocalAddress().getAddress().getHostAddress(),
                    msg.getRemoteAddress().getAddress().getHostAddress()) != null) {
                    continue;
                }

                machineDispatcher.dispatch(msg);
            }
        } catch (IOException | AddressStringException ex) {
            log.info(ex.getMessage());
        }
    }
    
    public String getTunnelNetwork() {
        return tunnelNetwork;
    }
    
    public synchronized String generateNextTunnelNet(String tunnelNet) {
        IPAddressString tunnelNetIPAddrStr = new IPAddressString(StringUtils.substringBefore(tunnelNet, IPv6Address.PREFIX_LEN_SEPARATOR));
        IPv6AddressSegment[] tunnelNetSegments = tunnelNetIPAddrStr.getAddress().toIPv6().getSegments();
        
        int newPrefixLen = 0;
        for (int i = 0; i < (tunnelNetSegments.length - 1); i++) {
            IPv6AddressSegment seg = tunnelNetSegments[i];
            if ((seg.getSegmentValue() + 1) < IPv6Address.MAX_VALUE_PER_SEGMENT) {
                tunnelNetSegments[i] = new IPv6AddressSegment(seg.getSegmentValue() + 1);
                newPrefixLen += IPv6Address.BITS_PER_SEGMENT;
                break;
            }
        }
        
        return new IPv6Address(tunnelNetSegments).toInetAddress().getHostAddress() + IPv6Address.PREFIX_LEN_SEPARATOR + newPrefixLen;
    }
    
    public IPv6Address applyTunnelNet(String tunnelNet, String inetAddr) {
        int tunnelNetPrefixLen;
        IPAddressString tunnelNetIPAddrStr = new IPAddressString(tunnelNet);
        IPv6AddressSegment[] tunnelNetSegments = tunnelNetIPAddrStr.getAddress().toIPv6().getSegments();
        
        if (tunnelNetIPAddrStr.isPrefixed()) {
            tunnelNetPrefixLen = tunnelNetIPAddrStr.getNetworkPrefixLength();
            if (tunnelNetPrefixLen < IPv6Address.BITS_PER_SEGMENT || tunnelNetPrefixLen > Constants.V6_MAX_TUNNEL_NETWORK_PREFIX_LEN) {
                tunnelNetPrefixLen = IPv6Address.BITS_PER_SEGMENT;
            }
        } else {
            tunnelNetIPAddrStr = new IPAddressString(tunnelNet + IPv6Address.PREFIX_LEN_SEPARATOR + IPv6Address.BITS_PER_SEGMENT);
            tunnelNetPrefixLen = tunnelNetIPAddrStr.getNetworkPrefixLength();
        }
        
        IPAddressString localInetIPAddrStr = new IPAddressString(StringUtils.substringBefore(inetAddr, IPv6Address.PREFIX_LEN_SEPARATOR) +
            IPv6Address.PREFIX_LEN_SEPARATOR + tunnelNetPrefixLen);
        
        IPv6AddressSegment[] tunnelInetAddrSegments = localInetIPAddrStr.getAddress().toIPv6().getSegments();
        System.arraycopy(tunnelNetSegments, 0, tunnelInetAddrSegments, 0, tunnelNetPrefixLen / IPv6Address.BITS_PER_SEGMENT);
        IPv6Address localTunnelIPv6Addr = new IPv6Address(new IPv6Address(tunnelInetAddrSegments).getBytes(), Constants.V6_SUBNET_MASK_32);
        
        return localTunnelIPv6Addr;
    }
    
    protected class ChannelHandler extends SimpleChannelUpstreamHandler {
        
        private final WgConnectLogger log = WgConnectLogger.getLogger(ChannelHandler.class);
        
        public ChannelHandler() {}
        
        /*
         * (non-Javadoc)
         * @see org.jboss.netty.channel.SimpleChannelHandler#messageReceived
         * (org.jboss.netty.channel.ChannelHandlerContext, org.jboss.netty.channel.MessageEvent)
         */
        @Override
        public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
            Object msg = e.getMessage();
            super.messageReceived(ctx, e);
            if (msg instanceof V6Message) {
                V6Message configMsg = (V6Message) msg;
                machineDispatcher.dispatch(configMsg);
            } else {
                // Note: in theory, we can't get here, because the codec would have thrown an exception beforehand
                log.error("Received unknown message object: " + (msg == null ? "n/a" : msg.getClass()));
            }
        }

        /* (non-Javadoc)
         * @see org.jboss.netty.channel.SimpleChannelUpstreamHandler#exceptionCaught
         * (org.jboss.netty.channel.ChannelHandlerContext, org.jboss.netty.channel.ExceptionEvent)
         */
        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
            log.error("Exception caught: " + e.getCause());
            e.getChannel().close();
        }
    }
    
    protected class MachineDispatcher extends Thread implements Runnable {

        static final String NAME = "MachineDispatcher";
        
        ExecutorService machineExecutor = Executors.newSingleThreadExecutor();

        LinkedBlockingQueue<Object> queue = new LinkedBlockingQueue<>();
        V6Message v6Msg = null;
                
        public MachineDispatcher() {
            super(NAME);
        }
        
        @Override
        public String toString() {
            return getName();
        }
        
        public void shutdown() {
            queue.add(null);
        }
        
        public void dispatch(V6Message msg) {
            if (msg != null) {
                queue.add(msg);
            }
        }
        
        public void dispatch(Machine machine) {
            if (machine != null) {
                queue.add(machine);
            }
        }
        
        @Override
        public void run() {
            while (true) {
                try {
                    Object obj = queue.take();
                    if (obj == null) {
                        break;
                    }
                    
                    if (obj instanceof V6Message) {
                        v6Msg = (V6Message) obj;
                        
                        short msgType = v6Msg.getMessageType();
                        short msgSender = v6Msg.getMessageSender();
                        switch (msgSender) {
                            case Constants.V6_MESSAGE_SENDER_CLIENT:
                                ServerMachine sm;
                                if (serverMachine == null ||
                                    serverMachine.getMachineId() != v6Msg.getTransactionId() ||
                                    (!serverMachine.getLocalPhysInetAddr().equalsIgnoreCase(v6Msg.getLocalAddress().getAddress().getHostAddress()) &&
                                    !serverMachine.getLocalTunnelInetAddr().equalsIgnoreCase(v6Msg.getLocalAddress().getAddress().getHostAddress()))) {
                                    sm = new ServerMachine((int) v6Msg.getTransactionId(),
                                        new InetSocketAddress(v6Msg.getLocalAddress().getAddress(), (int) v6Msg.getLocalAddress().getPort()),
                                        v6Msg.getRemoteAddress(), tunnelNetwork);

                                    machineExecutor.submit(sm);
                                } else {
                                    sm = serverMachine;
                                }

                                switch (msgType) {
                                    case Constants.V6_MESSAGE_TYPE_SOLICIT:
                                        sm.solicitMsgQueue.add(v6Msg);
                                        break;

                                    case Constants.V6_MESSAGE_TYPE_REQUEST:
                                        sm.requestsReceived.getAndIncrement();
                                        sm.requestMsg = v6Msg;
                                        sm.replySemaphore.release();
                                        break;

                                    case Constants.V6_MESSAGE_TYPE_TUNNEL_PING:
                                        sm.tunnelPingsReceived.getAndIncrement();
                                        sm.pingMsg = v6Msg;
                                        sm.replySemaphore.release();
                                        break;

                                    case Constants.V6_MESSAGE_TYPE_INFO_REQUEST:
                                        if (!sm.configInfoMsgQueue.add(v6Msg)) {
                                            log.error("Unable to add the message " + v6Msg + " to the config info message queue");
                                        }
                                        break;

                                    default:
                                        log.info("Received unhandled message type: " + v6Msg.getMessageType());
                                        break;
                                }

                                break;
                               
                            case Constants.V6_MESSAGE_SENDER_SERVER:
                                ClientMachine cm;
                                if (clientMachine == null || clientMachine.getMachineId() != v6Msg.getTransactionId() ||
                                    (!clientMachine.getLocalPhysInetAddr().equalsIgnoreCase(v6Msg.getLocalAddress().getAddress().getHostAddress()) &&
                                    !clientMachine.getLocalTunnelInetAddr().equalsIgnoreCase(v6Msg.getLocalAddress().getAddress().getHostAddress()))) {
                                    cm = new ClientMachine((int) v6Msg.getTransactionId(),
                                        new InetSocketAddress(v6Msg.getLocalAddress().getAddress(), (int) v6Msg.getLocalAddress().getPort()),
                                        new InetSocketAddress(StringUtils.substringBefore(v6Msg.getRemoteAddress().getAddress().getHostAddress(),
                                            IPv6Address.ZONE_SEPARATOR), (int) v6Msg.getRemoteAddress().getPort()), v6Msg.getMessageType());

                                    machineExecutor.submit(cm);
                                } else {
                                    cm = clientMachine;
                                }

                                switch (msgType) {
                                    case Constants.V6_MESSAGE_TYPE_ADVERTISE:
                                        cm.advertisesReceived.getAndIncrement();
                                        cm.advertiseMsg = v6Msg;
                                        cm.replySemaphore.release();
                                        break;

                                    case Constants.V6_MESSAGE_TYPE_REPLY:
                                        cm.repliesReceived.getAndIncrement();
                                        cm.replyMsg = v6Msg;
                                        cm.replySemaphore.release();
                                        break;

                                    case Constants.V6_MESSAGE_TYPE_TUNNEL_PING_REPLY:
                                        cm.tunnelPingRepliesReceived.getAndIncrement();
                                        cm.pingReplyMsg = v6Msg;
                                        cm.replySemaphore.release();
                                        break;

                                    default:
                                        log.info("Received unhandled message type: " + v6Msg.getMessageType());
                                        break;
                                }
                                
                                break;
                            
                            default:
                                log.error("Received message from unknown sender: " + v6Msg.getClass());
                                break;
                        }
                    }
                    
                    if (obj instanceof Machine) {
                        Machine machine = (Machine) obj;

                        if (!(machine instanceof ServerMachine) && !(machine instanceof ClientMachine)) {
                            log.info("Unknown machine type");
                            break;
                        }

                        machineExecutor.submit(machine);
                    }
                } catch (InterruptedException ex) {
                    log.info(ex.getMessage());
                }
            }
            
            log.info("Shutting down executor...");
            machineExecutor.shutdown();
        }
    }
    
    public class MessageInfo {
        private final PersistenceTunnel tunnel;
        private final V6Message message;
        
        public MessageInfo(PersistenceTunnel tunnel, V6Message message) {
            this.tunnel = tunnel;
            this.message = message;
        }
        
        public PersistenceTunnel getTunnel() {
            return tunnel;
        }
        
        public V6Message getMessage() {
            return message;
        }
    }
    
    protected abstract class Machine implements Runnable {
        String NAME = StringUtils.EMPTY;

        InetSocketAddress localPhysInetSockAddr = null;
        InetSocketAddress remotePhysInetSockAddr = null;
        
        String tunnelInetNet = null;
        IPv6Address localTunnelIPv6Addr = null;
        
        String remoteTunnelInetAddr = null;
        
        PersistenceTunnel persistenceTunnel = null;

        V6Message v6Msg = null;
        
        int id = 0;
        
    	Semaphore replySemaphore = new Semaphore(1);
        
        boolean retry = false;
        
        int state = 0;
        
        public String getName() {
            return NAME + "-" + localPhysInetSockAddr.getAddress().getHostAddress() + "-" +
                StringUtils.substringAfterLast(remotePhysInetSockAddr.getAddress().getHostAddress(), IPv6Address.SEGMENT_SEPARATOR);
        }

        @Override
        public String toString() {
            return getName();
        }
        
        public int getMachineId() {
            return id;
        }
        
        public V6Machine getV6Machine() {
            return v6Machine;
        }
        
        public InetSocketAddress getRemotePhysInetSockAddr() {
            return remotePhysInetSockAddr;
        }
        
        public String getRemotePhysInetAddr() {
            return remotePhysInetSockAddr.getAddress().getHostAddress();
        }
        
        public InetSocketAddress getLocalPhysInetSockAddr() {
            return localPhysInetSockAddr;
        }
        
        public String getLocalPhysInetAddr() {
            return localPhysInetSockAddr.getAddress().getHostAddress();
        }
        
        public int getState() {
            return state;
        }
        
        public void setRemoteTunnelInetAddr(String remoteTunnelInetAddr) {
            this.remoteTunnelInetAddr = remoteTunnelInetAddr;
        }
        
        public String getRemoteTunnelInetAddr() {
            return remoteTunnelInetAddr;
        }
        
        public void setTunnelInetNet(String tunnelInetNet) {
            this.tunnelInetNet = tunnelInetNet;
        }
        
        public String getTunnelInetNet() {
            return tunnelInetNet;
        }
        
        public String getLocalTunnelInetAddr() {
            return localTunnelIPv6Addr.toInetAddress().getHostAddress();
        }
        
        public IPv6Address getLocalTunnelIPv4Addr() {
            return localTunnelIPv6Addr;
        }

        public String generateNextTunnelNet() {
            configureLocalTunnelAddr(v6Machine.generateNextTunnelNet(tunnelInetNet));

            return tunnelInetNet;
        }
        
        public void setPersistenceTunnel(PersistenceTunnel persistenceTunnel) {
            this.persistenceTunnel = persistenceTunnel;
        }
        
        public PersistenceTunnel getPersistenceTunnel() {
            return persistenceTunnel;
        }

        public void configureLocalTunnelAddr(String tunnelInetNet) {
            this.tunnelInetNet = tunnelInetNet;
            
            IPAddressString tunnelNetIPAddrStr = new IPAddressString(tunnelInetNet);
            IPv6AddressSegment[] tunnelNetSegments = tunnelNetIPAddrStr.getAddress().toIPv6().getSegments();

            int tunnelNetPrefixLen;
            if (tunnelNetIPAddrStr.isPrefixed()) {
                tunnelNetPrefixLen = tunnelNetIPAddrStr.getNetworkPrefixLength();
                if (tunnelNetPrefixLen < IPv6Address.BITS_PER_SEGMENT || tunnelNetPrefixLen > Constants.V6_MAX_TUNNEL_NETWORK_PREFIX_LEN) {
                    tunnelNetPrefixLen = IPv6Address.BITS_PER_SEGMENT;
                }
            } else {
                tunnelNetIPAddrStr = new IPAddressString(tunnelInetNet + IPv6Address.PREFIX_LEN_SEPARATOR + IPv6Address.BITS_PER_SEGMENT);
                tunnelNetPrefixLen = tunnelNetIPAddrStr.getNetworkPrefixLength();
            }
            
            IPAddressString localInetIPAddrStr = new IPAddressString(StringUtils.substringBefore(localPhysInetSockAddr.getAddress().getHostAddress(),
                IPv6Address.PREFIX_LEN_SEPARATOR) + IPv6Address.PREFIX_LEN_SEPARATOR + tunnelNetPrefixLen);
            
            IPv6AddressSegment[] tunnelInetAddrSegments = localInetIPAddrStr.getAddress().toIPv6().getSegments();
            System.arraycopy(tunnelNetSegments, 0, tunnelInetAddrSegments, 0, tunnelNetPrefixLen / IPv6Address.BITS_PER_SEGMENT);
            localTunnelIPv6Addr = new IPv6Address(new IPv6Address(tunnelInetAddrSegments).getBytes(), Constants.V6_SUBNET_MASK_32);
        }
    }
    
    /**
     * The ClientMachine class
     */
    protected class ClientMachine extends Machine implements ChannelFutureListener {
                        
        AtomicInteger solicitsSent = new AtomicInteger();
        AtomicInteger advertisesReceived = new AtomicInteger();
        AtomicInteger requestsSent = new AtomicInteger();
        AtomicInteger repliesReceived = new AtomicInteger();

        AtomicInteger tunnelPingsSent = new AtomicInteger();
        AtomicInteger tunnelPingRepliesReceived = new AtomicInteger();
                
        V6Message advertiseMsg;
        V6Message replyMsg;
        V6Message pingReplyMsg;
                
        public ClientMachine(Integer id, InetSocketAddress localInetSockAddr, InetSocketAddress remoteInetSockAddr, int state) {
            NAME = "ClientMachine";
            
            if (id != null) {
                this.id = id;
            } else {
                this.id = random.nextInt();
            }
            
            this.localPhysInetSockAddr = localInetSockAddr;
            this.remotePhysInetSockAddr = remoteInetSockAddr;
            this.tunnelInetNet = tunnelNetwork;
            
            this.state = state;
            
            configureLocalTunnelAddr(tunnelInetNet);

            replySemaphore.drainPermits();
        }
        
        @Override
        public void run() {
            clientMachine = this;
            
            switch (state) {
                case Constants.V6_MESSAGE_TYPE_SOLICIT:
                    solicit();
                    break;

                case Constants.V6_MESSAGE_TYPE_ADVERTISE:
                    waitForAdvertise();
                    break;

                case Constants.V6_MESSAGE_TYPE_REQUEST:
                    request();
                    break;

                case Constants.V6_MESSAGE_TYPE_REPLY:
                    waitForReply();
                    break;

                case Constants.V6_MESSAGE_TYPE_TUNNEL_PING:
                    tunnelPing();
                    break;

                case Constants.V6_MESSAGE_TYPE_TUNNEL_PING_REPLY:
                    waitForTunnelPingReply();
                    break;

                default:
                    log.info("Unknown client state: " + state);
                    break;
            }
            
            clientMachine = null;
        }
        
        private void solicit() {
            v6Msg = buildSolicitMessage(this);
            
            if (Utils.isMulticastAddress(remotePhysInetSockAddr.getAddress().getHostAddress())) {
                try {
                    DatagramSocket dataSock = new DatagramSocket();

                    byte[] msgBytes = v6Msg.encode().array();
                    dataSock.send(new DatagramPacket(msgBytes, msgBytes.length,
                        remotePhysInetSockAddr.getAddress(), remotePhysInetSockAddr.getPort()));
                } catch (IOException ex) {
                    log.info(ex.getMessage());
                }
                
                return;
            } else {
                DatagramChannel c = channels.get(localPhysInetSockAddr.getAddress().getHostAddress());
                if (c != null) {
                    ChannelFuture future = c.write(v6Msg, remotePhysInetSockAddr);
                    future.addListener(this);
                } else {
                    log.error("Could not find a DatagramChannel for inet address {}", localPhysInetSockAddr.getAddress().getHostAddress());
                }
            }
            
            state = Constants.V6_MESSAGE_TYPE_SOLICIT;
            waitForAdvertise();
        }

        private void waitForAdvertise() {
            try {
                if (!replySemaphore.tryAcquire(WAIT_FOR_REPLY_TIME, TimeUnit.SECONDS)) {
                    if (retry) {
                        retry = false;
                        log.warn("Advertise timeout after {} seconds, retrying...", WAIT_FOR_REPLY_TIME);
                        solicit();
                    }
                } else {
                    state = Constants.V6_MESSAGE_TYPE_ADVERTISE;
                        
                    request();
                }
            } catch (InterruptedException ex) {
                log.info(ex.getMessage());
            }
        }

        private void request() {
            if (advertiseMsg != null) {
                if (Utils.isMulticastAddress(remotePhysInetSockAddr.getAddress().getHostAddress())) {
                    remotePhysInetSockAddr = new InetSocketAddress(advertiseMsg.getRemoteAddress().getAddress().getHostAddress(),
                        advertiseMsg.getRemoteAddress().getPort());
                }
                
                MessageInfo info = buildRequestMessage(this, advertiseMsg);
                if (info != null) {
                    v6Msg = info.getMessage();
                    
                    DatagramChannel c = channels.get(localPhysInetSockAddr.getAddress().getHostAddress());
                    if (c != null) {
                        ChannelFuture future = c.write(v6Msg, remotePhysInetSockAddr);
                        future.addListener(this);
                    } else {
                        log.error("Could not find a DatagramChannel for inet address {}", localPhysInetSockAddr.getAddress().getHostAddress());
                    }
                    
                    GenericResponseOption advertiseResponseOption = (GenericResponseOption) v6Msg.getOption(Constants.OPTION_GENERIC_RESPONSE);
                    switch ((int)advertiseResponseOption.getUnsignedInt()) {
                        case Constants.RESPONSE_DECLINE_REDUNDANT_PUBLIC_KEY:
                        case Constants.RESPONSE_DECLINE_TUNNEL_NETWORK:
                            waitForAdvertise();
                            break;
                            
                        case Constants.RESPONSE_ACCEPT:
                        default:
                            state = Constants.V6_MESSAGE_TYPE_REQUEST;
                            waitForReply();
                            break;
                    }
                } else {
                    log.error("Request error");
                }
            } else {
                log.error("Request error");
            }
        }

        private void waitForReply() {
            try {
                if (!replySemaphore.tryAcquire(WAIT_FOR_REPLY_TIME, TimeUnit.SECONDS)) {
                    if (retry) {
                        retry = false;
                        log.warn("Request timeout after {} seconds, retrying...", WAIT_FOR_REPLY_TIME);
                        request();
                    }
                } else {
                    state = Constants.V6_MESSAGE_TYPE_REPLY;

                    tunnelPing();
                }
            } catch (InterruptedException ex) {
                log.info(ex.getMessage());
            }
        }

        private void tunnelPing() {
            if (replyMsg != null) {
                MessageInfo info = buildTunnelPingMessage(this, replyMsg);
                if (info != null) {
                    DatagramChannel c = addDatagramChannel(info.getTunnel().getLocalTunnelInetSockAddr());
                                       
                    v6Msg = info.getMessage();
                    ChannelFuture future = c.write(v6Msg, v6Msg.getRemoteAddress());
                    future.addListener(this);
                    
                    state = Constants.V6_MESSAGE_TYPE_TUNNEL_PING;
                    
                    waitForTunnelPingReply();
                } else {
                    log.error("Tunnel ping error");
                }
            } else {
                log.error("Tunnel ping error");
            }
        }
        
        private void waitForTunnelPingReply() {
            try {
                if (!replySemaphore.tryAcquire(WAIT_FOR_REPLY_TIME, TimeUnit.SECONDS)) {
                    if (retry) {
                        retry = false;
                        log.info("Request timeout after {} seconds, retrying...", WAIT_FOR_REPLY_TIME);
                        tunnelPing();
                    }
                } else {
                    state = Constants.V6_MESSAGE_TYPE_TUNNEL_PING_REPLY;

                    PersistenceTunnel tunnel = checkPingReplyMessage(this, pingReplyMsg);
                    log.info("A valid remote tunnel ping reply was{}received from.", (tunnel == null ? " not " : " "), getRemotePhysInetAddr());
                }
            } catch (InterruptedException ex) {
                log.info(ex.getMessage());
                log.error("Tunnel ping reply error");
            }
        }

        /* (non-Javadoc)
         * @see org.jboss.netty.channel.ChannelFutureListener#operationComplete(org.jboss.netty.channel.ChannelFuture)
         */
        @Override
        public void operationComplete(ChannelFuture future) throws Exception {
            if (future.isSuccess()) {
                switch (v6Msg.getMessageType()) {
                    case Constants.V6_MESSAGE_TYPE_SOLICIT:
                        solicitsSent.getAndIncrement();
                        log.info("Successfully sent solicit message clientId = {} cnt = {}", id, solicitsSent);
                        break;
                        
                    case Constants.V6_MESSAGE_TYPE_REQUEST:
                        requestsSent.getAndIncrement();
                        log.info("Successfully sent request message to {}, cnt = {}", getRemotePhysInetAddr(), requestsSent);
                        break;
                        
                    case Constants.V6_MESSAGE_TYPE_TUNNEL_PING:
                        tunnelPingsSent.getAndIncrement();
                        log.info("Successfully sent tunnel ping message to {}, cnt = {}", getRemotePhysInetAddr(), tunnelPingsSent);
                        break;
                    
                    default:
                        break;
                }
            } else {
                log.error("Failed to send message id = " + v6Msg.getTransactionId() + ": " + future.getCause());
            }
        }
    }
    // end of ClientMachine
    
    public synchronized ServerMachine setServerMachine(ServerMachine machine) {
        serverMachine = machine;
        
        return serverMachine;
    }
    
    /**
     * The ServerMachine class
     */
    public class ServerMachine extends Machine implements ChannelFutureListener {

        PersistenceTunnel referenceTunnel = null;
        
        AtomicInteger solicitsReceived = new AtomicInteger();
        AtomicInteger advertisesSent = new AtomicInteger();
        AtomicInteger requestsReceived = new AtomicInteger();
        AtomicInteger repliesSent = new AtomicInteger();
        AtomicInteger tunnelPingsReceived = new AtomicInteger();
        AtomicInteger tunnelPingRepliesSent = new AtomicInteger();

    	LinkedBlockingQueue<V6Message> solicitMsgQueue = new LinkedBlockingQueue<>();
        LinkedBlockingQueue<V6Message> configInfoMsgQueue = new LinkedBlockingQueue<>();

        V6Message solicitMsg;
        V6Message requestMsg;
        V6Message pingMsg;
                
        LinkedBlockingQueue<Integer> msgQueue = new LinkedBlockingQueue<>(1);
        
        public ServerMachine(int id, InetSocketAddress localInetSockAddr, InetSocketAddress remoteInetSockAddr, String tunnelInetNet) {
            NAME = "ServerMachine";
            
            this.id = id;
            
            this.localPhysInetSockAddr = localInetSockAddr;
            this.remotePhysInetSockAddr = remoteInetSockAddr;
            this.tunnelInetNet = tunnelInetNet;
            
            configureLocalTunnelAddr(tunnelInetNet);

            replySemaphore.drainPermits();
        }
        
        public PersistenceTunnel getReferenceTunnel() {
            return referenceTunnel;
        }
        
        @Override
        public void run() {
            serverMachine = this;
            
            // Find all current tunnels using the localPhysInetAddr
            List<PersistenceTunnel> tunnels = WgConnect.getTunnelsByLocalPhysInetAddr(localPhysInetSockAddr.getAddress().getHostAddress());
            if (!tunnels.isEmpty()) {
                referenceTunnel = tunnels.get(0);
                tunnelInetNet = referenceTunnel.getTunnelInetNet();
            }
            
            waitForSolicit();
            
            serverMachine = null;
        }

        private void waitForSolicit () {
            try {
                solicitMsg = solicitMsgQueue.take();
                solicitsReceived.getAndIncrement();
                
                Integer msg = msgQueue.poll();
                if (msg != null) {
                    switch (msg) {
                        case Constants.THREAD_MESSAGE_STOP:
                            log.info("Received thread message STOP");
                            return;

                        default:
                            break;
                    }
                }

                if (solicitMsg != null) {
                    advertise();
                }
            } catch (InterruptedException ex) {
                log.info(ex.getMessage());
            }
        }
        
        private void advertise() {
            state = Constants.V6_MESSAGE_TYPE_ADVERTISE;
            
            v6Msg = new V6SolicitProcessor(this, solicitMsg, solicitMsg.getRemoteAddress().getAddress()).processMessage();
            if (v6Msg != null) {
                remotePhysInetSockAddr = v6Msg.getRemoteAddress();
                
                DatagramChannel c = channels.get(localPhysInetSockAddr.getAddress().getHostAddress());
                if (c != null) {
                    ChannelFuture future = c.write(v6Msg, remotePhysInetSockAddr);
                    future.addListener(this);
                } else {
                    log.error("Could not find a DatagramChannel for inet address {}", localPhysInetSockAddr.getAddress().getHostAddress());
                }
                
                waitForRequest();
            } else {
                log.error("Error processing solicit message");
            }
        }

        private void waitForRequest() {
            try {
                if (!replySemaphore.tryAcquire(WAIT_FOR_REPLY_TIME, TimeUnit.SECONDS)) {
                    if (retry) {
                        retry = false;
                        log.info("Request timeout after {} seconds, retrying...", WAIT_FOR_REPLY_TIME);
                        advertise();
                    }
                } else {
                    state = Constants.V6_MESSAGE_TYPE_REQUEST;
                    
                    reply();
                }
            } catch (InterruptedException ex) {
                log.info(ex.getMessage());
            }
        }

        private void reply() {
            v6Msg = new V6RequestProcessor(this, requestMsg, requestMsg.getRemoteAddress().getAddress()).processMessage();
            if (v6Msg != null) {
                DatagramChannel c = channels.get(localPhysInetSockAddr.getAddress().getHostAddress());
                if (c != null) {
                    ChannelFuture future = c.write(v6Msg, remotePhysInetSockAddr);
                    future.addListener(this);
                } else {
                    log.error("Could not find a DatagramChannel for inet address {}", localPhysInetSockAddr.getAddress().getHostAddress());
                }

                GenericResponseOption offerResponseOption = (GenericResponseOption) requestMsg.getOption(Constants.OPTION_GENERIC_RESPONSE);
                
                switch ((int) offerResponseOption.getUnsignedInt()) {
                    case Constants.RESPONSE_DECLINE_REDUNDANT_PUBLIC_KEY:
                    case Constants.RESPONSE_DECLINE_TUNNEL_NETWORK:
                        waitForRequest();
                        break;

                    case Constants.RESPONSE_ACCEPT:
                    default:
                        state = Constants.V6_MESSAGE_TYPE_REPLY;
                        waitForTunnelPing();
                        break;
                }
            } else {
                log.error("Error processing request message");
            }
        }
        
        private void waitForTunnelPing() {
            try {
                if (!replySemaphore.tryAcquire(WAIT_FOR_REPLY_TIME, TimeUnit.SECONDS)) {
                    if (retry) {
                        retry = false;
                        log.info("Request timeout after {} seconds, retrying...", WAIT_FOR_REPLY_TIME);
                    }
                } else {
                    state = Constants.V6_MESSAGE_TYPE_TUNNEL_PING;

                    tunnelPingReply();
                }
            } catch (InterruptedException ex) {
                log.info(ex.getMessage());
            }
        }
        
        private void tunnelPingReply() {
            v6Msg = new V6PingProcessor(this, pingMsg, pingMsg.getRemoteAddress().getAddress()).processMessage();
            if (v6Msg != null) {
                DatagramChannel c = channels.get(pingMsg.getLocalAddress().getAddress().getHostAddress());
                if (c != null) {
                    ChannelFuture future = c.write(v6Msg, pingMsg.getRemoteAddress());
                    future.addListener(this);
                } else {
                    log.error("Could not find a DatagramChannel for inet address {}", localPhysInetSockAddr.getAddress().getHostAddress());
                }

                waitForTunnelPingReplySent();
            } else {
                log.error("Error processing tunnel ping message");
            }
        }

        public void waitForTunnelPingReplySent() {
            state = Constants.V6_MESSAGE_TYPE_TUNNEL_PING_REPLY;
        }
        
        private void tunnelsInfoRequestReply(V6Message infoRequestMsg) {
            if (infoRequestMsg != null) {
                List<V6Machine.MessageInfo> infoMsgs = buildPeerTunnelsInfoRequestReplyMessage(this, infoRequestMsg);
                
                if (!infoMsgs.isEmpty()) {
                    for (V6Machine.MessageInfo info : infoMsgs) {
                        state = Constants.V6_MESSAGE_TYPE_INFO_REQUEST;

                        v6Msg = info.getMessage();
                        ChannelFuture future = getDatagramChannelByInetAddr(info.getTunnel().getLocalPhysInetAddr())
                            .write(v6Msg, v6Msg.getRemoteAddress());
                        future.addListener(this);
                    }
                }
            }
        }
    
        /* (non-Javadoc)
         * @see org.jboss.netty.channel.ChannelFutureListener#operationComplete(org.jboss.netty.channel.ChannelFuture)
         */
        @Override
        public void operationComplete(ChannelFuture future) throws Exception {
            if (future.isSuccess()) {
                switch (v6Msg.getMessageType()) {
                    case Constants.V6_MESSAGE_TYPE_ADVERTISE:
                        advertisesSent.getAndIncrement();
                        log.info("Successfully sent advertise message cnt = {}", advertisesSent);
                        break;
                    
                    case Constants.V6_MESSAGE_TYPE_REPLY:
                        repliesSent.getAndIncrement();
                        log.info("Successfully sent reply message cnt = {}", repliesSent);
                        break;
                        
                    case Constants.V6_MESSAGE_TYPE_TUNNEL_PING_REPLY:
                        tunnelPingRepliesSent.getAndIncrement();
                        log.info("Successfully sent tunnel ping reply message cnt = {}", tunnelPingRepliesSent);
                        replySemaphore.release();
                        break;
                        
                    default:
                        break;
                }
            } else {
                log.error("Failed to send message id = " + v6Msg.getTransactionId() + ":" + future.getCause().toString());
            }
        }
    }
    // end of ServerMachine
    
    /*
     *  Create and initialize a tunnel element
     */
    public synchronized PersistenceTunnel createTunnelAsClient(String remoteEndpointType, String tunnelId, int clientId,
        String remotePhysInetAddr, String localPhysInetAddr, String remoteTunnelInetAddr, String localTunnelInetAddr,
        String remotePublicKey, long remoteListenPort, long remoteTunnelInetComPort, String tunnelNet, 
        String remoteInterfaceName, boolean force) {
        
        PersistenceTunnel tunnel = new PersistenceTunnel();
        tunnel.setInetType(IPVersion.IPV6.toString());
        tunnel.setId(UUID.fromString(tunnelId));
        tunnel.setRemoteId(clientId);

        tunnel.setLocalEndpointType(Constants.TUNNEL_ENDPOINT_TYPE_CLIENT);
        tunnel.setRemoteEndpointType(remoteEndpointType);

        tunnel.setRemotePhysInetAddr(remotePhysInetAddr);

        tunnel.setLocalPhysInetAddr(localPhysInetAddr);
        tunnel.setLocalPhysInetComPort(localPort);
        
        tunnel.setLocalTunnelInetAddr(localTunnelInetAddr);

        tunnel.setRemoteTunnelInetAddr(remoteTunnelInetAddr);
        tunnel.setRemoteTunnelInetComPort(remoteTunnelInetComPort);
        tunnel.setTunnelInetNet(tunnelNet);
        
        tunnel.setRemotePublicKey(remotePublicKey);
        tunnel.setRemotePhysInetListenPort(remoteListenPort);

        tunnel.setKeepalive(WgConnect.getPersistentKeepalive());
                
        Wg wg = new Wg();
        int exitCode;
        
        PersistenceTunnel referenceTunnel = WgConnect.getTunnelByLocalTunnelInetAddr(localTunnelInetAddr);
        if (force || referenceTunnel == null) {
            // Check for an existing WgConnect V6 device that is not in wgConnectTunnels
            String ifName = Utils.getAnyExistingWgConnectIfByPrefix(Constants.getTunnelInterfacePrefix(IPVersion.IPV6));
            if (WgConnect.getTunnelByLocalIfName(ifName) != null ||
                Utils.getWgConnectIfByPrefixAndEndpointAddr(Constants.getTunnelInterfacePrefix(IPVersion.IPV6), remotePhysInetAddr) != null) {
                force = true;
            }
            
            if (!force && ifName != null) {
                tunnel.setLocalInterfaceName(ifName);

                tunnel.setLocalTunnelInetAddr(wg.getDeviceInetAddr(ifName, IPVersion.IPV6));
                tunnel.setLocalTunnelInetComPort(localPort);
                tunnel.setLocalTunnelInetSockAddr(new InetSocketAddress(tunnel.getLocalTunnelInetAddr(),
                    (int) tunnel.getLocalTunnelInetComPort()));

                tunnel.setLocalPrivateKey(wg.getInterfacePrivateKey(ifName));
                tunnel.setLocalPublicKey(wg.getInterfacePublicKey(ifName));
                tunnel.setLocalPreSharedKey(wg.getInterfacePresharedKey(ifName));

                // Get the listen port for the existing link
                long listenPort = wg.getInterfaceListenPort(tunnel.getLocalInterfaceName());
                if (listenPort > 0) {
                    tunnel.setLocalPhysInetListenPort(listenPort);
                } else {
                    log.error("Unable to obtain the listen port for WgConnect device " + tunnel.getLocalInterfaceName());
                    return null;
                }
            } else {
                // Generate the v6 interface name
                ifName = Utils.getNextAvailableNetIfName(Constants.getTunnelInterfacePrefix(IPVersion.IPV6), -1);
                tunnel.setLocalInterfaceName(ifName);
                tunnel.setLocalTunnelInetComPort(localPort);

                if (!wg.generateKeys()) {
                    log.error("Unable to generate WgConnect cryptographic keys");
                    return null;
                }

                tunnel.setLocalPublicKey(wg.getPublicKey());
                tunnel.setLocalPrivateKey(wg.getPrivateKey());

                if (tunnel.getLocalPrivateKey() == null || tunnel.getLocalPublicKey() == null) {
                    log.error("Unable to generate or retrieve WgConnect crytographic keys");
                    return null;
                }

                // Add, configure, and bring up the WgConnect network link device
                exitCode = wg.addDevice(tunnel.getLocalInterfaceName());
                if (exitCode == Wg.getCommandFailureCode()) {
                    log.error("Unable to add the WgConnect device " + tunnel.getLocalInterfaceName());
                    return null;
                }

                exitCode = wg.setDeviceInetAddr(tunnel.getLocalInterfaceName(),
                    tunnel.getLocalTunnelInetAddr(), Integer.toString(Constants.V6_SUBNET_MASK_64));
                if (exitCode == Wg.getCommandFailureCode()) {
                    log.error("Unable to set the inet address for WgConnect device " + tunnel.getLocalInterfaceName());
                    return null;
                }

                exitCode = wg.setInterfacePrivateKey(tunnel.getLocalInterfaceName(), tunnel.getLocalPrivateKey());
                if (exitCode == Wg.getCommandFailureCode()) {
                    log.error("Unable to set private key for WgConnect device " + tunnel.getLocalInterfaceName());
                    return null;
                }

                wg.setDeviceState(tunnel.getLocalInterfaceName(),
                    InterfaceDeviceManager.InterfaceDeviceState.UP);
                if (wg.getCommandExitCode() == Wg.getCommandFailureCode()) {
                    log.error("Unable to set the link state for WgConnect device " + tunnel.getLocalInterfaceName());
                    return null;
                }

                // Get the listen port for the new link
                long listenPort = wg.getInterfaceListenPort(tunnel.getLocalInterfaceName());
                if (listenPort > 0) {
                    tunnel.setLocalPhysInetListenPort(listenPort);
                } else {
                    log.error("Unable to obtain the listen-port for WgConnect device " + tunnel.getLocalInterfaceName());
                    return null;
                }

                exitCode = wg.setInterfaceConfigParameters(tunnel.getLocalInterfaceName(),
                    Wg.OPTION_PEER, tunnel.getRemotePublicKey(),
                    Wg.OPTION_ALLOWED_IPS, tunnel.getRemoteTunnelInetAddr() + IPv6Address.PREFIX_LEN_SEPARATOR + Constants.V6_SUBNET_MASK_64,
                    Wg.OPTION_ENDPOINT, tunnel.getRemotePhysInetAddr() + ":" + tunnel.getRemotePhysInetListenPort(),
                    Wg.OPTION_PERSISTENT_KEEPALIVE, Integer.toString(WgConnect.getPersistentKeepalive())
                );
                if (exitCode == Wg.getCommandFailureCode()) {
                    log.error("Unable to set the peer configuration for the device " + tunnel.getLocalInterfaceName());
                    return null;
                }

                // Add the tunnel to the datachannels
                tunnel.setLocalTunnelInetSockAddr(new InetSocketAddress(tunnel.getLocalTunnelInetAddr(),
                    (int) tunnel.getLocalTunnelInetComPort()));
                addDatagramChannel(tunnel.getLocalTunnelInetSockAddr());
                
                // Add the tunnel to the GUI
                Gui.addTunnel(tunnel);
            }
        } else {
            tunnel.setLocalInterfaceName(referenceTunnel.getLocalInterfaceName());

            tunnel.setLocalTunnelInetAddr(referenceTunnel.getLocalTunnelInetAddr());
            tunnel.setLocalTunnelInetComPort(referenceTunnel.getLocalTunnelInetComPort());
            tunnel.setLocalTunnelInetSockAddr(referenceTunnel.getLocalTunnelInetSockAddr());
            
            tunnel.setLocalPublicKey(referenceTunnel.getLocalPublicKey());
            tunnel.setLocalPrivateKey(referenceTunnel.getLocalPrivateKey());
            tunnel.setLocalPreSharedKey(referenceTunnel.getLocalPreSharedKey());
            
            tunnel.setLocalPhysInetListenPort(referenceTunnel.getLocalPhysInetListenPort());
            
            exitCode = wg.setInterfaceConfigParameters(tunnel.getLocalInterfaceName(),
                Wg.OPTION_PEER, tunnel.getRemotePublicKey(),
                Wg.OPTION_ALLOWED_IPS, tunnel.getRemoteTunnelInetAddr() + IPv6Address.PREFIX_LEN_SEPARATOR + Constants.V6_SUBNET_MASK_64,
                Wg.OPTION_ENDPOINT, tunnel.getRemotePhysInetAddr() + ":" + tunnel.getRemotePhysInetListenPort(),
                Wg.OPTION_PERSISTENT_KEEPALIVE,
                Integer.toString(WgConnect.getPersistentKeepalive())
            );
            if (exitCode == Wg.getCommandFailureCode()) {
                log.error("Unable to set the peer configuration for the device " + tunnel.getLocalInterfaceName());
                return null;
            }

            // Add the tunnel to the datachannels
            tunnel.setLocalTunnelInetSockAddr(new InetSocketAddress(tunnel.getLocalTunnelInetAddr(),
                (int) tunnel.getLocalTunnelInetComPort()));
            addDatagramChannel(tunnel.getLocalTunnelInetSockAddr());
            
            // Add the tunnel to the GUI
            Gui.addTunnel(tunnel);
        }

        WgConnect.addTunnel(tunnel);
        
        return tunnel;
    }
    
    public synchronized PersistenceTunnel createTunnelAsServer(ServerMachine v6ServerMachine, int remoteId,
        String localEndpointType, String remoteEndpointType, String localPhysInetAddr, String remotePhysInetAddr,
        long remotePhysInetComPort, String remoteTunnelInetAddr, String localTunnelInetAddr, String tunnelInetNet,
        boolean force) throws Exception {
        
        PersistenceTunnel tunnel = new PersistenceTunnel();
        tunnel.setRemoteId(remoteId);
        tunnel.setId(UUID.randomUUID());
        tunnel.setInetType(IPVersion.IPV6.toString());
        
        tunnel.setRemotePhysInetAddr(remotePhysInetAddr);
        tunnel.setRemotePhysInetComPort(remotePhysInetComPort);
        
        tunnel.setLocalPhysInetAddr(localPhysInetAddr);
        tunnel.setLocalPhysInetComPort(localPort);
        
        tunnel.setLocalTunnelInetAddr(localTunnelInetAddr);
        tunnel.setLocalTunnelInetComPort(localPort);
        
        tunnel.setRemoteTunnelInetAddr(remoteTunnelInetAddr);
        
        tunnel.setRemoteEndpointType(remoteEndpointType);
        tunnel.setLocalEndpointType(localEndpointType);
        tunnel.setTunnelInetNet(tunnelInetNet);

        tunnel.setKeepalive(WgConnect.getPersistentKeepalive());
        
        Wg wg = new Wg();
        
        PersistenceTunnel referenceTunnel = WgConnect.getTunnelByLocalPhysInetAddrAndTunnelInetNet(localPhysInetAddr, tunnelInetNet, IPVersion.IPV6);

        if (force || referenceTunnel == null) {
            // Check for an existing WgConnect V6 device that is not in wgConnectTunnels
            String ifName = Utils.getAnyExistingWgConnectIfByPrefix(Constants.getTunnelInterfacePrefix(IPVersion.IPV6));
            if (WgConnect.getTunnelByLocalIfName(ifName) != null ||
                Utils.getWgConnectIfByPrefixAndEndpointAddr(Constants.getTunnelInterfacePrefix(IPVersion.IPV6), remotePhysInetAddr) != null) {
                force = true;
            }
            
            if (!force && ifName != null) {
                tunnel.setLocalInterfaceName(ifName);

                tunnel.setLocalTunnelInetAddr(wg.getDeviceInetAddr(ifName, IPVersion.IPV6));
                tunnel.setLocalTunnelInetComPort(localPort);
                tunnel.setLocalTunnelInetSockAddr(new InetSocketAddress(tunnel.getLocalTunnelInetAddr(),
                    (int) tunnel.getLocalTunnelInetComPort()));

                tunnel.setLocalPrivateKey(wg.getInterfacePrivateKey(ifName));
                tunnel.setLocalPublicKey(wg.getInterfacePublicKey(ifName));
                tunnel.setLocalPreSharedKey(wg.getInterfacePresharedKey(ifName));

                // Get the listen port for the new link
                long listenPort = wg.getInterfaceListenPort(tunnel.getLocalInterfaceName());
                if (listenPort > 0) {
                    tunnel.setLocalPhysInetListenPort(listenPort);
                } else {
                    log.error("Unable to obtain the listen-port for WgConnect device " + tunnel.getLocalInterfaceName());
                    return null;
                }
            } else {
                // Generate the v6 interface name
                ifName = Utils.getNextAvailableNetIfName(Constants.getTunnelInterfacePrefix(IPVersion.IPV6), -1);
                tunnel.setLocalInterfaceName(ifName);

                if (!wg.generateKeys()) {
                    log.error("Unable to generate WgConnect cryptographic keys");
                    return null;
                }

                tunnel.setLocalPublicKey(wg.getPublicKey());
                tunnel.setLocalPrivateKey(wg.getPrivateKey());

                if (tunnel.getLocalPrivateKey() == null || tunnel.getLocalPublicKey() == null) {
                    log.error("Unable to generate or retrieve WgConnect crytographic keys");
                    return null;
                }

                // Add, configure, and bring up the WgConnect network link device
                int exitCode = wg.addDevice(tunnel.getLocalInterfaceName());
                if (exitCode == Wg.getCommandFailureCode()) {
                    log.error("Unable to add the WgConnect device " + tunnel.getLocalInterfaceName());
                    return null;
                }

                exitCode = wg.setDeviceInetAddr(tunnel.getLocalInterfaceName(),
                    tunnel.getLocalTunnelInetAddr(), Integer.toString(Constants.V6_SUBNET_MASK_64));
                if (exitCode == Wg.getCommandFailureCode()) {
                    log.error("Unable to set the inet address for WgConnect device " + tunnel.getLocalInterfaceName());
                    return null;
                }

                exitCode = wg.setInterfacePrivateKey(tunnel.getLocalInterfaceName(), tunnel.getLocalPrivateKey());
                if (exitCode == Wg.getCommandFailureCode()) {
                    log.error("Unable to set private key for WgConnect device " + tunnel.getLocalInterfaceName());
                    return null;
                }

                wg.setDeviceState(tunnel.getLocalInterfaceName(),
                    InterfaceDeviceManager.InterfaceDeviceState.UP);
                if (wg.getCommandExitCode() == Wg.getCommandFailureCode()) {
                    log.error("Unable to set the link state for WgConnect device " + tunnel.getLocalInterfaceName());
                    return null;
                }

                // Get the listen port for the new link
                long listenPort = wg.getInterfaceListenPort(tunnel.getLocalInterfaceName());
                if (listenPort > 0) {
                    tunnel.setLocalPhysInetListenPort(listenPort);
                } else {
                    log.error("Unable to obtain the listen-port for WgConnect device " + tunnel.getLocalInterfaceName());
                    return null;
                }

                // Add the tunnel to the datachannels
                tunnel.setLocalTunnelInetSockAddr(new InetSocketAddress(tunnel.getLocalTunnelInetAddr(),
                    (int) tunnel.getLocalTunnelInetComPort()));
                addDatagramChannel(tunnel.getLocalTunnelInetSockAddr());
            }
        } else {
            tunnel.setLocalInterfaceName(referenceTunnel.getLocalInterfaceName());

            tunnel.setLocalTunnelInetAddr(referenceTunnel.getLocalTunnelInetAddr());
            tunnel.setLocalTunnelInetComPort(referenceTunnel.getLocalTunnelInetComPort());
            tunnel.setLocalTunnelInetSockAddr(referenceTunnel.getLocalTunnelInetSockAddr());

            tunnel.setLocalPublicKey(referenceTunnel.getLocalPublicKey());
            tunnel.setLocalPrivateKey(referenceTunnel.getLocalPrivateKey());
            tunnel.setLocalPreSharedKey(referenceTunnel.getLocalPreSharedKey());
            
            tunnel.setLocalPhysInetListenPort(referenceTunnel.getLocalPhysInetListenPort());
            
            if (!StringUtils.equals(referenceTunnel.getTunnelInetNet(), v6ServerMachine.getTunnelInetNet())) {
                v6ServerMachine.configureLocalTunnelAddr(referenceTunnel.getTunnelInetNet());
            }
        }
        
        if (tunnel.getLocalTunnelInetAddr() != null && tunnel.getRemoteTunnelInetAddr() != null) {
            tunnel.setTimestamp(Timestamp.from(Instant.now())); 
            connectConfig.getDatabaseMgr().insertEntity(tunnel);
            WgConnect.addTunnel(tunnel);
        } else {
            log.error("No local or remote tunnel address is available");
            throw new ConfigException();
        }
        
        return tunnel;
    }
    
    public InetSocketAddress getInetSocketAddressFromChannels(InetAddress inetAddr) {
        return getInetSocketAddressFromChannels(inetAddr.getHostAddress());
    }

    public InetSocketAddress getInetSocketAddressFromChannels(String inetAddrStr) {
        DatagramChannel channel = getDatagramChannelByInetAddr(inetAddrStr);

        return channel != null ? channel.getLocalAddress() : null;
    }

    public DatagramChannel getDatagramChannelByInetAddr(String inetAddrStr) {
        String addrKey = new IPAddressString(inetAddrStr).getAddress().getHostSection().toNormalizedString();
        
        return channels.get(addrKey);
    }
    
    public DatagramChannel addDatagramChannel(InetSocketAddress localSockAddr) {
        DatagramChannel channel = null;

        DatagramChannel c = getDatagramChannelByInetAddr(localSockAddr.getAddress().getHostAddress());
        if (c != null && c.getLocalAddress().getPort() == localSockAddr.getPort()) {
            channel = c;
        }

        if (channel == null) {
            ChannelPipeline pipeline = Channels.pipeline();
            pipeline.addLast("logger", new LoggingHandler());
            pipeline.addLast("encoder", new V6ChannelEncoder());
            pipeline.addLast("decoder", new V6ChannelDecoder(localSockAddr, false));
            pipeline.addLast("executor", new ExecutionHandler(new OrderedMemoryAwareThreadPoolExecutor(16, 1048576, 1048576)));
            pipeline.addLast("handler", new ChannelHandler());

            channel = factory.newChannel(pipeline);
            channel.getConfig().setReceiveBufferSize(Constants.DEFAULT_RECEIVE_BUFFER_SIZE);
            channel.getConfig().setSendBufferSize(Constants.DEFAULT_SEND_BUFFER_SIZE);
            ChannelFuture future = channel.bind(localSockAddr);
            future.awaitUninterruptibly();
            
            String addrKey = new IPAddressString(localSockAddr.getAddress().getHostAddress())
                .getAddress().getHostSection().toNormalizedString();
            channels.put(addrKey, channel);
        }

        return channel;
    }
    
    private V6Message buildSolicitMessage(ClientMachine clientMachine) {
        V6Message msg = new V6Message(clientMachine.getLocalPhysInetSockAddr(), clientMachine.getRemotePhysInetSockAddr());
        msg.setTransactionId(clientMachine.getMachineId());
        
        msg.setMessageType(Constants.V6_MESSAGE_TYPE_SOLICIT);
        msg.setMessageSender(Constants.V6_MESSAGE_SENDER_CLIENT);

        msg.putOption(new GenericIdOption(Integer.toString(clientMachine.getMachineId()), false));
        msg.putOption(new SpecificInfoOption(StringUtils.SPACE, false));
        msg.putOption(new RemotePhysInetAddrOption(clientMachine.getLocalPhysInetAddr(), false));
        msg.putOption(new RemotePhysInetComPortOption(clientMachine.getLocalPhysInetSockAddr().getPort(), false));
        msg.putOption(new RemoteTunnelInetAddrOption(clientMachine.getLocalTunnelInetAddr(), false));
        msg.putOption(new TunnelNetworkOption(clientMachine.getTunnelInetNet(), false));
        
        return msg;
    }

    private MessageInfo buildRequestMessage(ClientMachine clientMachine, V6Message advertiseMsg) {
        MessageInfo info = null;
        
        GenericIdOption localIdOption = (GenericIdOption) advertiseMsg.getOption(Constants.OPTION_GENERIC_ID);
        TunnelIdOption tunnelIdOption = (TunnelIdOption) advertiseMsg.getOption(Constants.OPTION_TUNNEL_ID);
        
        RemoteWgPublicKeyOption remoteWgPublicKeyOption = (RemoteWgPublicKeyOption) advertiseMsg.getOption(Constants.OPTION_REMOTE_WG_PUBLIC_KEY);
        
        RemotePhysInetAddrOption remotePhysInetAddrOption = (RemotePhysInetAddrOption) advertiseMsg.getOption(Constants.OPTION_REMOTE_PHYS_INET_ADDR);
        RemotePhysInetComPortOption remotePhysInetComPortOption = (RemotePhysInetComPortOption) advertiseMsg.getOption(Constants.OPTION_REMOTE_PHYS_INET_COM_PORT);
        
        RemotePhysInetListenPortOption remotePhysInetListenPortOption = (RemotePhysInetListenPortOption) advertiseMsg.getOption(Constants.OPTION_REMOTE_PHYS_INET_LISTEN_PORT);
        
        RemoteTunnelInetAddrOption remoteTunnelInetAddrOption = (RemoteTunnelInetAddrOption) advertiseMsg.getOption(Constants.OPTION_REMOTE_TUNNEL_INET_ADDR);
        RemoteTunnelInetComPortOption remoteTunnelInetComPortOption = (RemoteTunnelInetComPortOption) advertiseMsg.getOption(Constants.OPTION_REMOTE_TUNNEL_INET_COM_PORT);
        
        LocalTunnelInetAddrOption localTunnelInetAddrOption = (LocalTunnelInetAddrOption) advertiseMsg.getOption(Constants.OPTION_LOCAL_TUNNEL_INET_ADDR);
        InterfaceNameOption remoteInterfaceNameOption = (InterfaceNameOption) advertiseMsg.getOption(Constants.OPTION_INTERFACE_NAME);
        
        TunnelNetworkOption tunnelNetworkOption = (TunnelNetworkOption) advertiseMsg.getOption(Constants.OPTION_TUNNEL_NETWORK);
        
        GenericResponseOption solicitResponseOption = (GenericResponseOption) advertiseMsg.getOption(Constants.OPTION_GENERIC_RESPONSE);
        
        try {
            if ((localIdOption != null && Integer.parseInt(localIdOption.getString()) == clientMachine.getMachineId()) && tunnelIdOption != null &&
                remoteWgPublicKeyOption != null && remotePhysInetAddrOption != null && remotePhysInetComPortOption != null &&
                remotePhysInetListenPortOption != null && remoteTunnelInetAddrOption != null && remoteTunnelInetComPortOption != null &&
                remoteInterfaceNameOption != null && tunnelNetworkOption != null && solicitResponseOption != null) {

                V6Message msg = new V6Message(clientMachine.getLocalPhysInetSockAddr(), clientMachine.getRemotePhysInetSockAddr());
                msg.setTransactionId(advertiseMsg.getTransactionId());
                
                // Check for an existing tunnel with the offered remote public key
                PersistenceTunnel tunnel = null;
                if (Utils.getWgConnectIfByPrefixAndRemotePublicKey(Constants.getTunnelInterfacePrefix(IPVersion.IPV6),
                    remoteWgPublicKeyOption.getString()) != null) {
                    
                    msg.setMessageType(Constants.V6_MESSAGE_TYPE_REQUEST);
                    msg.setMessageSender(Constants.V6_MESSAGE_SENDER_CLIENT);

                    msg.putOption(localIdOption);
                    msg.putOption(new TunnelIdOption(tunnelIdOption.getString(), false));
                    msg.putOption(new GenericResponseOption(Constants.RESPONSE_DECLINE_REDUNDANT_PUBLIC_KEY, false));
                    
                    msg.putOption(new RemotePhysInetListenPortOption(0, false));
                    msg.putOption(new RemoteTunnelInetComPortOption(0, false));
                    msg.putOption(new RemoteWgPublicKeyOption(StringUtils.SPACE, false));
                    msg.putOption(new InterfaceNameOption(StringUtils.SPACE, false));
                    
                    Wg wg = new Wg();
                    clientMachine.generateNextTunnelNet();
                    String newTunnelNetAddr = StringUtils.substringBefore(clientMachine.getLocalTunnelInetAddr(),
                        IPv6Address.PREFIX_LEN_SEPARATOR);
                    while (WgConnect.isLocalV6Addr(newTunnelNetAddr)) {
                        NetworkInterface netIf = WgConnect.getV6NetIfByIpAddr(newTunnelNetAddr);
                        if (netIf != null) {
                            for (String endpoint : wg.getInterfaceEndpointsAsList(netIf.getDisplayName())) {
                                if (StringUtils.equals(endpoint, newTunnelNetAddr)) {
                                    clientMachine.generateNextTunnelNet();
                                    newTunnelNetAddr = StringUtils.substringBefore(clientMachine.getLocalTunnelInetAddr(),
                                        IPv6Address.PREFIX_LEN_SEPARATOR);
                                }
                            }
                        }
                        clientMachine.generateNextTunnelNet();
                        newTunnelNetAddr = StringUtils.substringBefore(clientMachine.getLocalTunnelInetAddr(),
                            IPv6Address.PREFIX_LEN_SEPARATOR);
                    }
                    
                    msg.putOption(new TunnelNetworkOption(clientMachine.getTunnelInetNet(), true));
                    
                } else {
                    // Check the compatibility of the remoteTunnelNetwork and the localTunnelNetwork
                    if (!StringUtils.equals(clientMachine.getLocalTunnelInetAddr(), localTunnelInetAddrOption.getIpAddress())) {
                        log.info("The offerred local tunnel address {} differs from the current local tunnel address {}",
                            localTunnelInetAddrOption.getIpAddress(), clientMachine.getLocalTunnelInetAddr());

                        clientMachine.configureLocalTunnelAddr(tunnelNetworkOption.getString());
                    } else {
                        log.info("From {}: the offered local tunnel address {} is the same as the current local tunnel address {}",
                            clientMachine.getRemotePhysInetAddr(), localTunnelInetAddrOption.getIpAddress(), clientMachine.getLocalTunnelInetAddr());
                    }

                    boolean force = false;
                    switch ((int)solicitResponseOption.getUnsignedInt()) {
                        case Constants.RESPONSE_DECLINE_REDUNDANT_PUBLIC_KEY:
                            force = true;
                            break;
                        
                        default:
                            break;
                    }
                    
                    tunnel = createTunnelAsClient(Constants.TUNNEL_ENDPOINT_TYPE_SERVER,
                        tunnelIdOption.getString(), clientMachine.getMachineId(),
                        remotePhysInetAddrOption.getIpAddress(), clientMachine.getLocalPhysInetAddr(),
                        remoteTunnelInetAddrOption.getIpAddress(), clientMachine.getLocalTunnelInetAddr(),
                        remoteWgPublicKeyOption.getString(), remotePhysInetListenPortOption.getUnsignedInt(),
                        remoteTunnelInetComPortOption.getUnsignedInt(), clientMachine.getTunnelInetNet(),
                        remoteInterfaceNameOption.getString(), force);

                    if (tunnel != null) {
                        clientMachine.setPersistenceTunnel(tunnel);
                        
                        msg.setMessageType(Constants.V6_MESSAGE_TYPE_REQUEST);
                        msg.setMessageSender(Constants.V6_MESSAGE_SENDER_CLIENT);

                        msg.putOption(localIdOption);
                        msg.putOption(new TunnelIdOption(tunnel.getId().toString(), false));
                        msg.putOption(new GenericResponseOption(Constants.RESPONSE_ACCEPT, false));
                        
                        msg.putOption(new RemotePhysInetListenPortOption(tunnel.getLocalPhysInetListenPort(), false));
                        msg.putOption(new RemoteTunnelInetComPortOption(tunnel.getLocalTunnelInetComPort(), false));
                        msg.putOption(new RemoteWgPublicKeyOption(tunnel.getLocalPublicKey(), false));
                        msg.putOption(new InterfaceNameOption(tunnel.getLocalInterfaceName(), false));
                        msg.putOption(new TunnelNetworkOption(clientMachine.getTunnelInetNet(), false));
                        
                        tunnel.setState(Constants.V6_TUNNEL_STATUS_REQUEST);
                        Gui.refreshTunnelRowColumns(tunnel, Gui.COLUMN_INDEX_STATUS);
                    } else {
                        log.info("Unable to create a V6 tunnel");
                    }
                }

                info = new MessageInfo(tunnel, msg);
            } else {
                log.info("Ignoring the Advertisement message: A required option was not sent");
            }
        } catch (NumberFormatException | SocketException ex) {
            log.info(ex.getMessage());
        }
        
        return info;
    }

    private MessageInfo buildTunnelPingMessage(ClientMachine clientMachine, V6Message requestReplyMsg) {
        MessageInfo info = null;

        try {
            TunnelIdOption tunnelIdOption = (TunnelIdOption) requestReplyMsg.getOption(Constants.OPTION_TUNNEL_ID);

            if (tunnelIdOption != null) {
                PersistenceTunnel tunnel = WgConnect.getTunnelByTunnelId(tunnelIdOption.getString());
                if (tunnel != null) {
                    tunnel.setState(Constants.TUNNEL_STATUS_TUNNEL_PING);
                    Gui.refreshTunnelRowColumns(tunnel, Gui.COLUMN_INDEX_STATUS);

                    V6Message msg = new V6Message(tunnel.getLocalTunnelInetSockAddr(),
                        new InetSocketAddress(tunnel.getRemoteTunnelInetAddr(), (int) tunnel.getRemoteTunnelInetComPort()));
                    msg.setTransactionId(requestReplyMsg.getTransactionId());

                    msg.setMessageType(Constants.V6_MESSAGE_TYPE_TUNNEL_PING);
                    msg.setMessageSender(Constants.V6_MESSAGE_SENDER_CLIENT);

                    msg.putOption(tunnelIdOption);
                    msg.putOption(new PingInetAddrOption(tunnel.getLocalTunnelInetAddr(), false));
                    msg.putOption(new PingInetPortOption(tunnel.getLocalTunnelInetComPort(), false));

                    info = new MessageInfo(tunnel, msg);
                } else {
                    log.info("Ignoring the request reply message: Could not find the associated tunnel");
                }
            } else {
                log.info("Ignoring the request reply message: A required option was not sent");
            }
        } catch (Exception ex) {
            log.info("Ignoring the request reply message: Exception = {}" + ex);
        }
        
        return info;
    }
    
    private PersistenceTunnel checkPingReplyMessage(ClientMachine clientMachine, V6Message tunnelPingReplyMsg) {
        PersistenceTunnel tunnel = null;

        if (tunnelPingReplyMsg != null) {
            TunnelIdOption tunnelIdOption = (TunnelIdOption) tunnelPingReplyMsg.getOption(Constants.OPTION_TUNNEL_ID);
            if (tunnelIdOption != null) {
                tunnel = WgConnect.getTunnelByTunnelId(tunnelIdOption.getString());
                
                if (tunnel != null) {
                    tunnel.setState(Constants.TUNNEL_STATUS_UP);
                    WgConnect.printTunnelCompleteMessage(tunnel);
                    Gui.refreshTunnelRowColumns(tunnel, Gui.COLUMN_INDEX_STATUS);
                }
            }
        }
        
        return tunnel;
    }
    
    
    private List<MessageInfo> buildPeerTunnelsInfoRequestReplyMessage(Machine machine, V6Message requestMsg) {
        List <MessageInfo> infoMsgs = new ArrayList<>();
        
        LocalTunnelInetAddrOption localTunnelInetAddrOption =
            (LocalTunnelInetAddrOption) requestMsg.getOption(Constants.OPTION_LOCAL_TUNNEL_INET_ADDR);
        RemoteTunnelInetAddrOption remoteTunnelInetAddrOption =
            (RemoteTunnelInetAddrOption) requestMsg.getOption(Constants.OPTION_REMOTE_TUNNEL_INET_ADDR);
        
        PersistenceTunnel tunnel = WgConnect.getTunnelByLocalAndRemoteTunnelInetAddrs(
            localTunnelInetAddrOption.getIpAddress(), remoteTunnelInetAddrOption.getIpAddress());

        if (tunnel != null) {
            for (PersistenceTunnel t : WgConnect.getV6Tunnels()) {

                V6Message msg = new V6Message(tunnel.getLocalTunnelInetSockAddr(),
                    new InetSocketAddress(tunnel.getRemoteTunnelInetAddr(), (int) tunnel.getRemoteTunnelInetComPort()));
                msg.setTransactionId(requestMsg.getTransactionId());
                msg.setMessageType(Constants.V6_MESSAGE_TYPE_REPLY);

                msg.putOption(new MsgTypeOption((short) Constants.V6_MESSAGE_TYPE_INFO_REQUEST_REPLY, false));
                
                msg.putOption(new TunnelIdOption(t.getId().toString(), false));
                msg.putOption(new TunnelStatusOption(t.getState(), false));
                
                msg.putOption(new RemoteEndpointTypeOption(t.getRemoteEndpointType(), false));
                msg.putOption(new LocalEndpointTypeOption(t.getLocalEndpointType(), false));

                msg.putOption(new RemotePhysInetAddrOption(t.getRemotePhysInetAddr(), false));
                msg.putOption(new RemotePhysInetComPortOption(t.getRemotePhysInetComPort(), false));
                
                msg.putOption(new LocalPhysInetAddrOption(t.getLocalPhysInetAddr(), false));
                msg.putOption(new LocalPhysInetComPortOption(t.getLocalPhysInetComPort(), false));
                
                msg.putOption(new RemoteTunnelInetAddrOption(t.getRemoteTunnelInetAddr(), false));
                msg.putOption(new LocalTunnelInetAddrOption(t.getLocalTunnelInetAddr(), false));

                msg.putOption(new RemoteWgPublicKeyOption(t.getRemotePublicKey(), false));
                msg.putOption(new RemotePhysInetListenPortOption(t.getRemotePhysInetListenPort(), false));
                
                msg.putOption(new LocalWgPublicKeyOption(t.getLocalPublicKey(), false));
                msg.putOption(new LocalPhysInetListenPortOption(t.getLocalPhysInetListenPort(), false));

                infoMsgs.add(new MessageInfo(tunnel, msg));
            }
        }
        
        return infoMsgs;
    }
}
