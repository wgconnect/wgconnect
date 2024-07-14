/*
 * Copyright 2024 wgconnect@proton.me. All Rights Reserved.
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
import java.util.Scanner;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.routines.InetAddressValidator;

/**
 * BsdDeviceManager
 * 
 * @author: wgconnect@proton.me
 */
public class BsdDeviceManager implements DeviceManagerInterface {
    
    private static final WgConnectLogger log = WgConnectLogger.getLogger(BsdDeviceManager.class);    

    protected static final String COMMAND_IFCONFIG = "ifconfig";
    protected static final String COMMAND_DIR = "/usr/bin/";
    
    private static final String ARGS_ADD_DEVICE = "%s create";
    private static final String ARGS_SET_DEVICE_INET_ADDR = "%s inet %s/%s";
    private static final String ARGS_SET_DEVICE_STATE = "%s %s";
    private String commandOutputString;
    private String commandErrorString;
    private int commandExitCode;

    private static final InetAddressValidator validator = InetAddressValidator.getInstance();

    public static final String USAGE =
        "ifconfig [-j jail] [-kLmn] [-f type:format] interface [create]\n" +
        "         [address_family [address [dest_address]]] [parameters]\n" +
        "ifconfig [-j jail] interface destroy\n" +
        "ifconfig [-j jail] -a [-dkLmuv] [-f type:format] [-G groupname]\n" +
        "         [-g groupname] [address_family]\n" +
        "ifconfig -C\n" +
        "ifconfig [-j jail] -g groupname\n" +
        "ifconfig [-j jail] -l [-du] [-g groupname] [address_family]\n" +
        "ifconfig [-j jail] [-dkLmuv] [-f type:format]\n";
    
    public BsdDeviceManager() {
    }

    @Override
    public int addDevice(String deviceName) {
        executeCommand(COMMAND_IFCONFIG + StringUtils.SPACE + String.format(ARGS_ADD_DEVICE, deviceName));
        return commandExitCode;
    }

    @Override
    public int setDeviceInetAddr(String deviceName, String inetAddr, String subnetMask) {
        executeCommand(COMMAND_IFCONFIG + StringUtils.SPACE + String.format(ARGS_SET_DEVICE_INET_ADDR, deviceName, inetAddr, subnetMask));
        return commandExitCode;
    }

    @Override
    public int setDeviceState(String deviceName, InterfaceDeviceState state) {
        executeCommand(COMMAND_IFCONFIG + StringUtils.SPACE + String.format(ARGS_SET_DEVICE_STATE, deviceName, state.toString()));
        return commandExitCode;
    }
    
    @Override
    public String getDeviceInfo(String deviceName, IPVersion ipVersion) {
        executeCommand(COMMAND_IFCONFIG + StringUtils.SPACE + deviceName);
        return commandOutputString;
    }
    
    @Override
    public String getDeviceInetAddr(String deviceName, IPVersion ipVersion) {
        String inetAddr = null;
        
        executeCommand(COMMAND_IFCONFIG + StringUtils.SPACE + deviceName);
        Iterator<String> iter = Arrays.asList(StringUtils.split(commandOutputString)).iterator();
        while (iter.hasNext()) {
            if (iter.next().equalsIgnoreCase("inet")) {
                inetAddr = StringUtils.substringBefore(iter.next(), "/");
                break;
            }
        }
        
        return inetAddr;
    }
    
    private static final String COMMAND_GET_ENDPOINT = "route -n get -host ";
    private static final String ENDPOINT_TAG = "destination:";
    
    @Override
    public String getLocalEndpointByInetAddr(String inetAddr) {
        String localEndpoint = null;
        
        executeCommand(COMMAND_GET_ENDPOINT + inetAddr);
        
        Scanner s = new Scanner(commandOutputString);
        while(s.hasNextLine()) {
            String line = s.nextLine();
            if (StringUtils.containsIgnoreCase(line, ENDPOINT_TAG)) {
                localEndpoint = StringUtils.strip(StringUtils.stripStart(line, ENDPOINT_TAG));
            }
        }
        
        return validator.isValid(localEndpoint) ? localEndpoint : null;
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
        StringBuilder command = new StringBuilder(COMMAND_IFCONFIG + StringUtils.SPACE);
        for (String arg : args) {
            command.append(arg).append(StringUtils.SPACE);
        }
        
        BsdDeviceManager mgr = new BsdDeviceManager();
        mgr.executeCommand(command.toString());
    }
}
