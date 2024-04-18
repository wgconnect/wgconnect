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
package com.wgconnect.core.option.machine;

import com.wgconnect.core.option.base.BaseUnsignedIntOption;
import com.wgconnect.core.util.Constants;
import com.wgconnect.core.util.WgConnectLogger;

/**
 * PingInetPortOption
 * 
 * The ping inet port option.
 * 
 * @author: wgconnect@proton.me
 */
public class PingInetPortOption extends BaseUnsignedIntOption {
    
    private static final WgConnectLogger log = WgConnectLogger.getLogger(PingInetPortOption.class);

    public PingInetPortOption (boolean isV4) {
		this((long)0, isV4);
    }
    
    public PingInetPortOption(long unsignedInt, boolean isV4) {
        super(unsignedInt);
        setCode(Constants.OPTION_PING_INET_PORT);
        setV4(isV4);
    }
}
