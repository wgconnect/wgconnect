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
 * along with WireguardDhcp.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.wgconnect.gui;

import com.wgconnect.core.util.WgConnectLogger;
import com.wgconnect.db.persistence.PersistenceTunnel;

import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Polygon;

import org.apache.commons.lang3.StringUtils;

/**
 * TunnelRow
 *
 * The TunnelRow class.
 * 
 * @author: wgconnect@proton.me
 */
public class TunnelRow extends HBox {
    
    private static final WgConnectLogger log = WgConnectLogger.getLogger(TunnelRow.class);

    public static final String HEADER_LOCAL_TUNNEL_ADDR = "Local Tunnel Addr";
    public static final int INDEX_LOCAL_TUNNEL_ADDR = 0;
    public static final String HEADER_REMOTE_TUNNEL_ADDR = "Remote Tunnel Addr";
    public static final int INDEX_REMOTE_TUNNEL_ADDR = 1;

    public static final String HEADER_LOCAL_ENDPOINT = "Local Endpoint";
    public static final int INDEX_LOCAL_ENDPOINT = 0;
    public static final String HEADER_REMOTE_ENDPOINT = "Remote Endpoint";
    public static final int INDEX_REMOTE_ENDPOINT = 1;
    public static final String ENDPOINT_SEPARATOR = ":";

    public static final String HEADER_LOCAL_PUBLIC_KEY = "Local Public Key";
    public static final int INDEX_LOCAL_PUBLIC_KEY = 0;
    public static final String HEADER_REMOTE_PUBLIC_KEY = "Remote Public Key";
    public static final int INDEX_REMOTE_PUBLIC_KEY = 1;
        
    public static final String HEADER_STATE = "State";
    public static final int INDEX_STATE = 0;
    public static final String HEADER_KEEPALIVE = "Keepalive";
    public static final int INDEX_KEEPALIVE = 1;

    public static final String HEADER_RECEIVED_BANDWIDTH = "Transfer Received";
    public static final int INDEX_RECEIVED_BANDWIDTH = 0;
    public static final String HEADER_SENT_BANDWIDTH = "Transfer Sent";
    public static final int INDEX_SENT_BANDWIDTH = 1;

    public static final String HEADER_LATEST_HANDSHAKE = "Latest Handshake";
    public static final int INDEX_LATEST_HANDSHAKE = 0;
    public static final String HEADER_FWMARK = "Fwmark";
    public static final int INDEX_FWMARK = 1;
    
    public static final String HEADER_LOCAL_INTERFACE = "Local Interface";
    public static final int INDEX_LOCAL_INTERFACE = 0;
    public static final String HEADER_REMOTE_INTERFACE = "Remote Interface";
    public static final int INDEX_REMOTE_INTERFACE = 1;
    
    private static final double[] ROW_SEPARATOR_SHAPE_DIAMOND = { 10.0, 3.0, 14.0, 6.0, 10.0, 9.0, 6.0, 6.0 };

    private final double windowWidth;
    private final String colorString;
    private final Paint colorPaint;
    
    private final PersistenceTunnel tunnel;
    
    private ColumnBox tunnelAddrsBox;
    private ElementBox localTunnelAddrElement;
    private ElementBox remoteTunnelAddrElement;
    
    private ColumnBox endpointsBox; 
    private ElementBox localEndpointElement;
    private ElementBox remoteEndpointElement;
    
    private ColumnBox publicKeysBox;
    private ElementBox localPublicKeyElement;
    private ElementBox remotePublicKeyElement;
    
    private ColumnBox statusBox;
    private ElementBox stateElement;
    private ElementBox keepaliveElement;

    private ColumnBox bandwidthsBox;
    private ElementBox receivedBandwidthElement;
    private ElementBox sentBandwidthElement;
    
    private ColumnBox markersBox;
    private ElementBox latestHandshakeElement;
    private ElementBox fwmarkElement;
    
    private ColumnBox interfacesBox;
    private ElementBox localInterfaceName;
    private ElementBox remoteInterfaceName;
    
    public TunnelRow(PersistenceTunnel tunnel, double windowWidth, String color) {
        this.tunnel = tunnel;
        this.windowWidth = windowWidth;
        
        this.colorString = color;
        this.colorPaint = Color.valueOf(color);
        
        setUserData(tunnel.getId());
    }
    
    public TunnelRow init() {        
        tunnelAddrsBox = new ColumnBox(this, Gui.COLUMN_INDEX_TUNNEL_ADDRS);
        localTunnelAddrElement = new ElementBox(tunnelAddrsBox, HEADER_LOCAL_TUNNEL_ADDR, tunnel.getLocalTunnelInetAddr(), colorString);
        remoteTunnelAddrElement = new ElementBox(tunnelAddrsBox, HEADER_REMOTE_TUNNEL_ADDR, tunnel.getRemoteTunnelInetAddr(), colorString);
        Polygon separator = new Polygon(ROW_SEPARATOR_SHAPE_DIAMOND);
        separator.setFill(colorPaint);
        tunnelAddrsBox.addElement(localTunnelAddrElement, INDEX_LOCAL_TUNNEL_ADDR);
        tunnelAddrsBox.addElement(remoteTunnelAddrElement, INDEX_REMOTE_TUNNEL_ADDR);
        tunnelAddrsBox.addRowSeparator(separator);
        getChildren().add(tunnelAddrsBox);
               
        endpointsBox = new ColumnBox(this, Gui.COLUMN_INDEX_ENDPOINTS);
        localEndpointElement = new ElementBox(endpointsBox, HEADER_LOCAL_ENDPOINT,
            tunnel.getLocalPhysInetAddr() + ENDPOINT_SEPARATOR + tunnel.getLocalPhysInetListenPort(), colorString);
        remoteEndpointElement = new ElementBox(endpointsBox, HEADER_REMOTE_ENDPOINT,
            tunnel.getRemotePhysInetAddr() + ENDPOINT_SEPARATOR + tunnel.getRemotePhysInetListenPort(), colorString);
        separator = new Polygon(ROW_SEPARATOR_SHAPE_DIAMOND);
        separator.setFill(colorPaint);
        endpointsBox.addElement(localEndpointElement, INDEX_LOCAL_ENDPOINT);
        endpointsBox.addElement(remoteEndpointElement, INDEX_REMOTE_ENDPOINT);
        endpointsBox.addRowSeparator(separator);
        getChildren().add(endpointsBox);
        
        publicKeysBox = new ColumnBox(this, Gui.COLUMN_INDEX_PUBLIC_KEYS);
        localPublicKeyElement = new ElementBox(publicKeysBox, HEADER_LOCAL_PUBLIC_KEY, tunnel.getLocalPublicKey(), colorString);
        remotePublicKeyElement = new ElementBox(publicKeysBox, HEADER_REMOTE_PUBLIC_KEY, tunnel.getRemotePublicKey(), colorString);
        separator = new Polygon(ROW_SEPARATOR_SHAPE_DIAMOND);
        separator.setFill(colorPaint);
        publicKeysBox.addElement(localPublicKeyElement, INDEX_LOCAL_PUBLIC_KEY);
        publicKeysBox.addElement(remotePublicKeyElement, INDEX_REMOTE_PUBLIC_KEY);
        publicKeysBox.addRowSeparator(separator);
        getChildren().add(publicKeysBox);
        
        statusBox = new ColumnBox(this, Gui.COLUMN_INDEX_STATUS);
        stateElement = new ElementBox(statusBox, HEADER_STATE, tunnel.getState(), colorString);
        keepaliveElement = new ElementBox(statusBox, HEADER_KEEPALIVE, tunnel.getKeepAlive() > 0 ? Integer.toString(tunnel.getKeepAlive()) : "off", colorString);
        separator = new Polygon(ROW_SEPARATOR_SHAPE_DIAMOND);
        separator.setFill(colorPaint);
        statusBox.addElement(stateElement, INDEX_STATE);
        statusBox.addElement(keepaliveElement, INDEX_KEEPALIVE);
        statusBox.addRowSeparator(separator);
        getChildren().add(statusBox);
        
        bandwidthsBox = new ColumnBox(this, Gui.COLUMN_INDEX_BANDWIDTHS);
        receivedBandwidthElement = new ElementBox(bandwidthsBox, HEADER_RECEIVED_BANDWIDTH, Float.toString(tunnel.getReceivedBandwidth()), colorString);
        sentBandwidthElement = new ElementBox(bandwidthsBox, HEADER_SENT_BANDWIDTH, Float.toString(tunnel.getSentBandwidth()), colorString);
        separator = new Polygon(ROW_SEPARATOR_SHAPE_DIAMOND);
        separator.setFill(colorPaint);
        bandwidthsBox.addElement(receivedBandwidthElement, INDEX_RECEIVED_BANDWIDTH);
        bandwidthsBox.addElement(sentBandwidthElement, INDEX_SENT_BANDWIDTH);
        bandwidthsBox.addRowSeparator(separator);
        getChildren().add(bandwidthsBox);
        
        markersBox = new ColumnBox(this, Gui.COLUMN_INDEX_MARKERS);
        latestHandshakeElement = new ElementBox(markersBox, HEADER_LATEST_HANDSHAKE, Long.toString(tunnel.getLatestHandshake()), colorString);
        fwmarkElement = new ElementBox(markersBox, HEADER_FWMARK, StringUtils.isNotEmpty(tunnel.getFwmark()) ?  tunnel.getFwmark() : "off", colorString);
        separator = new Polygon(ROW_SEPARATOR_SHAPE_DIAMOND);
        separator.setFill(colorPaint);
        markersBox.addElement(latestHandshakeElement, INDEX_LATEST_HANDSHAKE);
        markersBox.addElement(fwmarkElement, INDEX_FWMARK);
        markersBox.addRowSeparator(separator);
        getChildren().add(markersBox);
        
        interfacesBox = new ColumnBox(this, Gui.COLUMN_INDEX_INTERFACES);
        localInterfaceName = new ElementBox(interfacesBox, HEADER_LOCAL_INTERFACE, tunnel.getLocalInterfaceName(), colorString);
        remoteInterfaceName = new ElementBox(interfacesBox, HEADER_REMOTE_INTERFACE, tunnel.getRemoteInterfaceName(), colorString);
        separator = new Polygon(ROW_SEPARATOR_SHAPE_DIAMOND);
        separator.setFill(colorPaint);
        interfacesBox.addElement(localInterfaceName, INDEX_LOCAL_INTERFACE);
        interfacesBox.addElement(remoteInterfaceName, INDEX_REMOTE_INTERFACE);
        interfacesBox.addRowSeparator(separator);
        getChildren().add(interfacesBox);
        
        return this;
    }
    
    public void refresh() {
        stateElement.getDataLabel().setText(tunnel.getState());
        fwmarkElement.getDataLabel().setText(StringUtils.isNotEmpty(tunnel.getFwmark()) ?  tunnel.getFwmark() : "off");
        latestHandshakeElement.getDataLabel().setText(Long.toString(tunnel.getLatestHandshake()));
        receivedBandwidthElement.getDataLabel().setText(Float.toString(tunnel.getReceivedBandwidth()));
        sentBandwidthElement.getDataLabel().setText(Float.toString(tunnel.getSentBandwidth()));
    }
    
    public void refreshColumns(int... columnIndices) {
        ElementBox element;
        for (int columnIndex : columnIndices) {
            if (columnIndex < getChildren().size()) {
                ColumnBox columnBox = ((ColumnBox) getChildren().get(columnIndex));
                switch (columnIndex) {
                    case Gui.COLUMN_INDEX_TUNNEL_ADDRS:
                        element = (ElementBox)columnBox.getChildren().get(INDEX_LOCAL_TUNNEL_ADDR);
                        element.getDataLabel().setText(tunnel.getLocalTunnelInetAddr());
                            
                        element = (ElementBox)columnBox.getChildren().get(INDEX_REMOTE_TUNNEL_ADDR);
                        element.getDataLabel().setText(tunnel.getRemoteTunnelInetAddr());
                        break;
                        
                    case Gui.COLUMN_INDEX_ENDPOINTS:
                        element = (ElementBox)columnBox.getChildren().get(INDEX_LOCAL_ENDPOINT);
                        element.getDataLabel().setText(tunnel.getLocalPhysInetAddr() + ENDPOINT_SEPARATOR + tunnel.getLocalPhysInetListenPort());
                        
                        element = (ElementBox)columnBox.getChildren().get(INDEX_REMOTE_ENDPOINT);
                        element.getDataLabel().setText(tunnel.getRemotePhysInetAddr() + ENDPOINT_SEPARATOR + tunnel.getRemotePhysInetListenPort());
                        break;
                        
                    case Gui.COLUMN_INDEX_PUBLIC_KEYS:
                        element = (ElementBox)columnBox.getChildren().get(INDEX_LOCAL_PUBLIC_KEY);
                        element.getDataLabel().setText(tunnel.getLocalPublicKey());
                        
                        element = (ElementBox)columnBox.getChildren().get(INDEX_REMOTE_PUBLIC_KEY);
                        element.getDataLabel().setText(tunnel.getRemotePublicKey());
                        break;
                    
                    case Gui.COLUMN_INDEX_STATUS:
                        element = (ElementBox)columnBox.getChildren().get(INDEX_STATE);
                        element.getDataLabel().setText(tunnel.getState());
                        
                        element = (ElementBox)columnBox.getChildren().get(INDEX_KEEPALIVE);
                        element.getDataLabel().setText(tunnel.getKeepAlive() > 0 ? Integer.toString(tunnel.getKeepAlive()) : "off");
                        break;
                    
                    case Gui.COLUMN_INDEX_BANDWIDTHS:
                        element = (ElementBox)columnBox.getChildren().get(INDEX_RECEIVED_BANDWIDTH);
                        element.getDataLabel().setText(Float.toString(tunnel.getReceivedBandwidth()));
                        
                        element = (ElementBox)columnBox.getChildren().get(INDEX_SENT_BANDWIDTH);
                        element.getDataLabel().setText(Float.toString(tunnel.getSentBandwidth()));
                        break;
                        
                    case Gui.COLUMN_INDEX_MARKERS:
                        element = (ElementBox)columnBox.getChildren().get(INDEX_LATEST_HANDSHAKE);
                        element.getDataLabel().setText(Long.toString(tunnel.getLatestHandshake()));
                    
                        element = (ElementBox)columnBox.getChildren().get(INDEX_FWMARK);
                        element.getDataLabel().setText(StringUtils.isNotEmpty(tunnel.getFwmark()) ?  tunnel.getFwmark() : "off");
                        break;
                    
                    default:
                        break;
                }
            }
        }
    }
    
    public PersistenceTunnel getTunnel() {
        return tunnel;
    }
    
    public String getColor() {
        return colorString;
    }
    
    public double getWindowWidth() {
        return windowWidth;
    }
}
