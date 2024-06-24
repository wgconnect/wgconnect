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
package com.wgtools;

import com.wgconnect.core.util.WgConnectLogger;

import inet.ipaddr.IPAddress.IPVersion;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Scanner;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.routines.InetAddressValidator;

/**
 * BsdDeviceManager
 * 
 * @author: wgconnect@proton.me
 */
public class BsdDeviceManager implements InterfaceDeviceManager {
    
    private static final WgConnectLogger log = WgConnectLogger.getLogger(BsdDeviceManager.class);    

    protected static final String COMMAND = "ifConfig";
    protected static final String COMMAND_DIR = "/usr/bin/";
    
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
    
    private static final String CONFIG_LOCAL_HEADER =  "[Interface]";

    private static final String CONFIG_LOCAL_TUNNEL_ADDR =  "Address = ";
    private static final String CONFIG_LOCAL_LISTEN_PORT = "ListenPort = ";
    private static final String CONFIG_LOCAL_PRIVATE_KEY = "PrivateKey = ";
    private static final String CONFIG_REMOTE_HEADER =  "[Peer]";
    private static final String CONFIG_REMOTE_ALLOWED_IPS = "AllowedIPs = ";
    private static final String CONFIG_REMOTE_ENDPOINT_ADDR = "Endpoint = ";
    private static final String CONFIG_REMOTE_PUBLIC_KEY = "PublicKey = ";
    private static final String CONFIG_REMOTE_PRESHARED_KEY = "PreSharedKey = ";

    private Charset charset = Charset.forName("UTF-8");
    
    protected BsdDeviceManager() {
    }

    public static final String WG_CONFIG_DIR = "/usr/local/etc/wireguard/";
    private static final String WG_CONFIG_FILE_SUFFIX = ".conf";
    private static final String TEMP_FILE_SUFFIX = ".tmp";
    
    public void setCharset(Charset charset) {
        this.charset = charset;
    }
    
    @Override
    public int init() {
        int status = 0;
        
        try {
            File dir = new File(WG_CONFIG_DIR);
            FileUtils.forceMkdir(dir);
     
            // Set the permissions for the owner
            dir.setReadable(true);
            dir.setWritable(true);
            dir.setExecutable(true);

            // Set the permissions for others
            dir.setReadable(false, false);
            dir.setWritable(false, false);
            dir.setExecutable(false, false);
        } catch (IOException ex) {
            log.info(ex.getMessage());
            status = 1;
        }

        return status;
    }

    @Override
    public int addDevice(String deviceName) {
        int status = 0;
        
        try {
            FileUtils.writeStringToFile(new File(WG_CONFIG_DIR + deviceName + WG_CONFIG_FILE_SUFFIX),
                CONFIG_LOCAL_HEADER + System.lineSeparator(), charset);
        } catch (IOException ex) {
            log.info(ex.getMessage());
            status = 1;
        }

        return status;
    }

    @Override
    public int setDeviceInetAddr(String deviceName, String inetAddr, String subnetMask) {
        return insertConfigParameter(deviceName, CONFIG_LOCAL_HEADER, CONFIG_LOCAL_TUNNEL_ADDR + inetAddr + "/" + subnetMask);
    }

    @Override
    public int setDevicePrivateKey(String deviceName, String privateKey) {
        return insertConfigParameter(deviceName, CONFIG_LOCAL_HEADER, CONFIG_LOCAL_PRIVATE_KEY + privateKey);
    }

    @Override
    public int setDeviceListenPort(String deviceName, long listenPort) {
        return insertConfigParameter(deviceName, CONFIG_LOCAL_HEADER, CONFIG_LOCAL_LISTEN_PORT + listenPort);
    }
    
    @Override
    public int setDeviceState(String deviceName, InterfaceDeviceState state) {
        return Wg.getCommandSuccessCode();
    }
    
    private static final String COMMAND_DEVICE_STATUS = "server wireguard status";
    private static final String COMMAND_CONFIG_NETWORK_DEVICE = "ifconfig";
    @Override
    public String getDeviceInfo(String deviceName, IPVersion ipVersion) {
        executeCommand(COMMAND_CONFIG_NETWORK_DEVICE + StringUtils.SPACE + deviceName);
        return commandOutputString;
    }
    
    @Override
    public String getDeviceInetAddr(String deviceName, IPVersion ipVersion) {
        String inetAddr = null;
        
        executeCommand(COMMAND_CONFIG_NETWORK_DEVICE + StringUtils.SPACE + deviceName);
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
    
    public int insertConfigParameter(String deviceName, String searchStr, String newLine) {
        int status = 0;
        
        try {
            File configFile = new File(WG_CONFIG_DIR + deviceName + WG_CONFIG_FILE_SUFFIX);
            if (!configFile.exists()) {
                addDevice(deviceName);
            }
            
            File tempFile = new File(WG_CONFIG_DIR + deviceName + TEMP_FILE_SUFFIX);
            BufferedReader br = new BufferedReader(new FileReader(configFile));
            PrintWriter pw = new PrintWriter(new FileWriter(tempFile));
            
            String line;
            while ((line = br.readLine()) != null) {
                pw.println(line);
                if (StringUtils.contains(line, searchStr)) {
                    pw.println(newLine);
                }
            }
            
            FileUtils.copyFile(tempFile, configFile);
        } catch (IOException ex) {
            log.info(ex.getMessage());
            status = 1;
        }
        
        return status;
    }
    
    public void executeCommand(String command) {
        setCommandExitCode(0);

        try {
            ProcessBuilder processBuilder = new ProcessBuilder();
            processBuilder.command(command.split(" "));

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
        StringBuilder command = new StringBuilder();
        for (String arg : args)
            command.append(arg).append(" ");
        BsdDeviceManager ip = new BsdDeviceManager();
        ip.executeCommand(command.toString());
    }
}
