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

import com.wgconnect.core.util.WgConnectLogger;

import inet.ipaddr.IPAddress.IPVersion;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Iterator;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.routines.InetAddressValidator;

/**
 * LinuxDeviceManager
 * 
 * @author: wgconnect@proton.me
 */
class LinuxDeviceManager implements DeviceManagerInterface {
    
    private static final WgConnectLogger log = WgConnectLogger.getLogger(LinuxDeviceManager.class);    

    protected static final String COMMAND_IP = "ip";
    protected static final String COMMAND_DIR = "/usr/bin/";
    
    private static final String ARGS_ADD_DEVICE = "link add %s type wireguard";
    private static final String ARGS_SET_DEVICE_INET_ADDR = "addr add %s/%s dev %s";
    private static final String ARGS_SET_DEVICE_STATE = "link set %s %s";
    private static final String ARGS_GET_V4_DEVICE_INFO = "-4 link show %s";
    private static final String ARGS_GET_V6_DEVICE_INFO = "-6 link show %s";
    private static final String ARGS_GET_INTERFACE_DEVICE_LOCAL_ENDPOINT = "route get to %s";
    
    private String commandOutputString;
    private String commandErrorString;
    private int commandExitCode;

    private static final InetAddressValidator validator = InetAddressValidator.getInstance();

    public static final String USAGE =
        "Usage: ip [ OPTIONS ] OBJECT { COMMAND | help }\n" +
        "       ip [ -force ] -batch filename\n" +
        "where  OBJECT := { address | addrlabel | amt | fou | help | ila | ioam | l2tp |\n" +
        "                   link | macsec | maddress | monitor | mptcp | mroute | mrule |\n" +
        "                   neighbor | neighbour | netconf | netns | nexthop | ntable |\n" +
        "                   ntbl | route | rule | sr | tap | tcpmetrics |\n" +
        "                   token | tunnel | tuntap | vrf | xfrm }\n" +
        "       OPTIONS := { -V[ersion] | -s[tatistics] | -d[etails] | -r[esolve] |\n" +
        "                    -h[uman-readable] | -iec | -j[son] | -p[retty] |\n" +
        "                    -f[amily] { inet | inet6 | mpls | bridge | link } |\n" +
        "                    -4 | -6 | -M | -B | -0 |\n" +
        "                    -l[oops] { maximum-addr-flush-attempts } | -br[ief] |\n" +
        "                    -o[neline] | -t[imestamp] | -ts[hort] | -b[atch] [filename] |\n" +
        "                    -rc[vbuf] [size] | -n[etns] name | -N[umeric] | -a[ll] |\n" +
        "                    -c[olor]}";
    
    public LinuxDeviceManager() {}
    
    @Override
    public int addDevice(String deviceName) {
        executeCommand(COMMAND_IP + StringUtils.SPACE + String.format(ARGS_ADD_DEVICE, deviceName));
        return commandExitCode;
    }

    @Override
    public int setDeviceInetAddr(String deviceName, String inetAddr, String subnetMask) {
        executeCommand(COMMAND_IP + StringUtils.SPACE + String.format(ARGS_SET_DEVICE_INET_ADDR, inetAddr, subnetMask, deviceName));
        return commandExitCode;
    }

    @Override
    public int setDeviceState(String deviceName, InterfaceDeviceState state) {
        executeCommand(COMMAND_IP + StringUtils.SPACE + String.format(ARGS_SET_DEVICE_STATE, deviceName, state.toString()));
        return commandExitCode;
    }
    
    @Override
    public String getDeviceInfo(String deviceName, IPVersion ipVersion) {
        executeCommand(COMMAND_IP + StringUtils.SPACE + String.format(ipVersion.isIPv4() ? ARGS_GET_V4_DEVICE_INFO : ARGS_GET_V6_DEVICE_INFO, deviceName));
        return commandOutputString;
    }
    
    @Override
    public String getDeviceInetAddr(String deviceName, IPVersion ipVersion) {
        String inetAddr = null;
        
        executeCommand(COMMAND_IP + StringUtils.SPACE + String.format(ipVersion.isIPv4() ? ARGS_GET_V4_DEVICE_INFO : ARGS_GET_V6_DEVICE_INFO, deviceName));
        Iterator<String> iter = Arrays.asList(StringUtils.split(commandOutputString)).iterator();
        while (iter.hasNext()) {
            if (iter.next().equalsIgnoreCase("inet")) {
                inetAddr = StringUtils.substringBefore(iter.next(), "/");
                break;
            }
        }
        
        return inetAddr;
    }
    
    @Override
    public String getLocalEndpointByInetAddr(String inetAddr) {
        String localEndpoint = null;
        
        executeCommand(COMMAND_IP + StringUtils.SPACE + String.format(ARGS_GET_INTERFACE_DEVICE_LOCAL_ENDPOINT, inetAddr));
        
        String[] split = StringUtils.split(commandOutputString, StringUtils.SPACE);
        for (String element : split) {
            if (validator.isValid(element)) {
                localEndpoint = element;
            }
        }
        
        return localEndpoint;
    }
    
    public void showUsage(PrintStream out) {
        out.printf(USAGE);
    }
    
    @Override
    public String getCommandOutputString() {
        return commandOutputString;
    }
    
    public void setCommandOutputString(String str) {
        commandOutputString = str;
    }

    @Override
    public String getCommandErrorString() {
        return commandErrorString;
    }
    
    public void setCommandErrorString(String str) {
        commandErrorString = str;
    }
    
    public int getCommandExitCode() {
        return commandExitCode;
    }
    
    public void setCommandExitCode(int code) {
        commandExitCode = code;
    }
    
    public void executeCommand(String command) {
        setCommandExitCode(0);

        try {
            ProcessBuilder processBuilder = new ProcessBuilder();
            processBuilder.command(command.split(StringUtils.SPACE));

            Process process = processBuilder.start();
            commandExitCode = process.waitFor();
            InputStream in = (commandExitCode == 0) ? process.getInputStream() : process.getErrorStream();

            StringBuilder processOutput = new StringBuilder();
            try (Reader reader = new BufferedReader(new InputStreamReader(in, Charset.forName(StandardCharsets.UTF_8.name())))) {
                int c;
                while ((c = reader.read()) != -1) {
                    processOutput.append((char) c);
                }
            }

            if (commandExitCode == 0)
                commandOutputString = processOutput.toString();
            else
                commandErrorString = processOutput.toString();
            
            process.destroy();
            
        } catch (IOException | InterruptedException ex) {
            log.error("System process exception: " + ex);
            setCommandExitCode(1);
        }
    }
    
    /**
     * @param args the command line arguments
     */
    public static void main(String... args) {
        StringBuilder command = new StringBuilder(COMMAND_IP + StringUtils.SPACE);
        for (String arg : args) {
            command.append(arg).append(StringUtils.SPACE);
        }
        
        LinuxDeviceManager mgr = new LinuxDeviceManager();
        mgr.executeCommand(command.toString());
    }
}
