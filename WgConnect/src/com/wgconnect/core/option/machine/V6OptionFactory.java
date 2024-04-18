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
 * V6OptionFactory
 * 
 * A factory for creating V6 option objects.
 *
 * @author JagornetDhcp version: A. Gregory Rabil
 * @author WgConnect version: wgconnect@proton.me
 */
public class V6OptionFactory {

    private static final WgConnectLogger log = WgConnectLogger.getLogger(V6OptionFactory.class);

    /**
     * Gets the option for the given option code.
     *
     * @param code the option code
     * @return the option object for the option code.
     */
    public static Option getDhcpOption(int code) {
        Option option;
        switch (code) {
            case Constants.OPTION_MESSAGE_SENDER:
                option = new MsgSenderOption(false);
                break;
                
            case Constants.OPTION_REMOTE_PHYS_INET_ADDR:
                option = new RemotePhysInetAddrOption(false);
                break;
            case Constants.OPTION_LOCAL_PHYS_INET_ADDR:
                option = new LocalPhysInetAddrOption(false);
                break;
                
            case Constants.OPTION_LOCAL_TUNNEL_INET_ADDR:
                option = new LocalTunnelInetAddrOption(false);
                break;
            case Constants.OPTION_REMOTE_TUNNEL_INET_ADDR:
                option = new RemoteTunnelInetAddrOption(false);
                break;
            case Constants.OPTION_REMOTE_TUNNEL_INET_COM_PORT:
                option = new RemoteTunnelInetComPortOption(false);
                break;
                
            case Constants.OPTION_INTERFACE_NAME:
                option = new InterfaceNameOption(false);
                break;
                
            case Constants.OPTION_REMOTE_WG_PUBLIC_KEY:
                option = new RemoteWgPublicKeyOption(false);
                break;
            case Constants.OPTION_LOCAL_WG_PUBLIC_KEY:
                option = new LocalWgPublicKeyOption(false);
                break;
                
            case Constants.OPTION_REMOTE_ENDPOINT_TYPE:
                option = new RemoteEndpointTypeOption(false);
                break;
            case Constants.OPTION_LOCAL_ENDPOINT_TYPE:
                option = new LocalEndpointTypeOption(false);
                break;
                
            case Constants.OPTION_REMOTE_PHYS_INET_LISTEN_PORT:
                option = new RemotePhysInetListenPortOption(false);
                break;
            case Constants.OPTION_LOCAL_PHYS_INET_LISTEN_PORT:
                option = new LocalPhysInetListenPortOption(false);
                break;
                
            case Constants.OPTION_REMOTE_PHYS_INET_COM_PORT:
                option = new RemotePhysInetComPortOption(false);
                break;
            case Constants.OPTION_LOCAL_PHYS_INET_COM_PORT:
                option = new LocalPhysInetComPortOption(false);
                break;
                
            case Constants.OPTION_TUNNEL_ID:
                option = new TunnelIdOption(false);
                break;
                
            case Constants.OPTION_GENERIC_ID:
                option = new GenericIdOption(false);
                break;
                
            case Constants.OPTION_KEEPALIVE_POLICY:
                option = new KeepalivePolicyOption(false);
                break;

            case Constants.OPTION_TUNNEL_STATUS:
                option = new TunnelStatusOption(false);
                break;
                
            case Constants.OPTION_SPECIFIC_INFO:
                option = new SpecificInfoOption(false);
                break;
            
            case Constants.OPTION_GENERIC_PUBLIC_KEY:
                option = new GenericPublicKeyOption(false);
                break;
            case Constants.OPTION_LOCAL_GENERIC_PUBLIC_KEY:
                option = new LocalGenericPublicKeyOption(false);
                break;
                
            case Constants.OPTION_PING_INET_ADDR:
                option = new PingInetAddrOption(false);
                break;
            case Constants.OPTION_PING_INET_PORT:
                option = new PingInetPortOption(false);
                break;
                
            case Constants.OPTION_GENERIC_RESPONSE:
                option = new GenericResponseOption(false);
                break;

            case Constants.OPTION_TUNNEL_NETWORK:
                option = new TunnelNetworkOption(false);
                break;
                
            default:
                // Unknown option code, build an opaque option to hold it
                UnknownOption unknownOption = new UnknownOption();
                unknownOption.setCode(code);
                unknownOption.setV4(false);
                option = unknownOption;
                break;
        }
        
        return option;
    }
}
