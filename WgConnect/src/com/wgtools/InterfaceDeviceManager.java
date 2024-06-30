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
package com.wgtools;

import inet.ipaddr.IPAddress.IPVersion;

/**
 * InterfaceDeviceManager
 * 
 * @author: wgconnect@proton.me
 */
public interface InterfaceDeviceManager {
    
    public static final int INTERFACE_DEVICE_STATE_UP = 0;
    public static final int INTERFACE_DEVICE_STATE_DOWN = 1;
    
    public static enum InterfaceDeviceState {
        UP("up"),
        DOWN("down");

        private final String state;

        InterfaceDeviceState(final String text) {
            this.state = text;
        }

        @Override
        public String toString() {
            return state;
        }
    }
        
    public int addDevice(String deviceName);
    
    public int setDeviceInetAddr(String deviceName, String inetAddr, String networkMask);
    
    public int setDeviceState(String deviceName, InterfaceDeviceState state);
    
    public String getDeviceInfo(String deviceName, IPVersion ipVersion);
    
    public String getDeviceInetAddr(String deviceName, IPVersion ipVersion);
    
    public String getLocalEndpointByInetAddr(String inetAddr);
    
    public String getCommandOutputString();
    public String getCommandErrorString();
}
