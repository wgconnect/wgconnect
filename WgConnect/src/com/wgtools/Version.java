// SPDX-License-Identifier: GPL-2.0
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

/*
 * wireguard-tools: Copyright (C) 2015-2020 Jason A. Donenfeld <Jason@zx2c4.com>. All Rights Reserved.
 */ 

package com.wgtools;

import java.util.Calendar;

/**
 * AddConf
 * 
 * @author wireguard-tools version: Jason A. Donenfeld
 * @author WgConnect version:  wgconnect@proton.me
 */
public class Version {
    
    public static String WIREGUARD_TOOLS_VERSION = "1.0.20210914";
    
    public static void main(String[] args) {
        System.out.println(getVersion());
    }

    public static String getVersion() {
        Package pkg = Package.getPackage("com.wgtools");
        StringBuilder sb = new StringBuilder();

        sb.append(pkg.getImplementationTitle());
        sb.append(' ');
        sb.append(WIREGUARD_TOOLS_VERSION);
        sb.append(System.getProperty("line.separator"));
        sb.append("Copyright ");
        sb.append(pkg.getImplementationVendor());
        sb.append(" 2021-");
        sb.append(Calendar.getInstance().get(Calendar.YEAR));
        sb.append(".  All Rights Reserved.");

        return sb.toString();
    }
}