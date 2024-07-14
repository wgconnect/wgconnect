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
package com.wgconnect.gui;

import com.wgconnect.db.persistence.PersistenceTunnel;

/**
 * Gui
 * 
 * @author: wgconnect@proton.me
 */
public interface Gui {
    
    public static final int COLUMN_INDEX_TUNNEL_ADDRS = 0;
    public static final int COLUMN_INDEX_ENDPOINTS = 1;
    public static final int COLUMN_INDEX_PUBLIC_KEYS = 2;
    public static final int COLUMN_INDEX_STATUS = 3;
    public static final int COLUMN_INDEX_BANDWIDTHS = 4;
    public static final int COLUMN_INDEX_MARKERS = 5;
    public static final int COLUMN_INDEX_INTERFACES = 6;
    
    public void initialize();
    public void addTunnel(PersistenceTunnel tunnel);
    public void refreshTunnelRowColumns(PersistenceTunnel tunnel, int... columnIndices);
}
