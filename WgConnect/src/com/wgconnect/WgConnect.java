/*
 * Copyright 2024 wgconnect@proton.me All Rights Reserved.
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
package com.wgconnect;

import com.wgconnect.config.ConnectConfig;
import com.wgconnect.config.ConfigException;
import com.wgconnect.config.WgInterfaceInfo;
import com.wgconnect.machine.V4Machine;
import com.wgconnect.machine.V6Machine;
import com.wgconnect.core.Version;
import com.wgconnect.core.util.Constants;
import com.wgconnect.core.util.Utils;
import com.wgconnect.core.util.WgConnectLogger;
import com.wgconnect.db.persistence.PersistenceTunnel;
import com.wgconnect.gui.Gui;

import inet.ipaddr.AddressStringException;
import inet.ipaddr.IPAddress;
import inet.ipaddr.IPAddress.IPVersion;
import inet.ipaddr.IPAddressString;
import inet.ipaddr.IncompatibleAddressException;
import inet.ipaddr.ipv4.IPv4AddressSegment;
import inet.ipaddr.ipv6.IPv6Address;
import inet.ipaddr.ipv6.IPv6AddressSegment;

import java.io.File;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.InterfaceAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javafx.application.Application;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.apache.commons.validator.routines.InetAddressValidator;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Help;
import picocli.CommandLine.Help.ColorScheme;
import picocli.CommandLine.IHelpFactory;
import picocli.CommandLine.Model.ArgSpec;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParseResult;
import picocli.CommandLine.Spec;

/**
 * WgConnect
 * 
 * The main entry point.
 * 
 * @author WgConnect version: wgconnect@proton.me
 */
@Command(name = Constants.APP_NAME,
    separator = " ",
    requiredOptionMarker = '*',
    sortOptions = false,
    headerHeading = "Usage:%n%n",
    synopsisHeading = "%n",
    descriptionHeading = "%nDescription:%n%n",
    parameterListHeading = "%nParameters:%n",
    optionListHeading = "%nOptions:%n",
    header = Constants.APP_NAME,
    description = "Java application to connect Wireguard tunnels between hosts")
public class WgConnect implements Runnable {
    
    private static final WgConnectLogger log = WgConnectLogger.getLogger(WgConnect.class);

    public static String WG_CONNECT_MACHINES = "Machines";
        
    protected static ConnectConfig config;
    
    protected ExecutorService executorService;
    protected ScheduledExecutorService scheduledExecutorService;

    protected static boolean databaseEnabled = false;
    protected static String databaseFilename = Constants.DATABASE_FILENAME;
    protected static String databaseUsername = Constants.DATABASE_USERNAME;
    protected static String databasePassword = Constants.DATABASE_PASSWORD;
        
    private static final List<McastReceiver> v4McastReceivers = new ArrayList<>();
    private static final List<McastReceiver> v6McastReceivers = new ArrayList<>();
    
    private static V4Machine v4Machine;
    private static V6Machine v6Machine;
    
    private MulticastSocket v4McastSock;
    private MulticastSocket v6McastSock;
    
    private static final int TUNNELS_REFRESH_RATE = 30;
    private static List<PersistenceTunnel> wgConnectTunnels;
    private static List<RefreshTunnelsHandler> wgConnectTunnelsHandlers;
    
    private static final String V4_OPTION = "-4";
    private static final String V6_OPTION = "-6";
    
    private static final String[] DEFAULT_OPTIONS = { "-4e", "-6e" };
    
    private static @Spec CommandSpec spec;

    @Option(names = {"-4e", "--4enable"},  defaultValue = "false",
        description = "Enable V4 support")
    protected static boolean v4Enable = false;
 
    @Option(names = {"-4n", "--4netif"}, arity = "1", paramLabel = "<interfaces>",
        description = "Specify the local V4 network interfaces for the Wireguard tunnels (default = none). Invoke without " +
        "arguments to use all the local V4 network interfaces or use the arguments to list specific network interfaces, separated by spaces.")
    protected List<NetworkInterface> v4LocalNetIfs = new ArrayList<>();
    
    @Option(names = {"-4a", "--4addr"}, arity = "1", paramLabel = "<addresses>",
        description = "Specify the local V4 addresses (default: ${DEFAULT-VALUE}) for the Wireguard tunnels. Invoke without " +
        "arguments to use all the local V4 addresses or use the arguments to list specific addresses, separated by spaces.")
    protected List<InetAddress> v4LocalInetAddrs = new ArrayList<>();

    @Option(names = {"-4r", "--4remote"}, arity = "0..*", paramLabel = "<addresses>",
        description = "Specify the list of V4 addresses (default: ${DEFAULT-VALUE}) for the Wireguard tunnels, separated by spaces. " +
        "Note: If no remote V4 addresses are listed, then any specified local interface or local address will use multicast packets.")
    protected List<InetAddress> v4RemoteInetAddrs = new ArrayList<>();
    
    @Option(names = {"-4m", "--4mcastaddr"}, arity = "1", paramLabel = "<address>",
        description = "Specify the local V4 multicast address (default: ${DEFAULT-VALUE}) for multicast packets.")
    protected InetAddress v4McastInetAddr = Constants.V4_MULTICAST_INET_ADDR;
    
    @Option(names = {"-4p", "--4port"}, arity = "1", paramLabel = "<portnum>",
        description = "Specify the local V4 port number (default: ${DEFAULT-VALUE})")
    protected static int v4PortNumber = Constants.V4_PORT;

    @Option(names = {"-4t", "--4tunnel"}, arity = "1", paramLabel = "<network>",
        description = "Specify the network portion of the local V4 tunnel addresses (default: ${DEFAULT-VALUE}).")
    protected String v4TunnelNetwork = Constants.V4_DEFAULT_TUNNEL_NETWORK;
    
    @Option(names = {"-6e", "--6enable"},  defaultValue = "false",
        description = "Enable V6 support")
    protected static boolean v6Enable = false;
 
    @Option(names = {"-6n", "--6netif"}, arity = "0..*", paramLabel = "<interfaces>",
        description = "Specify the local V6 network interfaces for the Wireguard tunnels (default = none). " +
        "Invoke without arguments to use all the local V6 network interfaces or use the arguments to list specific network interfaces, " +
        "separated by spaces.")
    protected List<NetworkInterface> v6LocalNetIfs = new ArrayList<>();
    
    @Option(names = {"-6a", "--6addr"}, arity = "1", paramLabel = "<address>",
        description = "Specify the local V6 addresses (default: ${DEFAULT-VALUE}) for the Wireguard tunnels. " +
        "Invoke without arguments to use all the local V4 addresses or use the arguments to list specific addresses, " +
        "separated by spaces.")
    protected List<InetAddress> v6LocalInetAddrs = new ArrayList<>();

    @Option(names = {"-6r", "--6remote"}, arity = "0..*", paramLabel = "<addresses>",
        description = "Specify the list of remote V6 addresses (default: ${DEFAULT-VALUE}) for the Wireguard tunnels, separated by spaces. " +
        "Note: If no remote V6 addresses are listed, then any specified local interface or local address will use multicast packets.")
    protected List<InetAddress> v6RemoteInetAddrs = new ArrayList<>();
    
    @Option(names = {"-6m", "--6multicastaddr"}, arity = "1", paramLabel = "<address>",
        description = "Specify the local V6 multicast address (default: ${DEFAULT-VALUE}) for multicast packets.")
    protected InetAddress v6McastInetAddr = Constants.V6_MULTICAST_INET_ADDR;

    @Option(names = {"-6p", "--6port"}, arity = "1", paramLabel = "<portnum>",
        description = "Specify the local V6 port number (default: ${DEFAULT-VALUE}).")
    protected static int v6PortNumber = Constants.V6_PORT;
    
    @Option(names = {"-6t", "--6tunnel"}, arity = "1", paramLabel = "<network>",
        description = "Specify the network portion of the local V6 tunnel addresses (default: ${DEFAULT-VALUE}).")
    protected String v6TunnelNetwork = Constants.V6_DEFAULT_TUNNEL_NETWORK;

    @Option(names = {"-g", "--gui"},  defaultValue = "false",
        description = "Enable the GUI.")
    protected static boolean guiEnable = false;

    @Option(names = {"-h", "--help"}, usageHelp = true, defaultValue = "false",
        description = "Show this help page, then exit.")
    protected boolean helpRequested;

    @Option(names = {"-v", "--version"}, versionHelp = true, defaultValue = "false",
        description = "Show the version information, then exit.")
    protected boolean versionRequested;
    
    @Option(names = {"-k", "--keepalive"}, arity = "1", paramLabel = "<seconds>",
        description = "Specify the persistent keepalive interval for all Wireguard tunnels (default: ${DEFAULT-VALUE})")
    protected static int persistentKeepalive = Constants.DEFAULT_PERSISTENT_KEEPALIVE;

    /**
     * Start the V4/V6 machines.
     * 
     */
    protected void startMachines() {
        try {
            log.info(Version.getVersion());
            log.info("Arguments: " + spec.args().toString());
            int cores = Runtime.getRuntime().availableProcessors();
            log.info("Number of available core processors: " + cores);
            
            // Create the persistent data and log directories for the application
            if (SystemUtils.IS_OS_UNIX) {
                FileUtils.forceMkdir(new File(Constants.DATABASE_DIRECTORY));
                FileUtils.forceMkdir(new File(Constants.LOG_DIRECTORY));
                FileUtils.openOutputStream(new File(Constants.LOG_FILE));
            } else if (SystemUtils.IS_OS_WINDOWS) {

            }
            
            Runtime.getRuntime().addShutdownHook(new Thread() {
                @Override
                public void run() {
                    log.info("Stopping " + WG_CONNECT_MACHINES);
                    System.out.println("Stopping " + WG_CONNECT_MACHINES + ": " + new Date());
                }
            });
            
            // Check the validity of the tunnel networks
            IPAddressString v4TunnelNetIPAddrStr = new IPAddressString(v4TunnelNetwork);
            for (IPv4AddressSegment seg : v4TunnelNetIPAddrStr.getAddress().toIPv4().getSegments()) {
                if (!seg.isIPv4()) {
                    System.err.printf("Invalid V4 tunnel network: %s", seg.toString());
                    System.exit(1);
                }
            }
            
            IPAddressString v6TunnelNetIPAddrStr = new IPAddressString(v6TunnelNetwork);
            for (IPv6AddressSegment seg : v6TunnelNetIPAddrStr.getAddress().toIPv6().getSegments()) {
                if (!seg.isIPv6()) {
                    System.err.printf("Invalid V6 tunnel network: %s", seg.toString());
                    System.exit(1);
                }
            }
            
            executorService = Executors.newCachedThreadPool();
            scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();

            wgConnectTunnels = Collections.synchronizedList(new ArrayList());
            wgConnectTunnelsHandlers = new ArrayList<>();
            
            config = ConnectConfig.getInstance();
            config.init();
            
            List<PersistenceTunnel> currentTunnels = config.initTunnelsList();
            
            String msg;
            msg = (v4LocalInetAddrs != null) ? "V4 local addresses: " + Arrays.toString(v4LocalInetAddrs.toArray()) :
                "V4 local addresses: none";
            System.out.println(msg);
            log.info(msg);
            
            msg = (v4RemoteInetAddrs != null) ? "V4 remote addresses: " + Arrays.toString(v4RemoteInetAddrs.toArray()) :
                "V4 remote addresses: none";
            System.out.println(msg);
            log.info(msg);
            
            msg = "V4 interfaces: none";
            if (!v4LocalNetIfs.isEmpty()) {
                StringBuilder sb = new StringBuilder();
                sb.append("V4 local interfaces: [");
                v4LocalNetIfs
                    .stream()
                    .map(mcastNetIf -> {
                        sb.append(mcastNetIf.getName());
                        return mcastNetIf;
                    })
                    .forEachOrdered(_item -> {
                        sb.append(", ");
                    });
                sb.setLength(sb.length() - 2);	// remove last ", "
                sb.append(']');
                msg = sb.toString();
                System.out.println(msg);
                log.info(msg);
            }
            System.out.println(msg);
            log.info(msg);
            
            msg = "V4 port number: " + v4PortNumber;
            System.out.println(msg);
            log.info(msg);
            
            msg = "V4 tunnel network: " + v4TunnelNetwork;
            System.out.println(msg);
            log.info(msg);
            
            msg = (v6LocalInetAddrs != null) ? "V6 local addresses: " + Arrays.toString(v6LocalInetAddrs.toArray()) :
                "V6 local addresses: none";
            System.out.println(msg);
            log.info(msg);
            
            msg = (v6RemoteInetAddrs != null) ? "V6 remote addresses: " + Arrays.toString(v6RemoteInetAddrs.toArray()) :
                "V6 remote addresses: none";
            System.out.println(msg);
            log.info(msg);
            
            msg = "V6 local interfaces: none";
            if (v6LocalNetIfs != null) {
                StringBuilder sb = new StringBuilder();
                sb.append("V6 local interfaces: [");
                v6LocalNetIfs
                    .stream()
                    .map(mcastNetIf -> {
                        sb.append(mcastNetIf.getName());
                        return mcastNetIf;
                    })
                    .forEachOrdered(_item -> {
                        sb.append(", ");
                    });
                sb.setLength(sb.length() - 2);	// remove last ", "
                sb.append(']');
                msg = sb.toString();
                System.out.println(msg);
                log.info(msg);
            }
            System.out.println(msg);
            log.info(msg);
            
            msg = "V6 port number: " + v6PortNumber;
            System.out.println(msg);
            log.info(msg);
            
            msg = "V6 tunnel network: " + v6TunnelNetwork;
            System.out.println(msg);
            log.info(msg);
            
            // Start the GUI
            if (guiEnable && SystemUtils.IS_OS_LINUX) {
                new Thread() {
                    @Override
                    public void run() {
                        Application.launch(Gui.class);
                    }
                }.start();
                
                Gui.waitForWindowShown();
            }
            
            // Start the tunnels refresh thread
            scheduledExecutorService.scheduleAtFixedRate(WgConnect::refreshTunnels, TUNNELS_REFRESH_RATE, TUNNELS_REFRESH_RATE, TimeUnit.SECONDS);

            // Start the V4 machiness
            if (spec.commandLine().getParseResult().originalArgs().stream().anyMatch(a -> a.startsWith(V4_OPTION))) {
                startV4Machiness();
            }
            
            // Start the V6 machines
            if (spec.commandLine().getParseResult().originalArgs().stream().anyMatch(a -> a.startsWith(V6_OPTION))) {
                startV6Machines();
            }
            
            log.info("Startup complete");
        } catch (ConfigException | IOException ex) {
            log.info(ex.getMessage());
        }
    }
    
    public static void refreshTunnels() {
        for (WgInterfaceInfo netIf : Utils.getAllWgNetIfs()) {
            for (WgInterfaceInfo.Peer peer : netIf.getPeers()) {
                PersistenceTunnel tunnel = WgConnect.getTunnelByLocalAndRemotePublicKeys(netIf.getLocalPublicKey(), peer.getPublicKey());
                if (tunnel != null) {
                    tunnel.setFwmark(netIf.getLocalFwmark());
                    tunnel.setSentBandwidth(peer.getSentBandwidth());
                    tunnel.setReceivedBandwidth(peer.getReceivedBandwidth());
                    tunnel.setLatestHandshake(peer.getLatestHandshake());
                }
            }
        }
        
        wgConnectTunnelsHandlers.stream().forEach(handler -> handler.handleNotification());
    }
    
    public static interface RefreshTunnelsHandler {
        public void handleNotification();
    }
    
    public static boolean addRefreshTunnelsHandler(RefreshTunnelsHandler handler) {
        return wgConnectTunnelsHandlers.add(handler);
    }
    
    // Start the V4 machines
    private void startV4Machiness() {
        try {
            if (v4LocalNetIfs.isEmpty()) {
                v4LocalNetIfs = getV4MulticastNetIfs();
            }

            v4McastSock = new MulticastSocket(new InetSocketAddress(v4McastInetAddr, v4PortNumber));
            if (v4RemoteInetAddrs.isEmpty()) {
                v4RemoteInetAddrs.add(v4McastInetAddr);
            }
            
            List<InetAddress> inetAddrs = new ArrayList<>();
            if (v4LocalInetAddrs.isEmpty()) {
                for (NetworkInterface netIf : v4LocalNetIfs) {
                    Enumeration<InetAddress> ifAddrs = netIf.getInetAddresses();
                    while (ifAddrs.hasMoreElements()) {
                        InetAddress inetAddr = ifAddrs.nextElement();
                        if (inetAddr instanceof Inet4Address && !inetAddr.isLinkLocalAddress() && !inetAddr.isLoopbackAddress()) {
                            IPAddressString addrStr = new IPAddressString(inetAddr.getHostAddress());
                            InetAddress ia;
                            if (!addrStr.isPrefixed()) {
                                ia = new IPAddressString(inetAddr.getHostAddress()).getAddress().toInetAddress();
                            } else {
                                ia = addrStr.toAddress().toInetAddress();
                            }

                            inetAddrs.add(ia);
                        }
                    }
                    
                    WgConnect.McastReceiver v4McastReceiver = new WgConnect.McastReceiver(
                        netIf, v4McastSock, IPVersion.IPV4);
                    v4McastReceivers.add(v4McastReceiver);

                    new Thread() {
                        @Override
                        public void run() {
                            executorService.execute(v4McastReceiver);
                        }
                    }.start();
                }
            } else {
                for (InetAddress inetAddr : v4LocalInetAddrs) {

                    NetworkInterface netIf = getV4NetIfByIpAddr(inetAddr.getHostAddress());
                    if (netIf != null) {
                        IPAddressString addrStr = new IPAddressString(inetAddr.getHostAddress());
                        if (!addrStr.isPrefixed()) {
                            inetAddr = new IPAddressString(inetAddr.getHostAddress()).getAddress().toInetAddress();
                        }
 
                        inetAddrs.add(inetAddr);
                        
                        WgConnect.McastReceiver v4McastReceiver = new WgConnect.McastReceiver(
                            netIf, v4McastSock, IPVersion.IPV4);
                        v4McastReceivers.add(v4McastReceiver);

                        new Thread() {
                            @Override
                            public void run() {
                                executorService.execute(v4McastReceiver);
                            }
                        }.start();
                    } else {
                        log.info("Could not find the network interface for V4 address {}", inetAddr.getHostAddress());
                    }
                }
            }
           
            v4Machine = new V4Machine(inetAddrs, v4PortNumber, v4RemoteInetAddrs, v4PortNumber, v4TunnelNetwork);

            new Thread() {
                @Override
                public void run() {
                    executorService.execute(v4Machine);
                }
            }.start();
        } catch (IOException | AddressStringException | IncompatibleAddressException ex) {
            log.info(ex.getMessage());
        }
    }
    
    // Start the V6 machines
    private void startV6Machines() {
        try {
            if (v6LocalNetIfs.isEmpty()) {
                v6LocalNetIfs = getV6MulticastNetIfs();
            }

            v6McastSock = new MulticastSocket(new InetSocketAddress(v6McastInetAddr, v6PortNumber));
            if (v6RemoteInetAddrs.isEmpty()) {
                v6RemoteInetAddrs.add(v6McastInetAddr);
            }
            
            List<InetAddress> inetAddrs = new ArrayList<>();
            if (v6LocalInetAddrs.isEmpty()) {
                for (NetworkInterface netIf : v6LocalNetIfs) {
                    Enumeration<InetAddress> ifAddrs = netIf.getInetAddresses();
                    while (ifAddrs.hasMoreElements()) {
                        InetAddress inetAddr = ifAddrs.nextElement();
                        if (inetAddr instanceof Inet6Address && !inetAddr.isLinkLocalAddress() && !inetAddr.isLoopbackAddress()) {
                            InetAddress ia = new IPAddressString(StringUtils.substringBefore(inetAddr.getHostAddress(),
                                IPv6Address.ZONE_SEPARATOR)).toAddress().toInetAddress();
                            inetAddrs.add(ia);
                        }
                    }
                    
                    WgConnect.McastReceiver v6McastReceiver = new WgConnect.McastReceiver(
                        netIf, v6McastSock, IPVersion.IPV6);
                    v6McastReceivers.add(v6McastReceiver);

                    new Thread() {
                        @Override
                        public void run() {
                            executorService.execute(v6McastReceiver);
                        }
                    }.start();
                }
            } else {
                for (InetAddress inetAddr : v6LocalInetAddrs) {
                    NetworkInterface netIf = getV6NetIfByIpAddr(inetAddr.getHostAddress());
                    if (netIf != null) {
                        IPAddressString addrStr = new IPAddressString(StringUtils.substringBefore(inetAddr.getHostAddress(),
                            IPv6Address.ZONE_SEPARATOR));
                        if (!addrStr.isPrefixed()) {
                            inetAddr = new IPAddressString(addrStr.toAddress().toInetAddress().getHostAddress() + "/" + IPv6Address.BITS_PER_SEGMENT)
                                .getAddress().toInetAddress();
                        }
                        
                        inetAddrs.add(inetAddr);
                       
                        WgConnect.McastReceiver v6McastReceiver = new WgConnect.McastReceiver(
                            netIf, v6McastSock, IPVersion.IPV6);
                        v6McastReceivers.add(v6McastReceiver);

                        new Thread() {
                            @Override
                            public void run() {
                                executorService.execute(v6McastReceiver);
                            }
                        }.start();
                    } else {
                        log.info("Could not find the network interface for V6 address {}", inetAddr.getHostAddress());
                    }
                }
            }
            
            v6Machine = new V6Machine(inetAddrs, v6PortNumber, v6RemoteInetAddrs, v6PortNumber, v6TunnelNetwork);
            
            new Thread() {
                @Override
                public void run() {
                    executorService.execute(v6Machine);
                }
            }.start();
        } catch (IOException | AddressStringException | IncompatibleAddressException ex) {
            log.info(ex.getMessage());
        }
    }
    
    protected class McastReceiver implements Runnable {
        
        static final int BUFFER_SIZE = 4096;
        
        NetworkInterface networkInterface = null;
        MulticastSocket mcastSock = null;
        IPVersion ipVersion;
        
        String name;
        
        public McastReceiver(NetworkInterface networkInterface, MulticastSocket mcastSock,
            IPVersion ipVersion) {
            this.networkInterface = networkInterface;
            this.mcastSock = mcastSock;
            this.ipVersion = ipVersion;
            
            name = String.format("%s-%s-%s", networkInterface.getName(), mcastSock.getLocalAddress().getHostAddress(),
                mcastSock.getLocalPort());
        }
        
        public String getName() {
            return name;
        }
        
        @Override
        public void run() {
            if (networkInterface != null) {
                try {
                    mcastSock.joinGroup(mcastSock.getLocalSocketAddress(), networkInterface);
                    
                    while (true) {
                        try {
                            byte[] buffer = new byte[BUFFER_SIZE];
                            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                            mcastSock.receive(packet);
                            
                            if (ipVersion.isIPv4()) {
                                v4Machine.processMulticastDiscoverMessage(packet, buffer, packet.getLength());
                            } else if (ipVersion.isIPv6()) {
                                v6Machine.processMulticastSolicitMessage(packet, buffer, packet.getLength());
                            } else {
                                log.info("Unknown IP type: {}", ipVersion);
                                break;
                            }
                        } catch (IOException ex) {
                            log.info(ex.getMessage());
                        }
                    }
                    
                    mcastSock.close();
                } catch (IOException ex) {
                    log.info(ex.getMessage());
                }
            }
        }
    }
    
    public static List<McastReceiver> getV4McastReceivers() {
        return v4McastReceivers;
    }
    
    public static V4Machine getV4Machine() {
        return v4Machine;
    }
    
    public static List<McastReceiver> getV6McastReceivers() {
        return v6McastReceivers;
    }

    public static V6Machine getV6Machine() {
        return v6Machine;
    }

    /**
     * Gets the V6 network interfaces for the supplied interface names.
     *
     * @param ifnames the interface names to locate NetworkInterfaces by
     *
     * @return the list of NetworkInterfaces that are up, support multicast, and
     * have at least one V6 address configured
     *
     * @throws SocketException the socket exception
     */
    public static List<NetworkInterface> getV6NetIfs(String[] ifnames) throws SocketException {
        List<NetworkInterface> netIfs = new ArrayList<>();
        for (String ifname : ifnames) {
            if (StringUtils.equals(ifname, "*")) {
                return getAllV6NetIfs();
            }
            
            NetworkInterface netIf = NetworkInterface.getByName(ifname);
            if (netIf == null) {
                // if not found by name, see if the name is actually an address
                try {
                    InetAddress ipaddr = InetAddress.getByName(ifname);
                    netIf = NetworkInterface.getByInetAddress(ipaddr);
                } catch (UnknownHostException ex) {
                    log.warn("Unknown interface: " + ifname + ": " + ex);
                }
            }
            
            if (netIf != null) {
                if (netIf.isUp()) {
                    // for multicast, the loopback interface is excluded
                    if (netIf.supportsMulticast() && !netIf.isLoopback()) {
                        boolean isV6 = false;
                        List<InterfaceAddress> ifAddrs = netIf.getInterfaceAddresses();
                        for (InterfaceAddress ifAddr : ifAddrs) {
                            if (ifAddr.getAddress() instanceof Inet6Address) {
                                netIfs.add(netIf);
                                isV6 = true;
                                break;
                            }
                        }
                        
                        if (!isV6) {
                            System.err.println("Interface is not configured for IPv6: " + netIf);
                            return null;
                        }
                    } else {
                        System.err.println("Interface does not support multicast: " + netIf);
                        return null;
                    }
                } else {
                    System.err.println("Interface is not up: " + netIf);
                    return null;
                }
            } else {
                System.err.println("Interface not found or inactive: " + ifname);
                return null;
            }
        }
        
        return netIfs;
    }

    /**
     * Gets all V6 network interfaces on the local host.
     *
     * @return the list NetworkInterfaces
     * @throws java.net.SocketException
     */
    public static List<NetworkInterface> getAllV6NetIfs() throws SocketException {
        List<NetworkInterface> netIfs = new ArrayList<>();
        Enumeration<NetworkInterface> localInterfaces = NetworkInterface.getNetworkInterfaces();
        if (localInterfaces != null) {
            while (localInterfaces.hasMoreElements()) {
                NetworkInterface netIf = localInterfaces.nextElement();
                // for multicast, the loopback interface is excluded
                if (netIf.supportsMulticast() && !netIf.isLoopback()) {
                    Enumeration<InetAddress> ifAddrs = netIf.getInetAddresses();
                    while (ifAddrs.hasMoreElements()) {
                        InetAddress ip = ifAddrs.nextElement();
                        if (ip instanceof Inet6Address) {
                            netIfs.add(netIf);
                            break;	// out to next interface
                        }
                    }
                }
            }
        } else {
            log.error("No network interfaces found!");
        }
        
        return netIfs;
    }

    public static List<InetAddress> getV6InetAddrs(String[] addrs) throws UnknownHostException {
        List<InetAddress> ipAddrs = new ArrayList<>();
        for (String addr : addrs) {
            InetAddress ipAddr = InetAddress.getByName(addr);
            // allow only IPv6 addresses?
            ipAddrs.add(ipAddr);
        }
        
        return ipAddrs;
    }

    static List<InetAddress> allV6InetAddrs;

    public static List<InetAddress> getAllV6InetAddrs() {
        if (allV6InetAddrs == null) {
            allV6InetAddrs = new ArrayList<>();
            try {
                Enumeration<NetworkInterface> localInterfaces = NetworkInterface.getNetworkInterfaces();
                if (localInterfaces != null) {
                    while (localInterfaces.hasMoreElements()) {
                        NetworkInterface netIf = localInterfaces.nextElement();
                        Enumeration<InetAddress> ifAddrs = netIf.getInetAddresses();
                        while (ifAddrs.hasMoreElements()) {
                            InetAddress inetAddr = ifAddrs.nextElement();
                            if (inetAddr instanceof Inet6Address) {
                                if (!inetAddr.isLinkLocalAddress() && !inetAddr.isLoopbackAddress()) {
                                    InetAddress ia = new IPAddressString(
                                        StringUtils.substringBefore(inetAddr.getHostAddress(), IPv6Address.ZONE_SEPARATOR)).toAddress().toInetAddress();
                                    allV6InetAddrs.add(ia);
                                }
                            }
                        }
                    }
                } else {
                    log.error("No network interfaces found!");
                }
            } catch (AddressStringException | IncompatibleAddressException | SocketException ex) {
                log.error("Failed to get IPv6 addresses: " + ex);
            }
        }
        
        return allV6InetAddrs;
    }

    public static List<InetAddress> getFilteredV6InetAddrs() {
        return getFilteredV6InetAddrs(getAllV6InetAddrs());
    }

    public static List<InetAddress> getFilteredV6InetAddrs(List<InetAddress> v6Addrs) {
        List<InetAddress> filteredV6Addrs = new ArrayList<>();
        if (v6Addrs != null) {
            for (InetAddress ip : v6Addrs) {
                filteredV6Addrs.add(ip);
            }
        }
        
        return filteredV6Addrs;
    }

    public static NetworkInterface getV4NetIf(String ifname) throws SocketException {
        NetworkInterface netIf = NetworkInterface.getByName(ifname);
        if (netIf == null) {
            // if not found by name, see if the name is actually an address
            try {
                InetAddress ipaddr = InetAddress.getByName(ifname);
                netIf = NetworkInterface.getByInetAddress(ipaddr);
            } catch (UnknownHostException ex) {
                log.warn("Unknown interface: " + ifname + ": " + ex);
            }
        }
        
        if (netIf != null) {
            if (netIf.isUp()) {
                // the loopback interface is excluded
                if (!netIf.isLoopback()) {
                    boolean isV4 = false;
                    List<InterfaceAddress> ifAddrs = netIf.getInterfaceAddresses();
                    for (InterfaceAddress ifAddr : ifAddrs) {
                        if (ifAddr.getAddress() instanceof Inet4Address) {
                            isV4 = true;
                            break;
                        }
                    }
        
                    if (!isV4) {
                        System.err.println("Interface is not configured for IPv4: " + netIf);
                        return null;
                    }
                } else {
                    System.err.println("Interface is loopback: " + netIf);
                    return null;
                }
            } else {
                System.err.println("Interface is not up: " + netIf);
                return null;
            }
        } else {
            System.err.println("Interface not found or inactive: " + ifname);
            return null;
        }
        
        return netIf;
    }

    public static List<InetAddress> getV4InetAddrs(String[] addrs) throws UnknownHostException {
        List<InetAddress> ipAddrs = new ArrayList<>();
        for (String addr : addrs) {
            InetAddress ipAddr = InetAddress.getByName(addr);
            // allow only IPv4 addresses?
            ipAddrs.add(ipAddr);
        }
        
        return ipAddrs;
    }

    static List<InetAddress> allV4InetAddrs;

    public static List<InetAddress> getAllV4InetAddrs() {
        if (allV4InetAddrs == null) {
            allV4InetAddrs = new ArrayList<>();
            try {
                Enumeration<NetworkInterface> localInterfaces = NetworkInterface.getNetworkInterfaces();
                if (localInterfaces != null) {
                    while (localInterfaces.hasMoreElements()) {
                        NetworkInterface netIf = localInterfaces.nextElement();
                        Enumeration<InetAddress> ifAddrs = netIf.getInetAddresses();
                        while (ifAddrs.hasMoreElements()) {
                            InetAddress ip = ifAddrs.nextElement();
                            if (ip instanceof Inet4Address) {
                                allV4InetAddrs.add(ip);
                            }
                        }
                    }
                } else {
                    log.error("No network interfaces found!");
                }
            } catch (IOException ex) {
                log.error("Failed to get IPv4 addresses: " + ex);
            }
        }
        
        return allV4InetAddrs;
    }

    public static List<InetAddress> getFilteredV4InetAddrs() {
        return getFilteredV4InetAddrs(getAllV4InetAddrs());
    }

    public static List<InetAddress> getFilteredV4InetAddrs(List<InetAddress> v4Addrs) {
        List<InetAddress> filteredV4Addrs = new ArrayList<>();
        if (v4Addrs != null) {
            for (InetAddress addr : v4Addrs) {
                if (!addr.isLoopbackAddress() && !addr.isLinkLocalAddress()) {
                    filteredV4Addrs.add(addr);
                }
            }
        }

        return filteredV4Addrs;
    }

    public static boolean isLocalV4Addr(String inetAddr) {
        return getAllV4InetAddrs().stream().anyMatch(ia -> StringUtils.equals(ia.getHostAddress(), inetAddr));
    }
    
    public static List<InterfaceAddress> getV4IfAddrs() throws SocketException {
        List<InterfaceAddress> interfaceList = new ArrayList<>();
        
        Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
        if (interfaces != null) {
            while (interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();
                if (networkInterface.isLoopback() || !networkInterface.isUp()) {
                    continue;
                }

                if (!networkInterface.getInterfaceAddresses().isEmpty()) {
                    for (InterfaceAddress ifAddr : networkInterface.getInterfaceAddresses()) {
                        if (ifAddr != null && ifAddr.getAddress() instanceof Inet4Address) {
                            interfaceList.add(ifAddr);
                        }
                    }
                }
            }
        }
        
        return interfaceList;
    }
    
    public static InterfaceAddress getV4IfAddr(String ipAddrStr) throws SocketException {
        IPAddressString ipAddressString = new IPAddressString(ipAddrStr);

        InterfaceAddress interfaceAddress = null;
        for (InterfaceAddress ifAddr : getV4IfAddrs()) {
            IPAddress ipAddr = new IPAddressString(ifAddr.getAddress().getHostAddress()).getAddress();
            if (ipAddr.matches(ipAddressString)) {
                interfaceAddress = ifAddr;
                break;
            }
        }
        
        return interfaceAddress;
    }
    
    public static List<NetworkInterface> getV4MulticastNetIfs() throws SocketException {
        List<NetworkInterface> multicastList = new ArrayList<>();
        
        Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
        if (interfaces != null) {
            while (interfaces.hasMoreElements()) {
                NetworkInterface netIf = interfaces.nextElement();
                if (!netIf.isUp() || !netIf.supportsMulticast() || netIf.isLoopback()) {
                    continue;
                }
                
                Enumeration<InetAddress> ifAddrs = netIf.getInetAddresses();
                while (ifAddrs.hasMoreElements()) {
                    InetAddress inetAddr = ifAddrs.nextElement();
                    if (inetAddr instanceof Inet4Address) {
                        multicastList.add(netIf);
                        break;
                    }
                }
            }
        }
        
        return multicastList;
    }
    
    public static List<NetworkInterface> getV4NetIfs() throws SocketException {
        List<NetworkInterface> networkIfList = new ArrayList<>();
        
        Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
        if (interfaces != null) {
            while (interfaces.hasMoreElements()) {
                NetworkInterface netIf = interfaces.nextElement();
                
                Enumeration<InetAddress> ifAddrs = netIf.getInetAddresses();
                while (ifAddrs.hasMoreElements()) {
                    InetAddress inetAddr = ifAddrs.nextElement();
                    if (inetAddr instanceof Inet4Address) {
                        networkIfList.add(netIf);
                        break;
                    }
                }
            }
        }
        
        return networkIfList;
    }
    
    public static NetworkInterface getV4NetIfByIpAddr(String ipAddrStr) throws SocketException {
        IPAddressString ipAddressString = new IPAddressString(ipAddrStr);

        NetworkInterface networkInterface = null;
        for (NetworkInterface netIf : getV4NetIfs()) {
            Enumeration<InetAddress> ifAddrs = netIf.getInetAddresses();
            while (ifAddrs.hasMoreElements()) {
                InetAddress inetAddr = ifAddrs.nextElement();
                IPAddress ipAddr = new IPAddressString(inetAddr.getHostAddress()).getAddress();
                if (ipAddr.matches(ipAddressString)) {
                    networkInterface = netIf;
                    break;
                }
            }
        }
        
        return networkInterface;
    }
    
    public static NetworkInterface getV4NetIfByName(String networkIfName) throws SocketException {
        NetworkInterface networkInterface = null;
        for (NetworkInterface netIf : getV4NetIfs()) {
            if (netIf.getName().equalsIgnoreCase(networkIfName)) {
                networkInterface = netIf;
                break;
            }
        }
        
        return networkInterface;
    }
    
    public static boolean isValidV4InetAddr(String inetAddr) throws SocketException {
        IPAddressString addrStr = new IPAddressString(inetAddr);

        boolean isValid = false;
        for (InterfaceAddress ifAddr : getV4IfAddrs()) {
            IPAddress ipAddr = new IPAddressString(ifAddr.getAddress().getHostAddress()).getAddress();
            if (ipAddr.matches(addrStr)) {
                isValid = true;
                break;
            }
        }
        
        return isValid;
    }
    
    public static List<NetworkInterface> getV6MulticastNetIfs() throws SocketException {
        List<NetworkInterface> multicastList = new ArrayList<>();
        
        Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
        if (interfaces != null) {
            while (interfaces.hasMoreElements()) {
                NetworkInterface netIf = interfaces.nextElement();
                if (!netIf.isUp() || !netIf.supportsMulticast() || netIf.isLoopback()) {
                    continue;
                }
                
                Enumeration<InetAddress> ifAddrs = netIf.getInetAddresses();
                while (ifAddrs.hasMoreElements()) {
                    InetAddress inetAddr = ifAddrs.nextElement();
                    if (inetAddr instanceof Inet6Address) {
                        if (inetAddr.isLinkLocalAddress()) {
                            continue;
                        }
                        multicastList.add(netIf);
                        break;
                    }
                }
            }
        }
        
        return multicastList;
    }
    
    public static List<NetworkInterface> getV6NetIfs() throws SocketException {
        List<NetworkInterface> interfaceList = new ArrayList<>();
        
        Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
        while (interfaces.hasMoreElements()) {
            NetworkInterface netIf = interfaces.nextElement();
            if (!netIf.isUp() || netIf.isLoopback()) {
                continue;
            }
            
            Enumeration<InetAddress> ifAddrs = netIf.getInetAddresses();
            while (ifAddrs.hasMoreElements()) {
                InetAddress inetAddr = ifAddrs.nextElement();
                if (inetAddr instanceof Inet6Address) {
                    interfaceList.add(netIf);
                    break;
                }
            }
        }
        
        return interfaceList;
    }

    public static NetworkInterface getV6NetIfByIpAddr(String ipAddrStr) throws SocketException {
        IPAddressString ipAddressString = new IPAddressString(ipAddrStr);

        NetworkInterface networkInterface = null;
        for (NetworkInterface netIf : getV6NetIfs()) {
            Enumeration<InetAddress> ifAddrs = netIf.getInetAddresses();
            while (ifAddrs.hasMoreElements()) {
                InetAddress inetAddr = ifAddrs.nextElement();
                IPAddress ipAddr = new IPAddressString(StringUtils.substringBefore(
                    inetAddr.getHostAddress(), IPv6Address.ZONE_SEPARATOR)).getAddress();
                if (ipAddr.matches(ipAddressString)) {
                    networkInterface = netIf;
                    break;
                }
            }
        }
        
        return networkInterface;
    }
    
    public static NetworkInterface getV6NetIfByName(String networkIfName) throws SocketException {
        NetworkInterface networkInterface = null;
        for (NetworkInterface netIf : getV6NetIfs()) {
            if (netIf.getName().equalsIgnoreCase(networkIfName)) {
                networkInterface = netIf;
                break;
            }
        }
        
        return networkInterface;
    }
    
    public static boolean isLocalV6Addr(String inetAddr) {
        return getAllV6InetAddrs().stream().anyMatch(ia -> StringUtils.equals(ia.getHostAddress(), inetAddr));
    }
    
    public static List<InterfaceAddress> getV6IfAddrs() throws SocketException {
        List<InterfaceAddress> interfaceList = new ArrayList<>();
        
        Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
        if (interfaces != null) {
            while (interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();
                if (networkInterface.isLoopback() || !networkInterface.isUp()) {
                    continue;
                }

                if (!networkInterface.getInterfaceAddresses().isEmpty()) {
                    for (InterfaceAddress ifAddr : networkInterface.getInterfaceAddresses()) {
                        if (ifAddr != null && ifAddr.getAddress() instanceof Inet6Address) {
                            interfaceList.add(ifAddr);
                        }
                    }
                }
            }
        }
        
        return interfaceList;
    }
    
    public static boolean isValidV6InetAddr(String inetAddr) throws SocketException {
        IPAddressString addrStr = new IPAddressString(inetAddr);
        boolean isValid = false;

        for (InterfaceAddress ifAddr : getV6IfAddrs()) {
            IPAddress ipAddr = new IPAddressString(
                StringUtils.substringBefore(ifAddr.getAddress().getHostAddress(), IPv6Address.ZONE_SEPARATOR)).getAddress();
            if (ipAddr.matches(addrStr)) {
                isValid = true;
                break;
            }
        }
        
        return isValid;
    }
    
    // Use null as tertiary value so that we calculate this only once
    static Boolean multipleV4Interfaces = null;

    public static boolean hasMultipleV4Interfaces() throws SocketException {
        if (multipleV4Interfaces == null) {
            multipleV4Interfaces = false;
            Enumeration<NetworkInterface> netIfs = NetworkInterface.getNetworkInterfaces();
            if (netIfs != null) {
                boolean oneV4Interface = false;
                while (netIfs.hasMoreElements()) {
                    NetworkInterface netIf = netIfs.nextElement();
                    if (netIf.isUp()) {
                        // the loopback interface is excluded
                        if (!netIf.isLoopback()) {
                            List<InterfaceAddress> ifAddrs = netIf.getInterfaceAddresses();
                            for (InterfaceAddress ifAddr : ifAddrs) {
                                if (ifAddr.getAddress() instanceof Inet4Address) {
                                    if (oneV4Interface) {
                                        // already have one, so must have multiple
                                        multipleV4Interfaces = true;
                                        break;
                                    }
                                    oneV4Interface = true;
                                }
                            }
                        }
                    }
                }
            }
        }
        
        return multipleV4Interfaces;
    }

    public List<NetworkInterface> getV6McastNetIfs() {
        return v6LocalNetIfs;
    }

    public void setV6McastNetIfs(List<NetworkInterface> v6McastNetIfs) {
        this.v6LocalNetIfs = v6McastNetIfs;
    }

    public void addV6McastNetIf(NetworkInterface v6McastNetIf) {
        if (v6LocalNetIfs == null) {
            v6LocalNetIfs = new ArrayList<>();
        }
        v6LocalNetIfs.add(v6McastNetIf);
    }

    public List<InetAddress> getV6UcastAddrs() {
        return v6RemoteInetAddrs;
    }

    public void setV6UcastAddrs(List<InetAddress> v6UcastAddrs) {
        this.v6RemoteInetAddrs = v6UcastAddrs;
    }

    public void addV6UcastAddr(InetAddress v6UcastAddr) {
        if (v6RemoteInetAddrs == null) {
            v6RemoteInetAddrs = new ArrayList<>();
        }
        v6RemoteInetAddrs.add(v6UcastAddr);
    }

    public static int getV6PortNumber() {
        return v6PortNumber;
    }

    public static void setV6PortNumber(int portNumber) {
        v6PortNumber = portNumber;
    }

    public List<NetworkInterface> getV4McastNetIfs() {
        return v4LocalNetIfs;
    }

    public void setV4McastNetIfs(List<NetworkInterface> v4McastNetIf) {
        this.v4LocalNetIfs = v4McastNetIf;
    }

    public List<InetAddress> getV4UcastAddrs() {
        return v4RemoteInetAddrs;
    }

    public void setV4UcastAddrs(List<InetAddress> v4UcastAddrs) {
        this.v4RemoteInetAddrs = v4UcastAddrs;
    }

    public void addV4UcastAddr(InetAddress v4UcastAddr) {
        if (v4RemoteInetAddrs == null) {
            v4RemoteInetAddrs = new ArrayList<>();
        }
        v4RemoteInetAddrs.add(v4UcastAddr);
    }

    public static int getV4PortNumber() {
        return v4PortNumber;
    }

    public static void setV4PortNumber(int portNumber) {
        v4PortNumber = portNumber;
    }

    public static ConnectConfig getConnectConfig() {
        return config;
    }

    public static boolean getDatabaseEnabled() {
        return databaseEnabled;
    }
    
    public static String getDatabaseFilename() {
        return databaseFilename;
    }
    
    public static String getDatabaseUsername() {
        return databaseUsername;
    }
    
    public static String getDatabasePassword() {
        return databasePassword;
    }
    
    public static int getPersistentKeepalive() {
        return persistentKeepalive;
    }
    
    private static final String TUNNEL_COMPLETE_MSG_FORMAT = "%s: Tunnel: %s to %s, Endpoints: %s:%s to %s:%s\n";
    public static void printTunnelCompleteMessage(PersistenceTunnel tunnel) {
        if (tunnel != null) {
            System.out.printf(TUNNEL_COMPLETE_MSG_FORMAT, spec.name(),
                tunnel.getLocalTunnelInetAddr(), tunnel.getRemoteTunnelInetAddr(),
                tunnel.getLocalPhysInetAddr(), tunnel.getLocalPhysInetListenPort(),
                tunnel.getRemotePhysInetAddr(), tunnel.getRemotePhysInetListenPort());
        }
    }
    
    public static void addTunnel(PersistenceTunnel tunnel) {
        wgConnectTunnels.add(tunnel);
    }

    public static PersistenceTunnel getTunnelByTunnelId(String id) {
        return wgConnectTunnels
            .stream()
            .filter(t -> t.getId() != null && StringUtils.equals(t.getId().toString(), id))
            .findFirst()
            .orElse(null);
    }
    
    public static PersistenceTunnel getTunnelByRemotePhysInetAddr(String remotePhysInetAddr) {
        return wgConnectTunnels
            .stream()
            .filter(t -> StringUtils.equals(t.getRemotePhysInetAddr(), remotePhysInetAddr))
            .findFirst()
            .orElse(null);
    }
    
    public static List<PersistenceTunnel> getTunnelsByRemotePhysInetAddr(String remotePhysInetAddr) {
        return wgConnectTunnels
            .stream()
            .filter(t -> StringUtils.equals(t.getRemotePhysInetAddr(), remotePhysInetAddr))
            .collect(Collectors.toList());
    }
    
    public static PersistenceTunnel getTunnelByLocalPhysInetAddr(String localPhysInetAddr) {
        return wgConnectTunnels
            .stream()
            .filter(t -> StringUtils.equals(t.getLocalPhysInetAddr(), localPhysInetAddr))
            .findFirst()
            .orElse(null);
    }
    
    public static PersistenceTunnel getTunnelByLocalPhysInetAddrAndTunnelInetNet(String localPhysInetAddr, String tunnelInetNet, IPVersion ipVersion) {
        return wgConnectTunnels
            .stream()
            .filter(t -> StringUtils.equals(t.getLocalPhysInetAddr(), localPhysInetAddr) &&
                         new IPAddressString(t.getTunnelInetNet()).getAddress(ipVersion).compareTo(
                             new IPAddressString(tunnelInetNet).getAddress(ipVersion)) == 0)
            .findFirst()
            .orElse(null);
    }
    
    public static PersistenceTunnel getTunnelByLocalAndRemotePhysInetAddr(String localPhysInetAddr, String remotePhysInetAddr) {
        return wgConnectTunnels
            .stream()
            .filter(t -> StringUtils.equals(t.getLocalPhysInetAddr(), localPhysInetAddr) &&
                         StringUtils.equals(t.getRemotePhysInetAddr(), remotePhysInetAddr))
            .findFirst()
            .orElse(null);
    }
    
    public static PersistenceTunnel getTunnelByLocalTunnelInetAddr(String localTunnelInetAddr) {
        return wgConnectTunnels
            .stream()
            .filter(t -> StringUtils.equals(t.getLocalTunnelInetAddr(), localTunnelInetAddr))
            .findFirst()
            .orElse(null);
    }
    
    public static PersistenceTunnel getTunnelByRemoteTunnelInetAddr(String remoteTunnelInetAddr) {
        return wgConnectTunnels
            .stream()
            .filter(t -> StringUtils.equals(t.getRemoteTunnelInetAddr(), remoteTunnelInetAddr))
            .findFirst()
            .orElse(null);
    }
    
    public static PersistenceTunnel getTunnelByLocalAndRemoteTunnelInetAddrs(String localTunnelInetAddr, String remoteTunnelInetAddr) {
        return wgConnectTunnels
            .stream()
            .filter(t -> StringUtils.equals(t.getLocalTunnelInetAddr(), localTunnelInetAddr) &&
                         StringUtils.equals(t.getRemoteTunnelInetAddr(), remoteTunnelInetAddr)) 
            .findFirst()
            .orElse(null);
    }
    
    public static PersistenceTunnel getTunnelByRemotePhysAndRemoteTunnelInetAddrs(String remotePhysInetAddr, String remoteTunnelInetAddr) {
        return wgConnectTunnels
            .stream()
            .filter(t -> StringUtils.equals(t.getRemotePhysInetAddr(), remotePhysInetAddr) &&
                         StringUtils.equals(t.getRemoteTunnelInetAddr(), remoteTunnelInetAddr)) 
            .findFirst()
            .orElse(null);
    }
    
    public static PersistenceTunnel getTunnelByRemotePublicKey(String remotePublicKey) {
        return wgConnectTunnels
            .stream()
            .filter(t -> StringUtils.equals(t.getRemotePublicKey(), remotePublicKey)) 
            .findFirst()
            .orElse(null);
    }

    public static PersistenceTunnel getTunnelByLocalAndRemotePublicKeys(String localPublicKey, String remotePublicKey) {
        return wgConnectTunnels
            .stream()
            .filter(t -> StringUtils.equals(t.getLocalPublicKey(), localPublicKey) &&
                         StringUtils.equals(t.getRemotePublicKey(), remotePublicKey))
            .findFirst()
            .orElse(null);
    }
    
    public static PersistenceTunnel getTunnelByLocalTunnelInetAddrAndLocalTunnelInetComPort(String localTunnelInetAddr,
        int localTunnelInetComPort) {
        return wgConnectTunnels
            .stream()
            .filter(t -> StringUtils.equals(t.getLocalTunnelInetAddr(), localTunnelInetAddr) &&
                         t.getLocalTunnelInetComPort() == localTunnelInetComPort)
            .findFirst()
            .orElse(null);
    }

    public static PersistenceTunnel getTunnelByLocalIfName(String ifName) {
        return wgConnectTunnels
            .stream()
            .filter(t -> StringUtils.equals(t.getLocalInterfaceName(), ifName)) 
            .findFirst()
            .orElse(null);
    }
    
    public static PersistenceTunnel getTunnelByRemoteIfName(String ifName) {
        return wgConnectTunnels
            .stream()
            .filter(t -> StringUtils.equals(t.getRemoteInterfaceName(), ifName)) 
            .findFirst()
            .orElse(null);
    }
    
    public static List<PersistenceTunnel> getTunnelsByLocalPhysInetAddr(String localPhysInetAddr) {
        return wgConnectTunnels
            .stream()
            .filter(t -> StringUtils.equals(t.getLocalPhysInetAddr(), localPhysInetAddr))
            .collect(Collectors.toList());
    }
    
    public static List<PersistenceTunnel> getAllTunnels() {
        return wgConnectTunnels;
    }
    
    public static List<PersistenceTunnel> getV4Tunnels() {
        return wgConnectTunnels
            .stream()
            .filter(t -> InetAddressValidator.getInstance().isValidInet4Address(t.getLocalPhysInetAddr()))
            .collect(Collectors.toList());
    }
    
    public static List<PersistenceTunnel> getV6Tunnels() {
        return wgConnectTunnels
            .stream()
            .filter(t -> InetAddressValidator.getInstance().isValidInet6Address(t.getLocalPhysInetAddr()))
            .collect(Collectors.toList());
    }
    
    public static void removeTunnel(PersistenceTunnel tunnel) {
        wgConnectTunnels.remove(tunnel);
    }
    
    public static void updateTunnel(PersistenceTunnel tunnel) {
        
    }
    
    public static void updateTunnelsInfo() {
        Utils.getAllWgNetIfs().forEach(info -> {
            for (WgInterfaceInfo.Peer peer : info.getPeers()) {
                PersistenceTunnel tunnel = getTunnelByLocalAndRemotePublicKeys(info.getLocalPublicKey(), peer.getPublicKey());
                if (tunnel != null) {
                    updateTunnel(tunnel);
                }
            }
        });
    }
    
    private static IHelpFactory createCustomizedUsageHelp() {
        return new IHelpFactory() {
            private static final int COLUMN_REQUIRED_OPTION_MARKER_WIDTH = 2;
            private static final int COLUMN_SHORT_OPTION_NAME_WIDTH = 2;
            private static final int COLUMN_OPTION_NAME_SEPARATOR_WIDTH = 2;
            private static final int COLUMN_LONG_OPTION_NAME_WIDTH = 27;

            private static final int INDEX_REQUIRED_OPTION_MARKER = 0;
            private static final int INDEX_SHORT_OPTION_NAME = 1;
            private static final int INDEX_OPTION_NAME_SEPARATOR = 2;
            private static final int INDEX_LONG_OPTION_NAME = 3;
            private static final int INDEX_OPTION_DESCRIPTION = 4;

            @Override
            public Help create(CommandSpec commandSpec, ColorScheme colorScheme) {
                return new Help(commandSpec, colorScheme) {
                    @Override
                    public Help.Layout createDefaultLayout() {

                        // The default layout creates a TextTable with 5 columns, as follows:
                        // 0: empty text or (if configured) the requiredOptionMarker character
                        // 1: short option name
                        // 2: comma separator (if option has both short and long option)
                        // 3: long option name(s)
                        // 4: option description
                        //
                        // The code below creates a TextTable with 3 columns, as follows:
                        // 0: empty text or (if configured) the requiredOptionMarker character
                        // 1: all option names, comma-separated if necessary
                        // 2: option description

                        int optionNamesColumnWidth = COLUMN_SHORT_OPTION_NAME_WIDTH +
                                COLUMN_OPTION_NAME_SEPARATOR_WIDTH +
                                COLUMN_LONG_OPTION_NAME_WIDTH;

                        Help.TextTable table = Help.TextTable.forColumnWidths(colorScheme,
                                COLUMN_REQUIRED_OPTION_MARKER_WIDTH,
                                optionNamesColumnWidth,
                                commandSpec.usageMessage().width() - (optionNamesColumnWidth + COLUMN_REQUIRED_OPTION_MARKER_WIDTH));
                        Help.Layout result = new Help.Layout(colorScheme,
                                table,
                                createDefaultOptionRenderer(),
                                createDefaultParameterRenderer()) {
                            @Override
                            public void layout(ArgSpec argSpec, Help.Ansi.Text[][] cellValues) {

                                // The default option renderer produces 5 Text values for each option.
                                // Below we combine the short option name, comma separator and long option name
                                // into a single Text object, and we pass 3 Text values to the TextTable.
                                for (Help.Ansi.Text[] original : cellValues) {
                                    if (original[INDEX_OPTION_NAME_SEPARATOR].getCJKAdjustedLength() > 0) {
                                        original[INDEX_OPTION_NAME_SEPARATOR] = original[INDEX_OPTION_NAME_SEPARATOR].concat(" ");
                                    }
                                    Help.Ansi.Text[] threeColumns = new Help.Ansi.Text[] {
                                            original[INDEX_REQUIRED_OPTION_MARKER],
                                            original[INDEX_SHORT_OPTION_NAME]
                                                    .concat(original[INDEX_OPTION_NAME_SEPARATOR])
                                                    .concat(original[INDEX_LONG_OPTION_NAME]),
                                            original[INDEX_OPTION_DESCRIPTION],
                                    };
                                    table.addRowValues(threeColumns);
                                }
                            }
                        };
                        return result;
                    }
                };
            }
        };
    }
    
    @Override
    public void run() {
        System.out.println("Starting " + WG_CONNECT_MACHINES + ": " + new Date());
        System.out.println(Version.getVersion());

        spec.commandLine().setHelpFactory(createCustomizedUsageHelp());

        // Check the command line arguments for the help and version options
        ParseResult pr = spec.commandLine().getParseResult();
        if (pr.isUsageHelpRequested()) {
            spec.commandLine().usage(System.out);
            System.exit(0);
        } else if (pr.isVersionHelpRequested()) {
            spec.commandLine().printVersionHelp(System.out);
            System.exit(0);
        }
        
        startMachines();
    }

    /**
     * The main method.
     *
     * @param args the arguments
     */
    public static void main(String... args) {
        log.setDebugEnabled(false);
                
        if (args == null || args.length == 0) {
            args = DEFAULT_OPTIONS;
        }
        
        new CommandLine(new WgConnect()).execute(args);
    }
}
