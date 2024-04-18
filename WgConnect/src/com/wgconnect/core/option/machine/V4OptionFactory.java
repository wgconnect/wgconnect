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

import com.wgconnect.core.option.UnknownOption;
import com.wgconnect.core.util.Constants;
import com.wgconnect.core.util.WgConnectLogger;
import com.wgconnect.core.option.base.Option;

/**
 * V4OptionFactory
 * 
 * A factory for creating V4 option objects.
 *
 * @author JagornetDhcp version: A. Gregory Rabil
 * @author WgConnect version: wgconnect@proton.me
 */
public class V4OptionFactory {
    
    private static final WgConnectLogger log = WgConnectLogger.getLogger(V4OptionFactory.class);

    /**
     * Gets the option for the given option code.
     *
     * @param code the option code
     * @return the option object for the option code.
     */
    public static Option getOption(int code) {
        Option option = null;
        switch (code) {
            case Constants.OPTION_MESSAGE_TYPE:
                option = new MsgTypeOption(true);
                break;
            case Constants.OPTION_MESSAGE_SENDER:
                option = new MsgSenderOption(true);
                break;
                
            case Constants.OPTION_REMOTE_PHYS_INET_ADDR:
                option = new RemotePhysInetAddrOption(true);
                break;
            case Constants.OPTION_LOCAL_PHYS_INET_ADDR:
                option = new LocalPhysInetAddrOption(true);
                break;
                
            case Constants.OPTION_LOCAL_TUNNEL_INET_ADDR:
                option = new LocalTunnelInetAddrOption(true);
                break;
            case Constants.OPTION_REMOTE_TUNNEL_INET_ADDR:
                option = new RemoteTunnelInetAddrOption(true);
                break;
            case Constants.OPTION_REMOTE_TUNNEL_INET_COM_PORT:
                option = new RemoteTunnelInetComPortOption(true);
                break;            
                
            case Constants.OPTION_INTERFACE_NAME:
                option = new InterfaceNameOption(true);
                break;
                
            case Constants.OPTION_REMOTE_WG_PUBLIC_KEY:
                option = new RemoteWgPublicKeyOption(true);
                break;
            case Constants.OPTION_LOCAL_WG_PUBLIC_KEY:
                option = new LocalWgPublicKeyOption(true);
                break;
                
            case Constants.OPTION_REMOTE_ENDPOINT_TYPE:
                option = new RemoteEndpointTypeOption(true);
                break;
            case Constants.OPTION_LOCAL_ENDPOINT_TYPE:
                option = new LocalEndpointTypeOption(true);
                break;
            
            case Constants.OPTION_REMOTE_PHYS_INET_LISTEN_PORT:
                option = new RemotePhysInetListenPortOption(true);
                break;
            case Constants.OPTION_LOCAL_PHYS_INET_LISTEN_PORT:
                option = new LocalPhysInetListenPortOption(true);
                break;
                
            case Constants.OPTION_REMOTE_PHYS_INET_COM_PORT:
                option = new RemotePhysInetComPortOption(true);
                break;
            case Constants.OPTION_LOCAL_PHYS_INET_COM_PORT:
                option = new LocalPhysInetComPortOption(true);
                break;
                
            case Constants.OPTION_TUNNEL_ID:
                option = new TunnelIdOption(true);
                break;
            case Constants.OPTION_GENERIC_ID:
                option = new GenericIdOption(true);
                break;
                
            case Constants.OPTION_KEEPALIVE_POLICY:
                option = new KeepalivePolicyOption(true);
                break;
            
            case Constants.OPTION_TUNNEL_STATUS:
                option = new TunnelStatusOption(true);
                break;
                
            case Constants.OPTION_SPECIFIC_INFO:
                option = new SpecificInfoOption(true);
                break;
                
            case Constants.OPTION_GENERIC_PUBLIC_KEY:
                option = new GenericPublicKeyOption(true);
                break;
            case Constants.OPTION_LOCAL_GENERIC_PUBLIC_KEY:
                option = new LocalGenericPublicKeyOption(true);
                break;
            
            case Constants.OPTION_PING_INET_ADDR:
                option = new PingInetAddrOption(true);
                break;
            case Constants.OPTION_PING_INET_PORT:
                option = new PingInetPortOption(true);
                break;
                
            case Constants.OPTION_GENERIC_RESPONSE:
                option = new GenericResponseOption(true);
                break;
            
            case Constants.OPTION_TUNNEL_NETWORK:
                option = new TunnelNetworkOption(true);
                break;
                
            case Constants.OPTION_EOF:
                break;
                
            default:
                // Unknown option code, build an opaque option to hold it
                UnknownOption unknownOption = new UnknownOption();
                unknownOption.setCode(code);
                unknownOption.setV4(true);
                option = unknownOption;
                break;
        }

        return option;
    }
}
