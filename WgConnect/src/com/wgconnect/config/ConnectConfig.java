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
package com.wgconnect.config;

import com.wgconnect.WgConnect;
import com.wgconnect.core.message.V4Message;
import com.wgconnect.core.message.V6Message;
import com.wgconnect.core.option.machine.LocalTunnelInetAddrOption;
import com.wgconnect.core.option.machine.RemoteTunnelInetAddrOption;
import com.wgconnect.core.util.Constants;
import com.wgconnect.core.util.WgConnectLogger;
import com.wgconnect.core.util.Utils;
import com.wgconnect.db.persistence.PersistenceTunnel;
import com.wgconnect.db.h2.PersistenceDatabaseManagerH2;
import com.wgconnect.db.PersistenceDatabaseManagerImpl;
import com.wgconnect.db.PersistenceDatabaseManager;
import com.wgconnect.gui.Gui;

import com.wgtools.Show;
import com.wgtools.Wg;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;

import java.io.File;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.security.KeyPair;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Random;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.routines.InetAddressValidator;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.H2Dialect;

/**
 * ConnectConfiguration
 * 
 * The connect configuration.
 * 
 * @author JagornetDhcp version: A. Gregory Rabil
 * @author WgConnect version: wgconnect@proton.me
 */
public class ConnectConfig {
    
    private static final WgConnectLogger log = WgConnectLogger.getLogger(ConnectConfig.class);

    private static ConnectConfig INSTANCE;
                
    private PersistenceDatabaseManager databaseMgr;
    private List<Class> databaseMgrEntityClasses;
    private final Properties databaseProperties = new Properties();
    
    private List<PersistenceTunnel> wgConnectTunnels;
    private final KeyPair genericKeyPair = Utils.generateCryptoKeyPair(
        Constants.GENERIC_CRYPTO_ALGORITHM, Constants.GENERIC_CRYPTO_KEYSIZE);
    
    private static final InetAddressValidator validator = InetAddressValidator.getInstance();

    private final Random random = new Random();

    /**
     * Gets the single instance of ConnectConfig.
     *
     * @return single instance of ConnectConfig
     */
    public static synchronized ConnectConfig getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new ConnectConfig();
        }
        
        return INSTANCE;
    }

    private ConnectConfig() {}

    public PersistenceDatabaseManager getDatabaseMgr() {
        return databaseMgr;
    }
    
    public void init() throws ConfigException, IOException {
        if (WgConnect.getDatabaseEnabled()) {
            databaseMgr = new PersistenceDatabaseManagerH2();

            initDatabaseProperties();
            File[] foundFiles = new File(Constants.DATABASE_DIRECTORY).listFiles((File file) -> {
                return file.getName().contains(Constants.DATABASE_FILENAME);
            });

            if (foundFiles.length == 0) {
                databaseProperties.put(AvailableSettings.HBM2DDL_AUTO, "create");
                databaseMgr.init(databaseProperties, databaseMgrEntityClasses);
                databaseMgr.closeEntityManager();
            }

            databaseProperties.put(AvailableSettings.HBM2DDL_AUTO, "validate");
            databaseMgr.init(databaseProperties, databaseMgrEntityClasses);
            
            databaseMgrEntityClasses = new ArrayList<>();
            databaseMgrEntityClasses.add(PersistenceTunnel.class);
        } else {
            databaseMgr = new PersistenceDatabaseManagerImpl();
        }
    }

    public KeyPair getGenericKeyPair() {
        return genericKeyPair;
    }
    
    private void initDatabaseProperties() {
        if (WgConnect.getDatabaseEnabled()) {
            databaseProperties.put(AvailableSettings.DIALECT, "org.hibernate.dialect.H2Dialect");
            databaseProperties.put("hibernate.id.new_generator_mappings", false);
            databaseProperties.put(AvailableSettings.JAKARTA_JDBC_DRIVER, databaseMgr.getJdbcDriver());
            databaseProperties.put(AvailableSettings.JAKARTA_JDBC_URL, databaseMgr.getJdbcConnectionUrl());
            databaseProperties.put(AvailableSettings.JAKARTA_JDBC_USER, WgConnect.getDatabaseUsername());
            databaseProperties.put(AvailableSettings.JAKARTA_JDBC_PASSWORD, WgConnect.getDatabasePassword());
            databaseProperties.put(AvailableSettings.DIALECT, H2Dialect.class);
            databaseProperties.put(AvailableSettings.SHOW_SQL, true);
            databaseProperties.put(AvailableSettings.FORMAT_SQL, true);
        }
    }
    
    public List<PersistenceTunnel> initTunnelsList() {
        if (wgConnectTunnels == null) {
            wgConnectTunnels = new ArrayList();
        }
        
        List<PersistenceTunnel> persistenceTunnels = new ArrayList<>();
        EntityManager em = databaseMgr.getEntityManager();
        
        if (em != null) {
            TypedQuery<PersistenceTunnel> query = em.createNamedQuery(PersistenceTunnel.QUERY_NAME_ALL_TUNNELS, PersistenceTunnel.class);
            for (PersistenceTunnel t : query.getResultList()) {
                persistenceTunnels.add(t);
            }
        }
        
        // Check for existing V4/V6 Wireguard tunnels
        List<PersistenceTunnel> nettyLocalAddons = new ArrayList<>();
        Wg wg = new Wg();
        wg.executeSubcommand(Show.COMMAND, Wg.OPTION_INTERFACES);
        if (wg.getCommandExitCode() == Wg.getCommandSuccessCode() && wg.getCommandOutputString() != null &&
            !wg.getCommandOutputString().isBlank()) {

            try {
                Enumeration<NetworkInterface> netIfs = NetworkInterface.getNetworkInterfaces();
                while (netIfs.hasMoreElements()) {
                    NetworkInterface netIf = netIfs.nextElement();
                    if (netIf.getDisplayName().startsWith(Constants.TUNNEL_V6_IF_NAME_PREFIX)) {
                        String inetAddr = null;
                        Enumeration<InetAddress> inetAddrs = netIf.getInetAddresses();
                        while (inetAddrs.hasMoreElements()) {
                            InetAddress currentAddress = inetAddrs.nextElement();
                            if (currentAddress instanceof Inet6Address && !currentAddress.isLoopbackAddress()) {
                                inetAddr = currentAddress.getHostAddress();
                                break;
                            }
                        }

                        String localTunnelInetAddr = inetAddr;
                        String privateKey = wg.getPrivateKeyFromInterface(netIf.getDisplayName());
                        String publicKey = wg.getPublicKeyFromInterface(netIf.getDisplayName());
                        int listenPort = wg.getListenPortFromInterface(netIf.getDisplayName());
                        List<String> peers = wg.getPeersFromInterface(netIf.getDisplayName());
                        Map<String, String> endpoints = wg.getEndpointsAsMap(netIf.getDisplayName());
                        Map<String, String> allowedIps = wg.getAllowedIpsAsMap(netIf.getDisplayName());
                        Map<String, String> persistentKeepalives = wg.getPersistentKeepalivesAsMap(netIf.getDisplayName());

                        if (privateKey != null && publicKey != null && listenPort >= 0 && !peers.isEmpty() && !endpoints.isEmpty() &&
                            !allowedIps.isEmpty() && !persistentKeepalives.isEmpty()) {

                            // Check if the persistent tunnels are actual exisiting tunnels
                            for (PersistenceTunnel t : persistenceTunnels) {
                                Optional<String> value = endpoints.values()
                                    .stream()
                                    .filter(v -> v.contains(t.getRemotePhysInetAddr()))
                                    .findFirst();

                                if (value.isEmpty()) {
                                    persistenceTunnels.remove(t);
                                }
                            }

                            // Perform a more detailed anaylsis of the persistence tunnels
                            for (String peer : peers) {
                                String endpoint = endpoints.get(peer);
                                String[] remotePhysInetAddrInfo = StringUtils.split(endpoint, ":");

                                String allowedIp = allowedIps.get(peer);
                                String[] remoteTunnelInetAddrInfo = allowedIp.split("/");

                                String persistentKeepalive = persistentKeepalives.get(peer);

                                // Check if this tunnel is in the persistence database
                                Optional<PersistenceTunnel> pt = persistenceTunnels
                                    .stream()
                                    .filter(t ->
                                        t.getInetType().equalsIgnoreCase(Constants.IPVersion.V6.toString()) &&
                                        StringUtils.equals(t.getLocalPrivateKey(), privateKey) &&
                                        StringUtils.equals(t.getLocalPublicKey(), publicKey) &&
                                        t.getLocalPhysInetListenPort() == listenPort &&
                                        StringUtils.equals(t.getRemotePublicKey(), peer) &&
                                        StringUtils.equals(t.getRemotePhysInetAddr(), remotePhysInetAddrInfo[0]) &&
                                        t.getRemotePhysInetListenPort() == Long.parseLong(remotePhysInetAddrInfo[1]) &&
                                        StringUtils.equals(t.getRemoteTunnelInetAddr(), remoteTunnelInetAddrInfo[0]) &&
                                        StringUtils.equals(t.getLocalTunnelInetAddr(), localTunnelInetAddr))
                                    .findFirst();

                                if (pt.isPresent()) {
                                    Optional<PersistenceTunnel> addon = nettyLocalAddons
                                        .stream()
                                        .filter(t ->
                                            StringUtils.equals(t.getLocalTunnelInetAddr(), pt.get().getLocalTunnelInetAddr()) &&
                                            t.getLocalTunnelInetComPort() == pt.get().getLocalTunnelInetComPort())
                                        .findAny();

                                    if (addon.isEmpty()) {
                                        nettyLocalAddons.add(pt.get());
                                    }

                                    wgConnectTunnels.add(pt.get());
                                    persistenceTunnels.remove(pt.get());

                                    continue;

                                }

                                PersistenceTunnel tunnel = new PersistenceTunnel();
                                tunnel.setId(UUID.randomUUID());
                                tunnel.setInetType(Constants.IPVersion.V6.toString());

                                tunnel.setRemoteEndpointType(Constants.TUNNEL_ENDPOINT_TYPE_CLIENT);
                                tunnel.setLocalEndpointType(Constants.TUNNEL_ENDPOINT_TYPE_CLIENT);

                                tunnel.setLocalPhysInetComPort(WgConnect.getV6PortNumber());
                                tunnel.setLocalPhysInetListenPort(listenPort);

                                tunnel.setRemoteTunnelInetAddr(remoteTunnelInetAddrInfo[0]);

                                tunnel.setLocalPrivateKey(privateKey);
                                tunnel.setLocalPublicKey(publicKey);
                                tunnel.setRemotePublicKey(peer);

                                tunnel.setKeepalive(Integer.parseInt(persistentKeepalives.get(persistentKeepalive)));

                                tunnel.setState(Constants.TUNNEL_STATUS_UP);
                                Gui.addTunnel(tunnel);

                                if (validator.isValid(remotePhysInetAddrInfo[0])) {
                                    tunnel.setRemotePhysInetAddr(remotePhysInetAddrInfo[0]);
                                    tunnel.setRemotePhysInetListenPort(Long.parseLong(remotePhysInetAddrInfo[1]));

                                    String serverPhysInetAddr = wg.getLinkDeviceManager().getLocalEndpointForInetAddr(remotePhysInetAddrInfo[0]);
                                    if (validator.isValid(serverPhysInetAddr)) {
                                        tunnel.setLocalPhysInetAddr(serverPhysInetAddr);
                                    }
                                }
                            }
                        }
                    }
                    
                    if (netIf.getDisplayName().startsWith(Constants.TUNNEL_V4_IF_NAME_PREFIX)) {
                        String inetAddr = null;
                        Enumeration<InetAddress> inetAddress = netIf.getInetAddresses();
                        while (inetAddress.hasMoreElements()) {
                            InetAddress currentAddress = inetAddress.nextElement();
                            if (currentAddress instanceof Inet4Address && !currentAddress.isLoopbackAddress()) {
                                inetAddr = currentAddress.getHostAddress();
                                break;
                            }
                        }

                        String serverTunnelInetAddr = inetAddr;
                        String privateKey = wg.getPrivateKeyFromInterface(netIf.getDisplayName());
                        String publicKey = wg.getPublicKeyFromInterface(netIf.getDisplayName());
                        int listenPort = wg.getListenPortFromInterface(netIf.getDisplayName());
                        List<String> peers = wg.getPeersFromInterface(netIf.getDisplayName());
                        Map<String, String> endpoints = wg.getEndpointsAsMap(netIf.getDisplayName());
                        Map<String, String> allowedIps = wg.getAllowedIpsAsMap(netIf.getDisplayName());
                        Map<String, String> persistentKeepalives = wg.getPersistentKeepalivesAsMap(netIf.getDisplayName());

                        if (privateKey != null && publicKey != null && listenPort >= 0 && !peers.isEmpty() && !endpoints.isEmpty() &&
                            !allowedIps.isEmpty() && !persistentKeepalives.isEmpty()) {

                            // Check if the persistent server tunnels are actual exisiting tunnels
                            for (PersistenceTunnel t : persistenceTunnels) {
                                Optional<String> value = endpoints.values()
                                    .stream()
                                    .filter(v -> v.contains(t.getRemotePhysInetAddr()))
                                    .findFirst();

                                if (value.isEmpty()) {
                                    persistenceTunnels.remove(t);
                                }
                            }

                            for (String peer : peers) {
                                String endpoint = endpoints.get(peer);
                                String[] clientPhysInetAddrInfo = StringUtils.split(endpoint, ":");

                                String allowedIp = allowedIps.get(peer);
                                String[] clientTunnelInetAddrInfo = allowedIp.split("/");

                                String persistentKeepalive = persistentKeepalives.get(peer);

                                // Check if this wg tunnel is in the persistence database
                                Optional<PersistenceTunnel> pt = persistenceTunnels
                                    .stream()
                                    .filter(t ->
                                        t.getInetType().equalsIgnoreCase(Constants.IPVersion.V4.toString()) &&
                                        StringUtils.equals(t.getLocalPrivateKey(), privateKey) &&
                                        StringUtils.equals(t.getLocalPublicKey(), publicKey) &&
                                        t.getLocalPhysInetListenPort() == listenPort &&
                                        StringUtils.equals(t.getRemotePublicKey(), peer) &&
                                        StringUtils.equals(t.getRemotePhysInetAddr(), clientPhysInetAddrInfo[0]) &&
                                        t.getRemotePhysInetListenPort() == Long.parseLong(clientPhysInetAddrInfo[1]) &&
                                        StringUtils.equals(t.getRemoteTunnelInetAddr(), clientTunnelInetAddrInfo[0]) &&
                                        StringUtils.equals(t.getLocalTunnelInetAddr(), serverTunnelInetAddr))
                                    .findFirst();

                                if (pt.isPresent()) {
                                    Optional<PersistenceTunnel> addon = nettyLocalAddons
                                        .stream()
                                        .filter(t ->
                                            StringUtils.equals(t.getLocalTunnelInetAddr(), pt.get().getLocalTunnelInetAddr()) &&
                                            t.getLocalTunnelInetComPort() == pt.get().getLocalTunnelInetComPort())
                                        .findAny();

                                    if (addon.isEmpty()) {
                                        nettyLocalAddons.add(pt.get());
                                    }

                                    wgConnectTunnels.add(pt.get());
                                    persistenceTunnels.remove(pt.get());

                                    continue;
                                }

                                PersistenceTunnel tunnel = new PersistenceTunnel();
                                tunnel.setId(UUID.randomUUID());
                                tunnel.setInetType(Constants.IPVersion.V4.toString());

                                tunnel.setRemoteEndpointType(Constants.TUNNEL_ENDPOINT_TYPE_CLIENT);
                                tunnel.setLocalEndpointType(Constants.TUNNEL_ENDPOINT_TYPE_SERVER);

                                tunnel.setLocalPhysInetComPort(WgConnect.getV6PortNumber());
                                tunnel.setLocalPhysInetListenPort(listenPort);

                                tunnel.setRemoteTunnelInetAddr(clientTunnelInetAddrInfo[0]);

                                tunnel.setLocalPrivateKey(privateKey);
                                tunnel.setLocalPublicKey(publicKey);
                                tunnel.setRemotePublicKey(peer);

                                tunnel.setKeepalive(Integer.parseInt(persistentKeepalives.get(persistentKeepalive)));

                                tunnel.setState(Constants.TUNNEL_STATUS_UP);
                                Gui.addTunnel(tunnel);

                                if (validator.isValid(clientPhysInetAddrInfo[0])) {
                                    tunnel.setRemotePhysInetAddr(clientPhysInetAddrInfo[0]);
                                    tunnel.setRemotePhysInetListenPort(Long.parseLong(clientPhysInetAddrInfo[1]));

                                    String serverPhysInetAddr = wg.getLinkDeviceManager().getLocalEndpointForInetAddr(clientPhysInetAddrInfo[0]);
                                    if (validator.isValid(serverPhysInetAddr)) {
                                        tunnel.setLocalPhysInetAddr(serverPhysInetAddr);
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (SocketException ex) {
                log.error("SocketException: " + ex);
            }
        }
        
        return nettyLocalAddons;
    }
    
    public V4Message buildV4TunnelsInfoRequestMessage(PersistenceTunnel localTunnel, InetSocketAddress remoteSockInetAddr) {
        InetSocketAddress localTunnelInetSockAddr = new InetSocketAddress(localTunnel.getLocalTunnelInetAddr(),
            (int) localTunnel.getLocalTunnelInetComPort());
        InetSocketAddress remoteTunnelInetSockAddr = new InetSocketAddress(localTunnel.getRemoteTunnelInetAddr(),
            (int) localTunnel.getRemoteTunnelInetComPort());
        
        V4Message msg = new V4Message(localTunnelInetSockAddr, remoteTunnelInetSockAddr);
        
        msg.setOp((short) Constants.V4_OP_REQUEST);
        msg.setTransactionId(random.nextLong());
        msg.setHtype((short) 1);
        msg.setClientAddr(localTunnelInetSockAddr.getAddress());
        msg.setClientPort(localTunnelInetSockAddr.getPort());
        msg.setServerAddr(remoteSockInetAddr.getAddress());
        msg.setServerPort(remoteSockInetAddr.getPort());
        
        msg.setMessageType((short) Constants.V4_MESSAGE_TYPE_INFO_REQUEST);

        msg.putOption(new LocalTunnelInetAddrOption(localTunnel.getRemoteTunnelInetAddr(), true));
        msg.putOption(new RemoteTunnelInetAddrOption(localTunnel.getLocalTunnelInetAddr(), true));
        
        return msg;
    }

    public V6Message buildV6TunnelsInfoRequestMessage(PersistenceTunnel tunnel) {
        InetSocketAddress serverTunnelInetSocketAddr = new InetSocketAddress(tunnel.getLocalTunnelInetAddr(), (int) tunnel.getLocalTunnelInetComPort());
        InetSocketAddress clientTunnelInetSocketAddr = new InetSocketAddress(tunnel.getRemoteTunnelInetAddr(), (int) tunnel.getRemoteTunnelInetComPort());
        
        V6Message msg = new V6Message(serverTunnelInetSocketAddr, clientTunnelInetSocketAddr);
        
        msg.setMessageType(Constants.V6_MESSAGE_TYPE_INFO_REQUEST);
        msg.setTransactionId(random.nextInt());

        msg.putOption(new LocalTunnelInetAddrOption(tunnel.getRemoteTunnelInetAddr(), false));
        msg.putOption(new RemoteTunnelInetAddrOption(tunnel.getLocalTunnelInetAddr(), false));
        
        return msg;
    }
    
    public void updatePersistenceDatabase(PersistenceTunnel tunnel) {
        databaseMgr.updateEntity(tunnel);
    }
}
