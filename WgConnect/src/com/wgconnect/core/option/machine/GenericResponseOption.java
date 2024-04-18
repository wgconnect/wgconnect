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
 * GenericResponseOption
 * 
 * The generic response option.
 * 
 * @author: wgconnect@proton.me
 */
public class GenericResponseOption extends BaseUnsignedIntOption {
    
    private static final WgConnectLogger log = WgConnectLogger.getLogger(GenericResponseOption.class);

    
    public GenericResponseOption(boolean isV4) {
		this((long)0, isV4);
    }
    
    public GenericResponseOption(long unsignedInt, boolean isV4) {
        super(unsignedInt);
        setCode(Constants.OPTION_GENERIC_RESPONSE);
        setV4(isV4);
    }
}
