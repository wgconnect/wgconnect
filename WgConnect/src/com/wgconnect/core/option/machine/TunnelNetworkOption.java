/*
 * Copyright 2024 wgdhcp@protonmail.com. All Rights Reserved.
 *
 * WgDhcp is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * WgDhcp is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with WireguardDhcp.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.wgconnect.core.option.machine;

import com.wgconnect.core.option.base.BaseStringOption;
import com.wgconnect.core.util.Constants;
import com.wgconnect.core.util.WgConnectLogger;

/**
 * TunnelNetworkOption
 * 
 * The tunnel network option.
 * 
 * @author: wgconnect@proton.me
 */
public class TunnelNetworkOption extends BaseStringOption {
    
    private static final WgConnectLogger log = WgConnectLogger.getLogger(TunnelNetworkOption.class);

    public TunnelNetworkOption(boolean isV4) {
        this(null, isV4);
    }

    public TunnelNetworkOption(String id, boolean isV4) {
        super(id);
        setCode(Constants.OPTION_TUNNEL_NETWORK);
        setV4(isV4);
    }
}