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
package com.wgconnect.core;

import com.wgconnect.core.util.Constants;

import java.util.Calendar;

/**
 * WgVersion
 * 
 * @author JagornetDhcp version: A. Gregory Rabil
 * @author WgConnect version: wgconnect@proton.me
 */
public class Version {
        
    public static void main(String[] args) {
        System.out.println(getVersion());
    }

    public static String getVersion() {
        Package pkg = Package.getPackage("com.wgconnect.server");
        
        if (pkg != null) {
            StringBuilder sb = new StringBuilder();
            sb.append(pkg.getImplementationTitle()).append(' ').append(Constants.APP_VERSION)
              .append(System.getProperty("line.separator")).append("Copyright ").append(pkg.getImplementationVendor())
              .append(" 2022-").append(Calendar.getInstance().get(Calendar.YEAR)).append(".  All Rights Reserved.");

            return sb.toString();
        }
        
        return "";
    }
}
