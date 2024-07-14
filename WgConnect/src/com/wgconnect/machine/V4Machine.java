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
import com.wgconnect.machine.processor.V4RequestProcessor;
import com.wgconnect.config.ConfigException;
import com.wgconnect.config.ConnectConfig;
import com.wgconnect.core.message.V4Message;
import com.wgconnect.core.option.machine.*;
import com.wgconnect.core.util.Constants;
import com.wgconnect.core.util.WgConnectLogger;
import com.wgconnect.core.util.Utils;
import com.wgconnect.db.persistence.PersistenceTunnel;
import com.wgconnect.gui.Gui;
import com.wgconnect.machine.processor.V4DiscoverProcessor;
import com.wgconnect.machine.processor.V4PingProcessor;

import com.wgtools.Wg;

import inet.ipaddr.AddressStringException;
import inet.ipaddr.IPAddress.IPVersion;
import inet.ipaddr.IPAddressString;
import inet.ipaddr.ipv4.IPv4Address;
import inet.ipaddr.ipv4.IPv4AddressSegment;

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
import com.wgtools.DeviceManagerInterface;

/**
 * V4Machine
 * 
 * A V4 machine that sends and receives configuration messages to and from other V4 machines.
 * 
 * @author JagornetDhcp version: A. Gregory Rabil
 * @author WgConnect version: wgconnect@proton.me
 */
public class V4Machine implements Runnable {

    private static final WgConnectLogger log = WgConnectLogger.getLogger(V4Machine.class);

    private static final String NAME = "V4Machine";
        
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
    protected int localPort = Constants.V4_PORT; 

    protected Map<String, DatagramChannel> channels = new LinkedHashMap<>();

    protected List<InetAddress> remotePhysInetAddrs = new ArrayList<>();
    protected int remotePort = Constants.V4_PORT;

    protected String tunnelNetwork = null;
    
    LinkedBlockingQueue<Integer> doneQueue = new LinkedBlockingQueue<>();
    
    private static V4Machine v4Machine;
    public static synchronized V4Machine getInstance() {
        return v4Machine;
    }
    
    /**
     * Instantiates new V4 connections
     *
     * @param localPhysInetAddrs
     * @param localPort
     * @param remotePhysInetAddrs
     * @param remotePort
     * @param tunnelNetwork
     */
    public V4Machine(List<InetAddress> localPhysInetAddrs, int localPort, List<InetAddress> remotePhysInetAddrs, int remotePort,
        String tunnelNetwork) {
        v4Machine = this;
        
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
     * Start receiving and sending V4 configuration messages
     */
    @Override
    public void run() {
        if (localPhysInetAddrs.isEmpty()) {
            log.error("A local internet address was not specified");
            return;            
        }
        
        if (remotePhysInetAddrs.isEmpty()) {
            log.error("A remote internet address was not specified");
            return;
        }
        
        log.info("Starting V4 machine on {}-{} at: {}", localPhysInetAddrs, localPort, LocalDateTime.now());

        factory = new NioDatagramChannelFactory(Executors.newCachedThreadPool(), InternetProtocolFamily.IPv4);
        
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
                    Constants.V4_MESSAGE_TYPE_DISCOVER);
                
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
    
    public void processMulticastDiscoverMessage(DatagramPacket packet, byte[] buffer, int len) {
        try {
            for (InetAddress addr : localPhysInetAddrs) {
                if (WgConnect.isLocalV4Addr(packet.getAddress().getHostAddress())) {
                    continue;
                }
                
                InetSocketAddress serverInetSockAddr = new InetSocketAddress(addr, localPort);
                V4Message msg = V4Message.decode(ByteBuffer.wrap(buffer, 0, len),
                    serverInetSockAddr, new InetSocketAddress(packet.getAddress().getHostAddress(), packet.getPort()));
                
                if (WgConnect.isLocalV4Addr(msg.getClientAddr().getHostAddress())) {
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
                
                msg.setServerAddr(serverInetSockAddr.getAddress());
                msg.setServerPort(serverInetSockAddr.getPort());
                
                if (WgConnect.getTunnelByLocalAndRemotePhysInetAddr(msg.getServerAddr().getHostAddress(),
                    msg.getClientAddr().getHostAddress()) != null) {
                    continue;
                }

                InetSocketAddress remoteSockInetAddr = new InetSocketAddress(msg.getClientAddr(), (int) msg.getClientPort());
                msg.setRemoteAddress(remoteSockInetAddr);
                
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
        IPAddressString tunnelNetIPAddrStr = new IPAddressString(StringUtils.substringBefore(tunnelNet, IPv4Address.PREFIX_LEN_SEPARATOR));
        IPv4AddressSegment[] tunnelNetSegments = tunnelNetIPAddrStr.getAddress().toIPv4().getSegments();
        
        int newPrefixLen = 0;
        for (int i = 0; i < (tunnelNetSegments.length - 1); i++) {
            IPv4AddressSegment seg = tunnelNetSegments[i];
            if ((seg.getSegmentValue() + 1) < IPv4Address.MAX_VALUE_PER_SEGMENT) {
                tunnelNetSegments[i] = new IPv4AddressSegment(seg.getSegmentValue() + 1);
                newPrefixLen += IPv4Address.BITS_PER_SEGMENT;
                break;
            }
        }
                
        return new IPv4Address(tunnelNetSegments).toInetAddress().getHostAddress() + IPv4Address.PREFIX_LEN_SEPARATOR + newPrefixLen;
    }
    
    public IPv4Address applyTunnelNet(String tunnelNet, String inetAddr) {
        int tunnelNetPrefixLen;
        IPAddressString tunnelNetIPAddrStr = new IPAddressString(tunnelNet);
        IPv4AddressSegment[] tunnelNetSegments = tunnelNetIPAddrStr.getAddress().toIPv4().getSegments();
        
        if (tunnelNetIPAddrStr.isPrefixed()) {
            tunnelNetPrefixLen = tunnelNetIPAddrStr.getNetworkPrefixLength();
            if (tunnelNetPrefixLen < IPv4Address.BITS_PER_SEGMENT || tunnelNetPrefixLen > Constants.V4_MAX_TUNNEL_NETWORK_PREFIX_LEN) {
                tunnelNetPrefixLen = IPv4Address.BITS_PER_SEGMENT;
            }
        } else {
            tunnelNetIPAddrStr = new IPAddressString(tunnelNet + IPv4Address.PREFIX_LEN_SEPARATOR + IPv4Address.BITS_PER_SEGMENT);
            tunnelNetPrefixLen = tunnelNetIPAddrStr.getNetworkPrefixLength();
        }
        
        IPAddressString localInetIPAddrStr = new IPAddressString(StringUtils.substringBefore(inetAddr, IPv4Address.PREFIX_LEN_SEPARATOR) +
            IPv4Address.PREFIX_LEN_SEPARATOR + tunnelNetPrefixLen);
        
        IPv4AddressSegment[] tunnelInetAddrSegments = localInetIPAddrStr.getAddress().toIPv4().getSegments();
        System.arraycopy(tunnelNetSegments, 0, tunnelInetAddrSegments, 0, tunnelNetPrefixLen / IPv4Address.BITS_PER_SEGMENT);
        IPv4Address localTunnelIPv4Addr = new IPv4Address(new IPv4Address(tunnelInetAddrSegments).getBytes(), Constants.V4_SUBNET_MASK_32);
        
        return localTunnelIPv4Addr;
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
            if (msg instanceof V4Message) {
                V4Message configMsg = (V4Message) msg;
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
        V4Message v4Msg = null;
                
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
        
        public void dispatch(V4Message msg) {
            if (msg != null) {
                queue.add(msg);
            }
        }
        
        public void dispatch(Machine machine) {
            if (machine != null) {
                queue.add(machine);
            }
        }
        
        public boolean machineMatchesV4Msg(Machine machine, V4Message msg) {            
            boolean match = false;
            
            if (machine instanceof ServerMachine) {
                match = machine.getMachineId() == msg.getTransactionId() &&
                    StringUtils.equals(machine.getLocalPhysInetAddr(), msg.getServerAddr().getHostAddress()) &&
                    StringUtils.equals(machine.getRemotePhysInetAddr(), msg.getClientAddr().getHostAddress());
            } else if (machine instanceof ClientMachine) {
                match = machine.getMachineId() == msg.getTransactionId() &&
                    StringUtils.equals(machine.getLocalPhysInetAddr(), msg.getClientAddr().getHostAddress()) &&
                    StringUtils.equals(machine.getRemotePhysInetAddr(), msg.getServerAddr().getHostAddress());
            }
            
            return match;
        }
        
        @Override
        public void run() {
            while (true) {
                try {
                    Object obj = queue.take();
                    if (obj == null) {
                        break;
                    }
                    
                    if (obj instanceof V4Message) {
                        
                        v4Msg = (V4Message) obj;
                        
                        short msgType = v4Msg.getMessageType();
                        short msgSender = v4Msg.getMessageSender();
                        switch (msgSender) {
                            case Constants.V4_MESSAGE_SENDER_CLIENT:
                                ServerMachine sm;
                                if (serverMachine == null || !machineMatchesV4Msg(serverMachine, v4Msg)) {
                                    sm = new ServerMachine((int) v4Msg.getTransactionId(),
                                        new InetSocketAddress(v4Msg.getServerAddr(), (int) v4Msg.getServerPort()),
                                        v4Msg.getRemoteAddress(), tunnelNetwork);
    
                                    machineExecutor.submit(sm);
                                } else {
                                    sm = serverMachine;
                                }
                                
                                switch (msgType) {
                                    case Constants.V4_MESSAGE_TYPE_DISCOVER:
                                        sm.discoverMsgQueue.add(v4Msg);
                                        break;
                                        
                                    case Constants.V4_MESSAGE_TYPE_REQUEST:
                                        sm.requestsReceived.getAndIncrement();
                                        sm.requestMsg = v4Msg;
                                        sm.replySemaphore.release();
                                        break;
                                        
                                    case Constants.V4_MESSAGE_TYPE_TUNNEL_PING:
                                        sm.tunnelPingsReceived.getAndIncrement();
                                        sm.pingMsg = v4Msg;
                                        sm.replySemaphore.release();
                                        break;
                                        
                                    case Constants.V4_MESSAGE_TYPE_INFO_REQUEST:
                                        if (!sm.configInfoMsgQueue.add(v4Msg)) {
                                            log.error("Unable to add the message " + v4Msg + " to the config info message queue");
                                        }
                                        break;
                                        
                                    default:
                                        log.info("Received unhandled client message type: " + v4Msg.getMessageType());
                                        break;
                                }

                                break;
                                
                            case Constants.V4_MESSAGE_SENDER_SERVER:
                                ClientMachine cm;
                                if (clientMachine == null || !machineMatchesV4Msg(clientMachine, v4Msg)) {
                                    cm = new ClientMachine((int) v4Msg.getTransactionId(),
                                        new InetSocketAddress(v4Msg.getClientAddr(), (int) v4Msg.getClientPort()),
                                        v4Msg.getRemoteAddress(), v4Msg.getMessageType());

                                    machineExecutor.submit(cm);
                                } else {
                                    cm = clientMachine;
                                }
                                
                                switch (msgType) {
                                    case Constants.V4_MESSAGE_TYPE_OFFER:
                                        cm.offersReceived.getAndIncrement();
                                        cm.offerMsg = v4Msg;
                                        cm.replySemaphore.release();
                                        break;
                                        
                                    case Constants.V4_MESSAGE_TYPE_ACK:
                                        cm.acksReceived.getAndIncrement();
                                        cm.ackMsg = v4Msg;
                                        cm.replySemaphore.release();
                                        break;
                                        
                                    case Constants.V4_MESSAGE_TYPE_TUNNEL_PING_REPLY:
                                        cm.tunnelPingRepliesReceived.getAndIncrement();
                                        cm.pingReplyMsg = v4Msg;
                                        cm.replySemaphore.release();
                                        break;
                                        
                                    default:
                                        log.info("Received unhandled server message type: " + v4Msg.getMessageType());
                                        break;
                                }
                                
                                break;
                            
                            default:
                                log.error("Received message from unknown sender: " + v4Msg.getClass());
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
        private final V4Message message;
        
        public MessageInfo(PersistenceTunnel tunnel, V4Message message) {
            this.tunnel = tunnel;
            this.message = message;
        }
        
        public PersistenceTunnel getTunnel() {
            return tunnel;
        }
        
        public V4Message getMessage() {
            return message;
        }
    }

    protected abstract class Machine implements Runnable {
        String NAME = StringUtils.EMPTY;

        InetSocketAddress localPhysInetSockAddr = null;
        InetSocketAddress remotePhysInetSockAddr = null;
        
        String tunnelInetNet = null;
        IPv4Address localTunnelIPv4Addr = null;
        
        String remoteTunnelInetAddr = null;
        
        PersistenceTunnel persistenceTunnel = null;

        V4Message v4Msg = null;
        
        int id = 0;
        
    	Semaphore replySemaphore = new Semaphore(1);
        
        boolean retry = false;
        
        int state = 0;
        
        public String getName() {
            return NAME + "-" + localPhysInetSockAddr.getAddress().getHostAddress() + "-" +
                StringUtils.substringAfterLast(remotePhysInetSockAddr.getAddress().getHostAddress(), IPv4Address.SEGMENT_SEPARATOR);
        }

        @Override
        public String toString() {
            return getName();
        }
        
        public int getMachineId() {
            return id;
        }
        
        public V4Machine getV4Machine() {
            return v4Machine;
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
            return localTunnelIPv4Addr.toInetAddress().getHostAddress();
        }
        
        public IPv4Address getLocalTunnelIPv4Addr() {
            return localTunnelIPv4Addr;
        }

        public String generateNextTunnelNet() {
            configureLocalTunnelAddr(v4Machine.generateNextTunnelNet(tunnelInetNet));

            return tunnelInetNet;
        }
        
        public void setPersistenceTunnel(PersistenceTunnel persistenceTunnel) {
            this.persistenceTunnel = persistenceTunnel;
        }
        
        public PersistenceTunnel getPersistenceTunnel() {
            return persistenceTunnel;
        }

        public void configureLocalTunnelAddr(String tunnelNet) {
            this.tunnelInetNet = tunnelNet;
            
            IPAddressString tunnelNetIPAddrStr = new IPAddressString(tunnelNet);
            IPv4AddressSegment[] tunnelNetSegments = tunnelNetIPAddrStr.getAddress().toIPv4().getSegments();

            int tunnelNetPrefixLen;
            if (tunnelNetIPAddrStr.isPrefixed()) {
                tunnelNetPrefixLen = tunnelNetIPAddrStr.getNetworkPrefixLength();
                if (tunnelNetPrefixLen < IPv4Address.BITS_PER_SEGMENT || tunnelNetPrefixLen > Constants.V4_MAX_TUNNEL_NETWORK_PREFIX_LEN) {
                    tunnelNetPrefixLen = IPv4Address.BITS_PER_SEGMENT;
                }
            } else {
                tunnelNetIPAddrStr = new IPAddressString(tunnelNet + IPv4Address.PREFIX_LEN_SEPARATOR + IPv4Address.BITS_PER_SEGMENT);
                tunnelNetPrefixLen = tunnelNetIPAddrStr.getNetworkPrefixLength();
            }
            
            IPAddressString localInetIPAddrStr = new IPAddressString(StringUtils.substringBefore(localPhysInetSockAddr.getAddress().getHostAddress(),
                IPv4Address.PREFIX_LEN_SEPARATOR) + IPv4Address.PREFIX_LEN_SEPARATOR + tunnelNetPrefixLen);
            
            IPv4AddressSegment[] tunnelInetAddrSegments = localInetIPAddrStr.getAddress().toIPv4().getSegments();
            System.arraycopy(tunnelNetSegments, 0, tunnelInetAddrSegments, 0, tunnelNetPrefixLen / IPv4Address.BITS_PER_SEGMENT);
            localTunnelIPv4Addr = new IPv4Address(new IPv4Address(tunnelInetAddrSegments).getBytes(), Constants.V4_SUBNET_MASK_32);
        }
    }
    
    /**
     * The ClientMachine class
     */
    protected class ClientMachine extends Machine implements ChannelFutureListener {
                      
        AtomicInteger discoversSent = new AtomicInteger();
        AtomicInteger offersReceived = new AtomicInteger();
        AtomicInteger requestsSent = new AtomicInteger();
        AtomicInteger acksReceived = new AtomicInteger();
        
        AtomicInteger tunnelPingsSent = new AtomicInteger();
        AtomicInteger tunnelPingRepliesReceived = new AtomicInteger();
        
    	V4Message offerMsg;
    	V4Message ackMsg;
        V4Message pingReplyMsg;
        
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
                case Constants.V4_MESSAGE_TYPE_DISCOVER:
                    discover();
                    break;

                case Constants.V4_MESSAGE_TYPE_OFFER:
                    waitForOffer();
                    break;

                case Constants.V4_MESSAGE_TYPE_REQUEST:
                    request();
                    break;

                case Constants.V4_MESSAGE_TYPE_ACK:
                    waitForAck();
                    break;

                case Constants.V4_MESSAGE_TYPE_TUNNEL_PING:
                    tunnelPing();
                    break;

                case Constants.V4_MESSAGE_TYPE_TUNNEL_PING_REPLY:
                    waitForTunnelPingReply();
                    break;

                default:
                    log.info("Unknown client state: " + state);
                    break;
            }

            clientMachine = null;
        }

        private void discover() {
            v4Msg = buildDiscoverMessage(this);

            if (Utils.isMulticastAddress(remotePhysInetSockAddr.getAddress().getHostAddress())) {
                try {                    
                    DatagramSocket dataSock = new DatagramSocket();

                    byte[] msgBytes = v4Msg.encode().array();
                    dataSock.send(new DatagramPacket(msgBytes, msgBytes.length,
                        remotePhysInetSockAddr.getAddress(), remotePhysInetSockAddr.getPort()));
                } catch (IOException ex) {
                    log.info(ex.getMessage());
                }
                
                return;
            } else {
                DatagramChannel c = channels.get(localPhysInetSockAddr.getAddress().getHostAddress());
                if (c != null) {
                    ChannelFuture future = c.write(v4Msg, remotePhysInetSockAddr);
                    future.addListener(this);
                } else {
                    log.error("Could not find a DatagramChannel for inet address {}", localPhysInetSockAddr.getAddress().getHostAddress());
                }
            }
            
            state = Constants.V4_MESSAGE_TYPE_DISCOVER;
            waitForOffer();
        }

        private void waitForOffer() {
            try {
                if (!replySemaphore.tryAcquire(WAIT_FOR_REPLY_TIME, TimeUnit.SECONDS)) {
                    if (retry) {
                        retry = false;
                        log.info("Discover timeout after {} seconds, retrying...", WAIT_FOR_REPLY_TIME);
                        discover();
                    }
                } else {
                    state = Constants.V4_MESSAGE_TYPE_OFFER;
                    
                    request();
                }
            } catch (InterruptedException ex) {
                log.info(ex.getMessage());
            }
        }

        private void request() {
            if (offerMsg != null) {
                if (Utils.isMulticastAddress(remotePhysInetSockAddr.getAddress().getHostAddress())) {
                    remotePhysInetSockAddr = new InetSocketAddress(offerMsg.getRemoteAddress().getAddress().getHostAddress(),
                        offerMsg.getRemoteAddress().getPort());
                }
                
                MessageInfo info = buildRequestMessage(this, offerMsg);
                if (info != null) {                    
                    v4Msg = info.getMessage();
                    
                    DatagramChannel c = channels.get(localPhysInetSockAddr.getAddress().getHostAddress());
                    if (c != null) {
                        ChannelFuture future = c.write(v4Msg, remotePhysInetSockAddr);
                        future.addListener(this);
                    } else {
                        log.error("Could not find a DatagramChannel for inet address {}", localPhysInetSockAddr.getAddress().getHostAddress());
                    }
                    
                    GenericResponseOption offerResponseOption = (GenericResponseOption) v4Msg.getOption(Constants.OPTION_GENERIC_RESPONSE);
                    switch ((int)offerResponseOption.getUnsignedInt()) {
                        case Constants.RESPONSE_DECLINE_REDUNDANT_PUBLIC_KEY:
                        case Constants.RESPONSE_DECLINE_TUNNEL_NETWORK:
                            waitForOffer();
                            break;
                            
                        case Constants.RESPONSE_ACCEPT:
                        default:
                            state = Constants.V4_MESSAGE_TYPE_REQUEST;
                            waitForAck();
                            break;
                    }
                } else {
                    log.error("Request error");
                }
            } else {
                log.error("Offer error");
            }
        }

        private void waitForAck() {
            try {
                if (!replySemaphore.tryAcquire(WAIT_FOR_REPLY_TIME, TimeUnit.SECONDS)) {
                    if (retry) {
                        retry = false;
                        log.info("Request timeout after {} seconds, retrying...", WAIT_FOR_REPLY_TIME);
                        request();
                    }
                } else {
                    state = Constants.V4_MESSAGE_TYPE_ACK;
                    
                    tunnelPing();
                }
            } catch (InterruptedException ex) {
                log.info(ex.getMessage());
            }
       }

        private void tunnelPing() {
            if (ackMsg != null) {
                MessageInfo info = buildTunnelPingMessage(this, ackMsg);
                if (info != null) {
                    DatagramChannel c = addDatagramChannel(info.getTunnel().getLocalTunnelInetSockAddr());
                    
                    v4Msg = info.getMessage();
                    ChannelFuture future = c.write(v4Msg, v4Msg.getRemoteAddress());
                    future.addListener(this);
                    
                    state = Constants.V4_MESSAGE_TYPE_TUNNEL_PING;
                    
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
                    state = Constants.V4_MESSAGE_TYPE_TUNNEL_PING_REPLY;

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
                switch (v4Msg.getMessageType()) {
                    case Constants.V4_MESSAGE_TYPE_DISCOVER:
                        discoversSent.getAndIncrement();
                        log.info("Successfully sent discover message cnt = {}", discoversSent);
                        break;
                        
                    case Constants.V4_MESSAGE_TYPE_REQUEST:
                        requestsSent.getAndIncrement();
                        log.info("Successfully sent request message to {}, cnt = {}", getRemotePhysInetAddr(), requestsSent);
                        break;
                        
                    case Constants.V4_MESSAGE_TYPE_TUNNEL_PING:
                        tunnelPingsSent.getAndIncrement();
                        log.info("Successfully sent tunnel ping message to {}, cnt = {}", getRemotePhysInetAddr(), tunnelPingsSent);
                        break;

                    default:
                        break;
                }
            } else {
                log.error("Failed to send message id = " + v4Msg.getTransactionId() + ":" + future.getCause().toString());
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
        
        AtomicInteger discoversReceived = new AtomicInteger();
        AtomicInteger offersSent = new AtomicInteger();
        AtomicInteger offerRejectsReceived = new AtomicInteger();
        AtomicInteger requestsReceived = new AtomicInteger();
        AtomicInteger requestAcceptReceived = new AtomicInteger();
        AtomicInteger acksSent = new AtomicInteger();
        AtomicInteger tunnelPingsReceived = new AtomicInteger();
        AtomicInteger tunnelPingRepliesSent = new AtomicInteger();

        LinkedBlockingQueue<V4Message> discoverMsgQueue = new LinkedBlockingQueue<>();
        LinkedBlockingQueue<V4Message> configInfoMsgQueue = new LinkedBlockingQueue<>();

        V4Message discoverMsg;
        V4Message requestMsg;
        V4Message pingMsg;
                
        LinkedBlockingQueue<Integer> msgQueue = new LinkedBlockingQueue<>();
        
        public ServerMachine(int id, InetSocketAddress localInetSockAddr, InetSocketAddress remoteInetSockAddr, String tunnelInetNet) {
            NAME = "ServerMachine";

            this.id = id;
            
            this.localPhysInetSockAddr = localInetSockAddr;
            this.remotePhysInetSockAddr = remoteInetSockAddr;
            this.tunnelInetNet = tunnelInetNet;
            
            configureLocalTunnelAddr(tunnelInetNet);

            replySemaphore.drainPermits();
        }
        
        @Override
        public void run() {
            serverMachine = this;
            
            waitForDiscover();
            
            serverMachine = null;
        }

        private void waitForDiscover () {
            try {
                discoverMsg = discoverMsgQueue.take();
                discoversReceived.getAndIncrement();

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

                if (discoverMsg != null) {
                    offer();
                }
            } catch (InterruptedException ex) {
                log.info(ex.getMessage());
            }
        }
        
        private void offer() {
            state = Constants.V4_MESSAGE_TYPE_OFFER;
            
            v4Msg = new V4DiscoverProcessor(this, discoverMsg, discoverMsg.getRemoteAddress().getAddress()).processMessage();
            
            if (v4Msg != null) {
                remotePhysInetSockAddr = v4Msg.getRemoteAddress();
               
                DatagramChannel c = channels.get(localPhysInetSockAddr.getAddress().getHostAddress());
                if (c != null) {
                    ChannelFuture future = c.write(v4Msg, remotePhysInetSockAddr);
                    future.addListener(this);
                } else {
                    log.error("Could not find a DatagramChannel for inet address {}", localPhysInetSockAddr.getAddress().getHostAddress());
                }

                waitForRequest();
            } else {
                log.error("Error processing discover message");
            }
        }

        private void waitForRequest() {
            try {
                if (!replySemaphore.tryAcquire(WAIT_FOR_REPLY_TIME, TimeUnit.SECONDS)) {
                    if (retry) {
                        retry = false;
                        log.info("Request timeout after {} seconds, retrying...", WAIT_FOR_REPLY_TIME);
                        offer();
                    }
                } else {
                    state = Constants.V4_MESSAGE_TYPE_REQUEST;
                    
                    ack();
                }
            } catch (InterruptedException ex) {
                log.info(ex.getMessage());
            }
        }

        private void ack() {
            v4Msg = new V4RequestProcessor(this, requestMsg, requestMsg.getRemoteAddress().getAddress()).processMessage();
            if (v4Msg != null) {
                DatagramChannel c = channels.get(localPhysInetSockAddr.getAddress().getHostAddress());
                if (c != null) {
                    ChannelFuture future = c.write(v4Msg, remotePhysInetSockAddr);
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
                        state = Constants.V4_MESSAGE_TYPE_ACK;
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
                    state = Constants.V4_MESSAGE_TYPE_TUNNEL_PING;

                    tunnelPingReply();
                }
            } catch (InterruptedException ex) {
                log.info(ex.getMessage());
            }
        }

        private void tunnelPingReply() {
            v4Msg = new V4PingProcessor(this, pingMsg, pingMsg.getRemoteAddress().getAddress()).processMessage();
            if (v4Msg != null) {
                DatagramChannel c = channels.get(pingMsg.getLocalAddress().getAddress().getHostAddress());
                if (c != null) {
                    ChannelFuture future = c.write(v4Msg, pingMsg.getRemoteAddress());
                    future.addListener(this);
                } else {
                    log.error("Could not find a DatagramChannel for inet address {}", localPhysInetSockAddr.getAddress().getHostAddress());
                }

                waitForTunnelPingReplySent();
            } else {
                log.error("Error processing tunnel ping message");
            }
        }
        
        private void waitForTunnelPingReplySent() {
            state = Constants.V4_MESSAGE_TYPE_TUNNEL_PING_REPLY;
        }
        
        private void tunnelsInfoRequestReply(V4Message infoRequestMsg) {
            if (infoRequestMsg != null) {
                List<MessageInfo> infoMsgs = buildPeerTunnelsInfoRequestReplyMessage(this, infoRequestMsg);
                
                if (!infoMsgs.isEmpty()) {
                    for (MessageInfo info : infoMsgs) {
                        state = Constants.V4_MESSAGE_TYPE_INFO_REQUEST;

                        v4Msg = info.getMessage();
                        ChannelFuture future = getDatagramChannelByInetAddr(info.getTunnel().getLocalPhysInetAddr())
                            .write(v4Msg, v4Msg.getRemoteAddress());
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
                switch (v4Msg.getMessageType()) {
                    case Constants.V4_MESSAGE_TYPE_OFFER:
                        offersSent.getAndIncrement();
                        log.info("Successfully sent offer message cnt = {}", offersSent);
                        break;
                    
                    case Constants.V4_MESSAGE_TYPE_ACK:
                        acksSent.getAndIncrement();
                        log.info("Successfully sent ack message cnt = {}", acksSent);
                        break;
                        
                    case Constants.V4_MESSAGE_TYPE_TUNNEL_PING_REPLY:
                        tunnelPingRepliesSent.getAndIncrement();
                        log.info("Successfully sent tunnel ping reply message cnt = {}", tunnelPingRepliesSent);
                        break;
                        
                    default:
                        break;
                }
            } else {
                log.error("Failed to send message id = " + v4Msg.getTransactionId() + ":" + future.getCause().toString());
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
        tunnel.setInetType(IPVersion.IPV4.toString());
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

        tunnel.setRemoteInterfaceName(remoteInterfaceName);
        
        tunnel.setKeepalive(WgConnect.getPersistentKeepalive());
        
        Wg wg = new Wg();
        int exitCode;
        
        PersistenceTunnel referenceTunnel = WgConnect.getTunnelByLocalTunnelInetAddr(localTunnelInetAddr);
        if (force || referenceTunnel == null) {
            // Check for an existing WgConnect V4 device that is not in wgConnectTunnels
            String ifName = Utils.getAnyExistingWgConnectIfByPrefix(Constants.getTunnelInterfacePrefix(IPVersion.IPV4));
            if (WgConnect.getTunnelByLocalIfName(ifName) != null ||
                Utils.getWgConnectIfByPrefixAndEndpointAddr(Constants.getTunnelInterfacePrefix(IPVersion.IPV4), remotePhysInetAddr) != null) {
                force = true;
            }
            
            if (!force && ifName != null) {
                tunnel.setLocalInterfaceName(ifName);

                tunnel.setLocalTunnelInetAddr(wg.getDeviceInetAddr(ifName, IPVersion.IPV4));
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
                // Generate the v4 interface name
                ifName = Utils.getNextAvailableNetIfName(Constants.getTunnelInterfacePrefix(IPVersion.IPV4), -1);
                tunnel.setLocalInterfaceName(ifName);
                tunnel.setLocalTunnelInetComPort(localPort);

                if (!wg.generateKeys()) {
                    log.error("Unable to generate WgConnectV4 cryptographic keys");
                    return null;
                }

                tunnel.setLocalPublicKey(wg.getPublicKey());
                tunnel.setLocalPrivateKey(wg.getPrivateKey());

                if (tunnel.getLocalPrivateKey() == null || tunnel.getLocalPublicKey() == null) {
                    log.error("Unable to generate or retrieve WgConnect V4 crytographic keys");
                    return null;
                }

                // Add, configure, and bring up the WgConnect network link device
                exitCode = wg.addDevice(tunnel.getLocalInterfaceName());
                if (exitCode == Wg.getCommandFailureCode()) {
                    log.error("Unable to add the WgConnect device " + tunnel.getLocalInterfaceName());
                    return null;
                }

                exitCode = wg.setDeviceInetAddr(tunnel.getLocalInterfaceName(), tunnel.getLocalTunnelInetAddr(),
                    Integer.toString(Constants.V4_SUBNET_MASK_24));
                if (exitCode == Wg.getCommandFailureCode()) {
                    log.error("Unable to set the inet address for WgConnect device " + tunnel.getLocalInterfaceName());
                    return null;
                }

                exitCode = wg.setInterfacePrivateKey(tunnel.getLocalInterfaceName(), tunnel.getLocalPrivateKey());
                if (exitCode == Wg.getCommandFailureCode()) {
                    log.error("Unable to set private key for WgConnect device " + tunnel.getLocalInterfaceName());
                    return null;
                }

                wg.setDeviceState(tunnel.getLocalInterfaceName(), DeviceManagerInterface.InterfaceDeviceState.UP);
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
                    Wg.OPTION_ALLOWED_IPS, tunnel.getRemoteTunnelInetAddr() + IPv4Address.PREFIX_LEN_SEPARATOR + Constants.V4_SUBNET_MASK_32,
                    Wg.OPTION_ENDPOINT, tunnel.getRemotePhysInetAddr() + ":" + tunnel.getRemotePhysInetListenPort(),
                    Wg.OPTION_PERSISTENT_KEEPALIVE, Integer.toString(WgConnect.getPersistentKeepalive()));
                if (exitCode == Wg.getCommandFailureCode()) {
                    log.error("Unable to set the peer configuration for the device " + tunnel.getLocalInterfaceName());
                    return null;
                }

                // Add the tunnel to the datachannels
                tunnel.setLocalTunnelInetSockAddr(new InetSocketAddress(tunnel.getLocalTunnelInetAddr(),
                    (int) tunnel.getLocalTunnelInetComPort()));
                addDatagramChannel(tunnel.getLocalTunnelInetSockAddr());
                
                // Add the tunnel to the GUI
                WgConnect.guiAddTunnel(tunnel);
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
                Wg.OPTION_ALLOWED_IPS, tunnel.getRemoteTunnelInetAddr() + IPv4Address.PREFIX_LEN_SEPARATOR + Constants.V4_SUBNET_MASK_32,
                Wg.OPTION_ENDPOINT, tunnel.getRemotePhysInetAddr() + ":" + tunnel.getRemotePhysInetListenPort(),
                Wg.OPTION_PERSISTENT_KEEPALIVE,
                Integer.toString(WgConnect.getPersistentKeepalive())
            );
            if (exitCode == Wg.getCommandFailureCode()) {
                log.error("Unable to set the peer configuration for the device " + tunnel.getLocalInterfaceName());
                return null;
            }

            // Add the tunnel to the datachannels
            tunnel.setLocalTunnelInetSockAddr(new InetSocketAddress(tunnel.getLocalTunnelInetAddr(), (int) tunnel.getLocalTunnelInetComPort()));
            addDatagramChannel(tunnel.getLocalTunnelInetSockAddr());
            
            // Add the tunnel to the GUI
            WgConnect.guiAddTunnel(tunnel);
        }

        WgConnect.addTunnel(tunnel);

        return tunnel;
    }

    public synchronized PersistenceTunnel createTunnelAsServer(ServerMachine v4ServerMachine, int remoteId,
        String localEndpointType, String remoteEndpointType, String localPhysInetAddr, String remotePhysInetAddr,
        long remotePhysInetComPort, String remoteTunnelInetAddr, String localTunnelInetAddr, String tunnelInetNet,
        boolean force) throws Exception {
        
        PersistenceTunnel tunnel = new PersistenceTunnel();
        tunnel.setRemoteId(remoteId);
        tunnel.setId(UUID.randomUUID());
        tunnel.setInetType(IPVersion.IPV4.toString());
        
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
        
        PersistenceTunnel referenceTunnel = WgConnect.getTunnelByLocalPhysInetAddrAndTunnelInetNet(localPhysInetAddr, tunnelInetNet, IPVersion.IPV4);

        if (force || referenceTunnel == null) {
            // Check for an existing WgConnect V4 device that is not in wgConnectTunnels
            String ifName = Utils.getAnyExistingWgConnectIfByPrefix(Constants.getTunnelInterfacePrefix(IPVersion.IPV4));
            if (WgConnect.getTunnelByLocalIfName(ifName) != null ||
                Utils.getWgConnectIfByPrefixAndEndpointAddr(Constants.getTunnelInterfacePrefix(IPVersion.IPV4), remotePhysInetAddr) != null) {
                force = true;
            }
            
            if (!force && ifName != null) {
                tunnel.setLocalInterfaceName(ifName);

                tunnel.setLocalTunnelInetAddr(wg.getDeviceInetAddr(ifName, IPVersion.IPV4));
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
                // Generate the v4 interface name
                ifName = Utils.getNextAvailableNetIfName(Constants.getTunnelInterfacePrefix(IPVersion.IPV4), -1);
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
                    tunnel.getLocalTunnelInetAddr(), Integer.toString(Constants.V4_SUBNET_MASK_24));
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
                    DeviceManagerInterface.InterfaceDeviceState.UP);
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
            
            if (!StringUtils.equals(referenceTunnel.getTunnelInetNet(), v4ServerMachine.getTunnelInetNet())) {
                v4ServerMachine.configureLocalTunnelAddr(referenceTunnel.getTunnelInetNet());
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
            pipeline.addLast("encoder", new V4ChannelEncoder());
            pipeline.addLast("decoder", new V4ChannelDecoder(localSockAddr, false));
            pipeline.addLast("executor", new ExecutionHandler(new OrderedMemoryAwareThreadPoolExecutor(Constants.MAX_CHANNEL_THREADS, 0, 0)));
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

    private V4Message buildDiscoverMessage(ClientMachine clientMachine) {
        V4Message msg = new V4Message(clientMachine.getLocalPhysInetSockAddr(), clientMachine.getRemotePhysInetSockAddr());
        msg.setOp((short) Constants.V4_OP_REQUEST);
        msg.setTransactionId(clientMachine.getMachineId());
        msg.setHtype((short) 1);
        msg.setClientAddr(clientMachine.getLocalPhysInetSockAddr().getAddress());
        msg.setClientPort(clientMachine.getLocalPhysInetSockAddr().getPort());
        msg.setServerAddr(clientMachine.getRemotePhysInetSockAddr().getAddress());
        msg.setServerPort(clientMachine.getRemotePhysInetSockAddr().getPort());
        
        msg.setMessageType((short) Constants.V4_MESSAGE_TYPE_DISCOVER);
        msg.setMessageSender((short) Constants.V4_MESSAGE_SENDER_CLIENT);

        msg.putOption(new GenericIdOption(Integer.toString(clientMachine.getMachineId()), true));
        msg.putOption(new SpecificInfoOption(StringUtils.SPACE, true));
        msg.putOption(new RemotePhysInetAddrOption(clientMachine.getLocalPhysInetAddr(), true));
        msg.putOption(new RemotePhysInetComPortOption(clientMachine.getLocalPhysInetSockAddr().getPort(), true));
        msg.putOption(new RemoteTunnelInetAddrOption(clientMachine.getLocalTunnelInetAddr(), true));
        msg.putOption(new TunnelNetworkOption(clientMachine.getTunnelInetNet(), true));
        
        return msg;
    }

    private MessageInfo buildRequestMessage(ClientMachine clientMachine, V4Message offerMsg) {
        MessageInfo info = null;

        GenericIdOption localIdOption = (GenericIdOption) offerMsg.getOption(Constants.OPTION_GENERIC_ID);
        TunnelIdOption tunnelIdOption = (TunnelIdOption) offerMsg.getOption(Constants.OPTION_TUNNEL_ID);
        
        RemoteWgPublicKeyOption remoteWgPublicKeyOption = (RemoteWgPublicKeyOption) offerMsg.getOption(Constants.OPTION_REMOTE_WG_PUBLIC_KEY);
        
        RemotePhysInetAddrOption remotePhysInetAddrOption = (RemotePhysInetAddrOption) offerMsg.getOption(Constants.OPTION_REMOTE_PHYS_INET_ADDR);
        RemotePhysInetComPortOption remotePhysInetComPortOption = (RemotePhysInetComPortOption) offerMsg.getOption(Constants.OPTION_REMOTE_PHYS_INET_COM_PORT);
        
        RemotePhysInetListenPortOption remotePhysInetListenPortOption = (RemotePhysInetListenPortOption) offerMsg.getOption(Constants.OPTION_REMOTE_PHYS_INET_LISTEN_PORT);
        
        RemoteTunnelInetAddrOption remoteTunnelInetAddrOption = (RemoteTunnelInetAddrOption) offerMsg.getOption(Constants.OPTION_REMOTE_TUNNEL_INET_ADDR);
        RemoteTunnelInetComPortOption remoteTunnelInetComPortOption = (RemoteTunnelInetComPortOption) offerMsg.getOption(Constants.OPTION_REMOTE_TUNNEL_INET_COM_PORT);
        
        LocalTunnelInetAddrOption localTunnelInetAddrOption = (LocalTunnelInetAddrOption) offerMsg.getOption(Constants.OPTION_LOCAL_TUNNEL_INET_ADDR);
        InterfaceNameOption remoteInterfaceNameOption = (InterfaceNameOption) offerMsg.getOption(Constants.OPTION_INTERFACE_NAME);
        
        TunnelNetworkOption tunnelNetworkOption = (TunnelNetworkOption) offerMsg.getOption(Constants.OPTION_TUNNEL_NETWORK);
        
        GenericResponseOption discoverResponseOption = (GenericResponseOption) offerMsg.getOption(Constants.OPTION_GENERIC_RESPONSE);
        
        try {
            if ((localIdOption != null && Integer.parseInt(localIdOption.getString()) == clientMachine.getMachineId()) && tunnelIdOption != null &&
                remoteWgPublicKeyOption != null && remotePhysInetAddrOption != null && remotePhysInetComPortOption != null &&
                remotePhysInetListenPortOption != null && remoteTunnelInetAddrOption != null && remoteTunnelInetComPortOption != null &&
                remoteInterfaceNameOption != null && tunnelNetworkOption != null) {

                V4Message msg = new V4Message(clientMachine.getLocalPhysInetSockAddr(), clientMachine.getRemotePhysInetSockAddr());
                msg.setOp((short) Constants.V4_OP_REQUEST);
                msg.setTransactionId(offerMsg.getTransactionId());
                msg.setHtype((short) 1);
                msg.setClientAddr(offerMsg.getClientAddr());
                msg.setClientPort(offerMsg.getClientPort());
                msg.setServerAddr(offerMsg.getServerAddr());
                msg.setServerPort(offerMsg.getServerPort());
                
                // Check for an existing tunnel with the offered remote public key
                PersistenceTunnel tunnel = null;
                if (Utils.getWgConnectIfByPrefixAndRemotePublicKey(Constants.getTunnelInterfacePrefix(IPVersion.IPV4),
                    remoteWgPublicKeyOption.getString()) != null) {
                    
                    msg.setMessageType((short) Constants.V4_MESSAGE_TYPE_REQUEST);
                    msg.setMessageSender((short) Constants.V4_MESSAGE_SENDER_CLIENT);

                    msg.putOption(localIdOption);
                    msg.putOption(new TunnelIdOption(tunnelIdOption.getString(), true));
                    msg.putOption(new GenericResponseOption(Constants.RESPONSE_DECLINE_REDUNDANT_PUBLIC_KEY, true));

                    msg.putOption(new RemotePhysInetListenPortOption(0, true));
                    msg.putOption(new RemoteTunnelInetComPortOption(0, true));
                    msg.putOption(new RemoteWgPublicKeyOption(StringUtils.SPACE, true));
                    msg.putOption(new InterfaceNameOption(StringUtils.SPACE, true));

                    Wg wg = new Wg();
                    clientMachine.generateNextTunnelNet();
                    String newTunnelNetAddr = StringUtils.substringBefore(clientMachine.getLocalTunnelInetAddr(), IPv4Address.PREFIX_LEN_SEPARATOR);
                    while (WgConnect.isLocalV4Addr(newTunnelNetAddr)) {
                        NetworkInterface netIf = WgConnect.getV4NetIfByIpAddr(newTunnelNetAddr);
                        if (netIf != null) {
                            for (String endpoint : wg.getInterfaceEndpointsAsList(netIf.getDisplayName())) {
                                if (StringUtils.equals(endpoint, newTunnelNetAddr)) {
                                    clientMachine.generateNextTunnelNet();
                                    newTunnelNetAddr = StringUtils.substringBefore(clientMachine.getLocalTunnelInetAddr(),
                                        IPv4Address.PREFIX_LEN_SEPARATOR);
                                }
                            }
                        }
                        clientMachine.generateNextTunnelNet();
                        newTunnelNetAddr = StringUtils.substringBefore(clientMachine.getLocalTunnelInetAddr(),
                            IPv4Address.PREFIX_LEN_SEPARATOR);
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
                    switch ((int)discoverResponseOption.getUnsignedInt()) {
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
                        
                        msg.setMessageType((short) Constants.V4_MESSAGE_TYPE_REQUEST);
                        msg.setMessageSender((short) Constants.V4_MESSAGE_SENDER_CLIENT);

                        msg.putOption(localIdOption);
                        msg.putOption(new TunnelIdOption(tunnel.getId().toString(), true));
                        msg.putOption(new GenericResponseOption(Constants.RESPONSE_ACCEPT, true));

                        msg.putOption(new RemotePhysInetListenPortOption(tunnel.getLocalPhysInetListenPort(), true));
                        msg.putOption(new RemoteTunnelInetComPortOption(tunnel.getLocalTunnelInetComPort(), true));
                        msg.putOption(new RemoteWgPublicKeyOption(tunnel.getLocalPublicKey(), true));
                        msg.putOption(new InterfaceNameOption(tunnel.getLocalInterfaceName(), true));
                        msg.putOption(new TunnelNetworkOption(clientMachine.getTunnelInetNet(), true));
                        
                        tunnel.setState(Constants.V4_TUNNEL_STATUS_REQUEST);
                        WgConnect.guiRefreshTunnelRowColumns(tunnel, Gui.COLUMN_INDEX_STATUS);
                    } else {
                        log.info("Unable to create a V4 tunnel");
                    }
                }

                info = new MessageInfo(tunnel, msg);
            } else {
                log.info("Ignoring the Offer message: A required option was not sent");
            }
        } catch (NumberFormatException | SocketException ex) {
            log.info(ex.getMessage());
        }

        return info;
    }
    
    private MessageInfo buildTunnelPingMessage(ClientMachine clientMachine, V4Message ackMsg) {
        MessageInfo info = null;

        try {
            TunnelIdOption tunnelIdOption = (TunnelIdOption) ackMsg.getOption(Constants.OPTION_TUNNEL_ID);

            if (tunnelIdOption != null) {
                PersistenceTunnel tunnel = WgConnect.getTunnelByTunnelId(tunnelIdOption.getString());
                if (tunnel != null) {
                    tunnel.setState(Constants.TUNNEL_STATUS_TUNNEL_PING);
                    WgConnect.guiRefreshTunnelRowColumns(tunnel, Gui.COLUMN_INDEX_STATUS);

                    V4Message msg = new V4Message(tunnel.getLocalTunnelInetSockAddr(),
                        new InetSocketAddress(tunnel.getRemoteTunnelInetAddr(), (int) tunnel.getRemoteTunnelInetComPort()));
                    msg.setOp((short) Constants.V4_OP_REQUEST);
                    msg.setTransactionId(ackMsg.getTransactionId());
                    msg.setHtype((short) 1);
                    msg.setClientAddr(ackMsg.getClientAddr());
                    msg.setClientPort(ackMsg.getClientPort());
                    msg.setServerAddr(ackMsg.getServerAddr());
                    msg.setServerPort(ackMsg.getServerPort());
                    
                    msg.setMessageType((short) Constants.V4_MESSAGE_TYPE_TUNNEL_PING);
                    msg.setMessageSender((short) Constants.V4_MESSAGE_SENDER_CLIENT);

                    msg.putOption(new TunnelIdOption(tunnel.getId().toString(), true));
                    msg.putOption(new PingInetAddrOption(tunnel.getLocalTunnelInetAddr(), true));
                    msg.putOption(new PingInetPortOption(tunnel.getLocalTunnelInetComPort(), true));

                    info = new MessageInfo(tunnel, msg);
                } else {
                    log.info("Ignoring the ack message: Could not find the associated tunnel");
                }
            } else {
                log.info("Ignoring the ack message: A required option was not sent");
            }
        } catch (Exception ex) {
            log.info("Ignoring the ack message: Exception = {}" + ex);
        }
        
        return info;
    }
    
    private PersistenceTunnel checkPingReplyMessage(ClientMachine clientMachine, V4Message pingReplyMsg) {
        PersistenceTunnel tunnel = null;

        if (pingReplyMsg != null) {
            TunnelIdOption tunnelIdOption = (TunnelIdOption) pingReplyMsg.getOption(Constants.OPTION_TUNNEL_ID);
            if (tunnelIdOption != null) {
                tunnel = WgConnect.getTunnelByTunnelId(tunnelIdOption.getString());
                
                if (tunnel != null) {
                    tunnel.setState(Constants.TUNNEL_STATUS_UP);
                    WgConnect.printTunnelCompleteMessage(tunnel);
                    WgConnect.guiRefreshTunnelRowColumns(tunnel, Gui.COLUMN_INDEX_STATUS);
                }
            }
        }
       
        return tunnel;
    }

    private List<MessageInfo> buildPeerTunnelsInfoRequestReplyMessage(Machine machine, V4Message requestMsg) {
        List <MessageInfo> infoMsgs = new ArrayList<>();
        
        LocalTunnelInetAddrOption localTunnelInetAddrOption =
            (LocalTunnelInetAddrOption) requestMsg.getOption(Constants.OPTION_LOCAL_TUNNEL_INET_ADDR);
        RemoteTunnelInetAddrOption remoteTunnelInetAddrOption =
            (RemoteTunnelInetAddrOption) requestMsg.getOption(Constants.OPTION_REMOTE_TUNNEL_INET_ADDR);
        
        PersistenceTunnel tunnel = WgConnect.getTunnelByLocalAndRemoteTunnelInetAddrs(
            localTunnelInetAddrOption.getIpAddress(), remoteTunnelInetAddrOption.getIpAddress());

        if (tunnel != null) {
            for (PersistenceTunnel t : WgConnect.getV4Tunnels()) {

                V4Message msg = new V4Message(tunnel.getLocalTunnelInetSockAddr(),
                    new InetSocketAddress(tunnel.getRemoteTunnelInetAddr(), (int) tunnel.getRemoteTunnelInetComPort()));
                msg.setOp((short) Constants.V4_OP_REPLY);
                msg.setTransactionId(requestMsg.getTransactionId());
                msg.setHtype((short) 1);
                
                msg.setClientAddr(requestMsg.getClientAddr());
                msg.setClientPort(requestMsg.getClientPort());
                msg.setServerAddr(requestMsg.getServerAddr());
                msg.setServerPort(requestMsg.getServerPort());
                
                msg.setMessageType((short) Constants.V4_MESSAGE_TYPE_INFO_REQUEST_REPLY);
                msg.setMessageSender((short) Constants.V4_MESSAGE_SENDER_SERVER);
                
                msg.putOption(new TunnelIdOption(t.getId().toString(), true));
                msg.putOption(new TunnelStatusOption(t.getState(), true));
                
                msg.putOption(new RemoteEndpointTypeOption(t.getRemoteEndpointType(), true));
                msg.putOption(new LocalEndpointTypeOption(t.getLocalEndpointType(), true));

                msg.putOption(new RemotePhysInetAddrOption(t.getRemotePhysInetAddr(), true));
                msg.putOption(new RemotePhysInetComPortOption(t.getRemotePhysInetComPort(), true));
                
                msg.putOption(new LocalPhysInetAddrOption(t.getLocalPhysInetAddr(), true));
                msg.putOption(new LocalPhysInetComPortOption(t.getLocalPhysInetComPort(), true));
                
                msg.putOption(new RemoteTunnelInetAddrOption(t.getRemoteTunnelInetAddr(), true));
                msg.putOption(new LocalTunnelInetAddrOption(t.getLocalTunnelInetAddr(), true));

                msg.putOption(new RemoteWgPublicKeyOption(t.getRemotePublicKey(), true));
                msg.putOption(new RemotePhysInetListenPortOption(t.getRemotePhysInetListenPort(), true));
                
                msg.putOption(new LocalWgPublicKeyOption(t.getLocalPublicKey(), true));
                msg.putOption(new LocalPhysInetListenPortOption(t.getLocalPhysInetListenPort(), true));

                infoMsgs.add(new MessageInfo(tunnel, msg));
            }
        }
        
        return infoMsgs;
    }
}
