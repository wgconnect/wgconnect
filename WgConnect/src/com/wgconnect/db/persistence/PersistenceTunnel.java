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
package com.wgconnect.db.persistence;

import com.wgconnect.core.util.WgConnectLogger;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.net.InetSocketAddress;
import java.sql.Timestamp;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;

/**
 * PersistenceTunnel
 *
 * The persistenceTunnel class.
 * 
 * @author: wgconnect@proton.me
 */
@Entity
@Table(name = PersistenceTunnel.PERSISTENCE_TABLE_NAME)
@NamedQuery(query = PersistenceTunnel.QUERY_STATEMENT_ALL_TUNNELS,
    name = PersistenceTunnel.QUERY_NAME_ALL_TUNNELS)
@NamedQuery(query = PersistenceTunnel.QUERY_STATEMENT_LOCAL_ENDPOINT_TYPE,
    name = PersistenceTunnel.QUERY_NAME_LOCAL_ENDPOINT_TYPE)
public class PersistenceTunnel implements Serializable {
    private static final long serialVersionUID = 1L;

    private static final WgConnectLogger log = WgConnectLogger.getLogger(PersistenceTunnel.class);

    public static final String V6_INTERFACE_SUFFIX_DELIMITER = "%";

    public static final String PERSISTENCE_TABLE_NAME = "Tunnels";
    
    public static final String QUERY_NAME_ALL_TUNNELS = "all tunnels";
    public static final String QUERY_STATEMENT_ALL_TUNNELS = "Select t from PersistenceTunnel t";
    
    public static final String QUERY_NAME_LOCAL_ENDPOINT_TYPE = "localEndpointType";
    public static final String QUERY_STATEMENT_LOCAL_ENDPOINT_TYPE = "Select t from PersistenceTunnel t where t.localEndpointType = :type";
    
    @Id
    @Column(nullable=false)
    private UUID id;
    private int remoteId;
    
    private String inetType;
        
    private String remoteEndpointType;
    private String localEndpointType;
    
    private String remotePhysInetAddr;
    private long remotePhysInetListenPort;
    private long remotePhysInetComPort;
    
    private String localPhysInetAddr;
    private long localPhysInetListenPort;
    private long localPhysInetComPort;
 
    private String remoteTunnelInetAddr;
    private long remoteTunnelInetComPort;
    private String tunnelInetNet;
    
    private String localTunnelInetAddr;
    private long localTunnelInetComPort;
    
    private InetSocketAddress localTunnelInetSockAddr;
    
    private String remoteInterfaceName;
    private String localInterfaceName;
    
    private String remotePublicKey;
    private String localPublicKey;
    private String localPrivateKey;
    private String localPreSharedKey;
        
    private String fwmark;
    
    private long latestHandshake = 0;
    private float receivedBandwidth = 0;
    private float sentBandwidth = 0;
    private int keepAlive = 0;
    
    private Timestamp datestamp;
    private String state;
    
    private boolean isConfigured;
        
    public PersistenceTunnel() {}

    public void setId(UUID id) {
        this.id = id;
    }
    
    public UUID getId() {
        return id;
    }
    
    public void setRemoteId(int remoteId) {
        this.remoteId = remoteId;
    }
    
    public int getRemoteId() {
        return remoteId;
    }
    
    public void setInetType(String inetType) {
        this.inetType = inetType;
    }
    
    public String getInetType() {
        return inetType;
    }
    
    public void setRemoteEndpointType(String type) {
        remoteEndpointType = type;
    }
    
    public String getRemoteEndpointType() {
        return remoteEndpointType;
    }
    
    public void setLocalEndpointType(String type) {
        localEndpointType = type;
    }
    
    public String getLocalEndpointType() {
        return localEndpointType;
    }
    
    public void setLocalPhysInetAddr(String addr) {
        String[] addrArray = StringUtils.split(addr, V6_INTERFACE_SUFFIX_DELIMITER);
        if (addrArray != null) {
            localPhysInetAddr = addrArray[0];
        } else {
            localPhysInetAddr = addr;            
        }
    }
    
    public String getLocalPhysInetAddr() {
        return localPhysInetAddr;
    }
    
    public void setLocalPhysInetListenPort(long port) {
        localPhysInetListenPort = port;
    }
    
    public long getLocalPhysInetListenPort() {
        return localPhysInetListenPort;
    }
    
    public void setLocalPhysInetComPort(long port) {
        localPhysInetComPort = port;
    }
    
    public long getLocalPhysInetComPort() {
        return localPhysInetComPort;
    }
    
    public void setRemotePhysInetAddr(String addr) {
        String[] addrArray = StringUtils.split(addr, V6_INTERFACE_SUFFIX_DELIMITER);
        if (addrArray != null) {
            remotePhysInetAddr = addrArray[0];
        } else {
            remotePhysInetAddr = addr;            
        }
    }
    
    public String getRemotePhysInetAddr() {
        return remotePhysInetAddr;
    }
    
    public void setRemotePhysInetListenPort(long port) {
        remotePhysInetListenPort = port;
    }
    
    public long getRemotePhysInetListenPort() {
        return remotePhysInetListenPort;
    }
    
    public void setRemotePhysInetComPort(long port) {
        remotePhysInetComPort = port;
    }
    
    public long getRemotePhysInetComPort() {
        return remotePhysInetComPort;
    }
    
    public void setLocalTunnelInetAddr(String addr) {
        localTunnelInetAddr = addr;
    }
    
    public String getLocalTunnelInetAddr() {
        return localTunnelInetAddr;
    }
    
    public void setLocalTunnelInetComPort(long port) {
        localTunnelInetComPort = port;
    }
    
    public long getLocalTunnelInetComPort() {
        return localTunnelInetComPort;
    }
    
    public void setRemoteTunnelInetAddr(String addr) {
        remoteTunnelInetAddr = addr;
    }
    
    public String getRemoteTunnelInetAddr() {
        return remoteTunnelInetAddr;
    }
    
    public void setTunnelInetNet(String net) {
        tunnelInetNet = net;
    }
    
    public String getTunnelInetNet() {
        return tunnelInetNet;
    }
    
    public void setRemoteTunnelInetComPort(long port) {
        remoteTunnelInetComPort = port;
    }
    
    public long getRemoteTunnelInetComPort() {
        return remoteTunnelInetComPort;
    }
    
    public void setLocalTunnelInetSockAddr(InetSocketAddress addr) {
        localTunnelInetSockAddr = addr;
    }
    
    public InetSocketAddress getLocalTunnelInetSockAddr() {
        return localTunnelInetSockAddr;
    }
    
    public void setLocalInterfaceName(String name) {
        localInterfaceName = name;
    }
    
    public String getLocalInterfaceName() {
        return localInterfaceName;
    }
    
    public void setRemoteInterfaceName(String name) {
        remoteInterfaceName = name;
    }
    
    public String getRemoteInterfaceName() {
        return remoteInterfaceName;
    }
    
    public void setLocalPublicKey(String key) {
        localPublicKey = key;
    }
    
    public String getLocalPublicKey() {
        return localPublicKey;
    }
   
    public void setRemotePublicKey(String key) {
        remotePublicKey = key;
    }
    
    public String getRemotePublicKey() {
        return remotePublicKey;
    }

    public void setLocalPrivateKey(String key) {
        localPrivateKey = key;
    }
    
    public String getLocalPrivateKey() {
        return localPrivateKey;
    }
    
    public void setLocalPreSharedKey(String key) {
        localPreSharedKey = key;
    }
    
    public String getLocalPreSharedKey() {
        return localPreSharedKey;
    }
    
    public void setLatestHandshake(long handshake) {
        latestHandshake = handshake;
    }
    
    public long getLatestHandshake() {
        return latestHandshake;
    }
    
    public void setReceivedBandwidth(float bandwidth) {
        receivedBandwidth = bandwidth;
    }
    
    public float getReceivedBandwidth() {
        return receivedBandwidth;
    }
    
    public void setSentBandwidth(float bandwidth) {
        sentBandwidth = bandwidth;
    }
    
    public float getSentBandwidth() {
        return sentBandwidth;
    }
    
    public void setFwmark(String mark) {
        fwmark = mark;
    }
    
    public String getFwmark() {
        return fwmark;
    }
    
    public void setKeepalive(int interval) {
        keepAlive = interval;
    }
    
    public int getKeepAlive() {
        return keepAlive;
    }
    
    public void setTimestamp(Timestamp time) {
        datestamp = time;
    }
    
    public Timestamp getTimestamp() {
        return datestamp;
    }

    public void setState(String value) {
        state = value;
    }
    
    public String getState() {
        return state;
    }
    
    public void setIsConfigured(boolean value) {
        isConfigured = value;
    }
    
    public boolean getIsConfigured() {
        return isConfigured;
    }
    
    @Override
    public int hashCode() {
        int hash = 0;
        hash += (id != null ? id.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof PersistenceTunnel)) {
            return false;
        }
        
        PersistenceTunnel other = (PersistenceTunnel) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        
        return true;
    }

    @Override
    public String toString() {
        return "PersistenceTunnel[ id=" + id + " ]";
    }
}
