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

import com.wgconnect.WgConnect;
import com.wgconnect.core.util.CircularQueue;
import com.wgconnect.core.util.WgConnectLogger;
import com.wgconnect.db.persistence.PersistenceTunnel;

import java.awt.Dimension;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import org.apache.commons.lang3.StringUtils;

/**
 * Gui
 * 
 * The WgConnect GUI.
 * 
 * @author: wgconnect@proton.me
 */
public class Gui extends Application {
    
    private static final WgConnectLogger log = WgConnectLogger.getLogger(Gui.class);
    
    private static Gui INSTANCE = null;
    
    private ScheduledExecutorService scheduledExecutorService;
        
    private static Stage primaryStage;
    private static Scene primaryScene;
    private static VBox primaryVBox;
    private static ScrollPane primaryScrollPane;

    private static final int PANE_PADDING = 1;
    
    private static final String WINDOW_TITLE_TEXT = "WgConnect Tunnels";
    private static final String WINDOW_TITLE_STYLE =
        "-fx-font-weight: bold;" +
        "-fx-font-size: 14px;";
        
    private static final CountDownLatch windowIsShowing = new CountDownLatch(1);
    
    private static double windowWidth;
    private static double windowHeight;
    private static final double WINDOW_HEIGHT_FACTOR = 0.8;
    
    public static final int COLUMN_INDEX_TUNNEL_ADDRS = 0;
    public static final int COLUMN_INDEX_ENDPOINTS = 1;
    public static final int COLUMN_INDEX_PUBLIC_KEYS = 2;
    public static final int COLUMN_INDEX_STATUS = 3;
    public static final int COLUMN_INDEX_BANDWIDTHS = 4;
    public static final int COLUMN_INDEX_MARKERS = 5;
    public static final int COLUMN_INDEX_INTERFACES = 6;
    
    private static final String ID_TUNNEL_ROW = "ID_TUNNEL_ROW";
    
    private static final CircularQueue tunnelRowColors = new CircularQueue("black", "darkblue", "darkgreen");

    /**
     * Gets the single instance of Gui.
     *
     * @return single instance of Gui
     */
    public static synchronized Gui getInstance() {
        return INSTANCE;
    }
    
    public Gui() {        
        INSTANCE = this;
    }
    
    public static Gui waitForWindowShowing() {
        if (INSTANCE != null) {
            try {
                windowIsShowing.await();
            } catch (InterruptedException ex) {
                log.info(ex.getMessage());
            }
        }
        
        return INSTANCE;
    }

    public static void addTunnel(PersistenceTunnel tunnel) {
        if (INSTANCE != null) {
            Platform.runLater(() -> {
                addTunnelRow(tunnel);
            });
        }
    }
    
    private static void addTunnelRow(PersistenceTunnel tunnel) {
        for (Node node : primaryVBox.getChildren()) {
            if (StringUtils.equals(node.getId(), ID_TUNNEL_ROW)) {
                TunnelRow row = (TunnelRow) node;
                if (StringUtils.equals(tunnel.getLocalPublicKey(), row.getTunnel().getLocalPublicKey()) &&
                    StringUtils.equals(tunnel.getRemotePublicKey(), row.getTunnel().getRemotePublicKey())) {
                    return;
                }
            }
        }

        TunnelRow tunnelRow = new TunnelRow(tunnel, windowWidth, (String) tunnelRowColors.dequeue()).init();
        tunnelRow.setId(ID_TUNNEL_ROW);
        primaryVBox.getChildren().add(tunnelRow);
    }

    public static void refreshAllTunnelRows() {
        if (INSTANCE != null) {
            for (Node node : primaryVBox.getChildren()) {
                if (StringUtils.equals(node.getId(), ID_TUNNEL_ROW)) {
                    TunnelRow row = (TunnelRow) node;
                    PersistenceTunnel tunnel = WgConnect.getTunnelByTunnelId(row.getTunnel().getId().toString());
                    if (tunnel != null) {
                        Platform.runLater(() -> {
                            row.refresh();
                        });
                    }
                }
            }
        }
    }
    
    public static void refreshTunnelRow(PersistenceTunnel tunnel) {
        if (INSTANCE != null) {
            if (tunnel != null) {
                for (Node node : primaryVBox.getChildren()) {
                    if (StringUtils.equals(node.getId(), ID_TUNNEL_ROW)) {
                        TunnelRow row = (TunnelRow) node;
                        if (StringUtils.equals(tunnel.getId().toString(), row.getTunnel().getId().toString())) {
                            Platform.runLater(() -> {
                                row.refresh();
                            });
                        }
                    }
                }
            }
        }
    }
    
    public static void refreshTunnelRowColumns(PersistenceTunnel tunnel, int... columnIndices) {
        if (INSTANCE != null) {
            for (Node node : primaryVBox.getChildren()) {
                if (StringUtils.equals(node.getId(), ID_TUNNEL_ROW)) {
                    TunnelRow row = (TunnelRow) node;
                    if (row.getTunnel().getId().equals(tunnel.getId())) {
                        Platform.runLater(() -> {
                            row.refreshColumns(columnIndices);
                        });
                        break;
                    }
                }
            }
        }
    }
    
    @Override
    public void start(Stage stage) {
        Dimension screenSize = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
		windowWidth = screenSize.getWidth();
	    windowHeight = screenSize.getHeight() * WINDOW_HEIGHT_FACTOR;
        
        scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();

        primaryStage = stage;
        TextFlow titleText = new TextFlow(new Text(WINDOW_TITLE_TEXT));
        titleText.setStyle(WINDOW_TITLE_STYLE);
        primaryStage.setTitle(((Text) titleText.getChildren().get(0)).getText());
        primaryStage.setOnCloseRequest((WindowEvent t) -> {
            scheduledExecutorService.shutdownNow();
        });
        
        primaryScrollPane = new ScrollPane();
        
        primaryVBox = new VBox();
        primaryVBox.setPadding(new Insets(PANE_PADDING, PANE_PADDING, PANE_PADDING, PANE_PADDING));
        
        primaryScrollPane.setContent(primaryVBox);

        primaryScene = new Scene(primaryScrollPane, windowWidth, windowHeight);
        
        primaryStage.setScene(primaryScene);
        primaryStage.show();

        // Register the rows refresh handler
        WgConnect.addRefreshTunnelsHandler(() -> {
            refreshAllTunnelRows();
        });
        
        windowIsShowing.countDown();
    }
}
