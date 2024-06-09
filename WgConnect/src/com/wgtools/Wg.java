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

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import org.scijava.nativelib.NativeLoader;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;

/**
 * Wg
 * 
 * @author: wgconnect@proton.me
 */
@Command(name = "wg",
    separator = " ",
    requiredOptionMarker = '*',
    subcommands = {
        Show.class, ShowConf.class, Set.class, SetConf.class, AddConf.class,
        SyncConf.class, GenKey.class,GenPsk.class, PubKey.class
    },
    description = "Cross-platform userspace tools to configure Wireguard implementations")
public class Wg implements Runnable {
    
    private static final WgConnectLogger log = WgConnectLogger.getLogger(Wg.class);    
    
    public static final String WG_NATIVE_LIBRARY_NAME = "wg";
    public static final String WG_NATIVE_LIBRARY_BASE_PATH = "/natives/";

    public static final String OS_NAME_AIX = "aix";
    public static final String OS_NAME_LINUX = "linux";
    public static final String OS_NAME_BSD_UNIX = "bsd";
    public static final String OS_NAME_OSX = "osx";
    public static final String OS_NAME_WINDOWS = "windows";
    public static final String OS_NAME_DEFAULT = OS_NAME_LINUX;
    
    public static final String OS_ARCH_64 = "64";
    public static final String OS_ARCH_32 = "32";
    public static final String OS_ARCH_DEFAULT = OS_ARCH_64;
    
    private static final int COMMAND_SUCCESS = 0;
    private static final int COMMAND_FAILURE = 1;

    private static boolean libraryFileLoaded = false;

    private String privateKey;
    private String publicKey;
    private String preSharedKey;
    
    private ByteArrayOutputStream commandOutputByteArrayStream;
    private OutputStream commandOutputStream;
    private String commandOutputString;
    private String commandErrorString;
    
    private int commandExitCode;

    private static final InterfaceDeviceManager deviceMgr = new Ip();
    
    public static final String OPTION_ALL = "all";
    public static final String OPTION_INTERFACES = "interfaces";
    public static final String OPTION_PUBLIC_KEY = "public-key";
    public static final String OPTION_PRIVATE_KEY = "private-key";
    public static final String OPTION_LISTEN_PORT = "listen-port";
    public static final String OPTION_FWMARK = "fwmark";
    public static final String OPTION_PEERS = "peers";
    public static final String OPTION_PRESHARED_KEYS = "preshared-keys";
    public static final String OPTION_ENDPOINTS = "endpoints";
    public static final String OPTION_ALLOWED_IPS = "allowed-ips";
    public static final String OPTION_LATEST_HANDSHAKES = "latest-handshakes";
    public static final String OPTION_TRANSFER = "transfer";
    public static final String OPTION_PERSISTENT_KEEPALIVE = "persistent-keepalive";
    public static final String OPTION_DUMP = "dump";
    public static final String OPTION_PEER = "peer";
    public static final String OPTION_REMOVE = "remove";
    public static final String OPTION_PRESHARED_KEY = "preshared-key";
    public static final String OPTION_ENDPOINT = "endpoint";
    
    @Option(names = {"-h", "--help"}, defaultValue = "false")
    private boolean helpRequested;

    @Option(names = {"-v", "--version"}, defaultValue = "false")
    private boolean versionRequested;

    @Spec CommandSpec spec;

    public Wg() {
        try {
            commandOutputByteArrayStream = new ByteArrayOutputStream();
            commandOutputStream = new PrintStream(commandOutputByteArrayStream, true, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException ex) {
            log.error("Encoding exception: " + ex);
        }
    }
    
    public String getPrivateKey() {
        return privateKey;
    }
    
    public void setPrivateKey(String key) {
        privateKey = key;
    }
    
    public String getPublicKey() {
        return publicKey;
    }
    
    public void setPublicKey(String key) {
        publicKey = key;
    }
    
    public String getPreSharedKey() {
        return preSharedKey;
    }
    
    public void setPreSharedKey(String key) {
        preSharedKey = key;
    }
    
    public ByteArrayOutputStream getCommandOutputByteArrayStream() {
        return commandOutputByteArrayStream;
    }
    
    public void setCommandOutputByteArrayStream(ByteArrayOutputStream stream) {
        commandOutputByteArrayStream = stream;
    }
    
    public OutputStream getCommandOutputStream() {
        return commandOutputStream;
    }
    
    public void setCommandOutputStream(OutputStream stream) {
        commandOutputStream = stream;
    }
    
    public String getCommandOutputString() {
        return commandOutputString;
    }
    
    public void setCommandOutputString(String str) {
        commandOutputString = str;
    }

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
    
    public static int getCommandSuccessCode() {
        return COMMAND_SUCCESS;
    }
    
    public static int getCommandFailureCode() {
        return COMMAND_FAILURE;
    }
    
    public void showUsage() {
        if (spec.commandLine().getCommandSpec() != null) {
            commandOutputStreamWrite(String.format("%s\n\n", spec.commandLine().getCommandSpec().usageMessage().description()[0]));
            commandOutputStreamWrite(String.format("Usage: %s <cmd> [<args>]\n\n", spec.commandLine().getCommandSpec().name()));
            commandOutputStreamWrite("Available subcommands:\n");
            spec.commandLine().getCommandSpec().subcommands().entrySet().forEach(sub -> {
                commandOutputStreamWrite(String.format("  %s: %s\n", sub.getKey(), sub.getValue().getCommandSpec().usageMessage().description()[0]));
            });
            commandOutputStreamWrite(String.format("You may pass `--help' to any of these subcommands to view usage.\n"));
        }
    }

    public void commandOutputStreamWrite(String str) {
        if (commandOutputStream != null) {
            try {
                commandOutputStream.write(str.getBytes());
                commandOutputStream.flush();
            } catch (IOException ex) {
                log.error("Command output stream write exception: " + ex);
            }
        }
    }
    
    public String commandOutputStreamRead() {
        String output = "";
        try {
            output = commandOutputByteArrayStream.toString(StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException ex) {
            log.error("Encoding exception: " + ex);
        }
        
        return output;
    } 

    public boolean generateKeys() {
        executeSubcommand(GenKey.COMMAND);
        executeSubcommand(PubKey.COMMAND, privateKey);
        executeSubcommand(GenPsk.COMMAND);

        return (privateKey != null && publicKey != null && preSharedKey != null);
    }
    
    public boolean keysAreValid() {
        return (privateKey != null && publicKey != null && preSharedKey != null);
    }
    
    public String getPrivateKeyFromInterface(String ifName) {
        executeSubcommand(Show.COMMAND, ifName, Wg.OPTION_PRIVATE_KEY);
        return commandOutputString != null ? commandOutputString.trim() : null;
    }
    
    public String getPublicKeyFromInterface(String ifName) {
        executeSubcommand(Show.COMMAND, ifName, Wg.OPTION_PUBLIC_KEY);
        return commandOutputString != null ? commandOutputString.trim() : null;
    }
    
    public String getPresharedKeyFromInterface(String ifName) {
        executeSubcommand(Show.COMMAND, ifName, Wg.OPTION_PRESHARED_KEY);
        return commandOutputString != null ? commandOutputString.trim() : null;
    }
    
    public int getListenPortFromInterface(String ifName) {
        executeSubcommand(Show.COMMAND, ifName, Wg.OPTION_LISTEN_PORT);
        return commandOutputString != null ? Integer.parseInt(commandOutputString.trim()) : -1;
    }
    
    public String getFwmarkFromInterface(String ifName) {
        executeSubcommand(Show.COMMAND, ifName, Wg.OPTION_FWMARK);
        return commandOutputString != null ? commandOutputString.trim() : null;
    }
    
    public List<String> getPeersFromInterface(String ifName) {
        List<String> peers = new ArrayList<>();
        
        executeSubcommand(Show.COMMAND, ifName, Wg.OPTION_PEERS);
        if (commandOutputString != null) {
            try {
                BufferedReader reader = new BufferedReader(new StringReader(commandOutputString));
                String peer;
                while ((peer = reader.readLine()) != null) {
                    peers.add(peer);
                }
            } catch (IOException ex) {
                log.error("Exception: " + ex);
            }
        }

        return peers;
    }
    
    public Map<String, String> getEndpointsAsMap(String ifName) {
        Map<String, String> endpoints = new LinkedHashMap<>();
        
        executeSubcommand(Show.COMMAND, ifName, Wg.OPTION_ENDPOINTS);
        if (commandOutputString != null) {
            try {
                BufferedReader reader = new BufferedReader(new StringReader(commandOutputString));
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] split = line.split("\\s+");
                    if (split.length >= 2) {
                        endpoints.put(split[0], split[1]);
                    }
                }
            } catch (IOException ex) {
                log.error("Exception: " + ex);
            }
        }
        
        return endpoints;
    }
    
    public List<String> getEndpointsAsList(String ifName) {
        List<String> endpoints = new ArrayList<>();
        
        executeSubcommand(Show.COMMAND, ifName, Wg.OPTION_ENDPOINTS);
        if (commandOutputString != null) {
            try {
                BufferedReader reader = new BufferedReader(new StringReader(commandOutputString));
                String line;
                while ((line = reader.readLine()) != null) {
                    endpoints.add(line);
                }
            } catch (IOException ex) {
                log.error("Exception: " + ex);
            }
        }
        
        return endpoints;
    }
    
    public Map<String, String> getAllowedIpsAsMap(String ifName) {
        Map<String, String> allowedIps = new LinkedHashMap<>();
        
        executeSubcommand(Show.COMMAND, ifName, Wg.OPTION_ALLOWED_IPS);
        if (commandOutputString != null) {
            try {
                BufferedReader reader = new BufferedReader(new StringReader(commandOutputString));
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] split = line.split("\\s+");
                    allowedIps.put(split[0], split[1]);
                }
            } catch (IOException ex) {
                log.error("Exception: " + ex);
            }
        }
        
        return allowedIps;
    }
    
    public List<String> getAllowedIpsAsList(String ifName) {
        List<String> allowedIps = new ArrayList<>();
        
        executeSubcommand(Show.COMMAND, ifName, Wg.OPTION_ALLOWED_IPS);
        if (commandOutputString != null) {
            try {
                BufferedReader reader = new BufferedReader(new StringReader(commandOutputString));
                String line;
                while ((line = reader.readLine()) != null) {
                    allowedIps.add(line);
                }
            } catch (IOException ex) {
                log.error("Exception: " + ex);
            }
        }
        
        return allowedIps;
    }
    
    public Map<String, String> getPersistentKeepalivesAsMap(String ifName) {
        Map<String, String> persistentKeepalives = new LinkedHashMap<>();
        
        executeSubcommand(Show.COMMAND, ifName, Wg.OPTION_PERSISTENT_KEEPALIVE);
        if (commandOutputString != null) {
            try {
                BufferedReader reader = new BufferedReader(new StringReader(commandOutputString));
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] split = line.split("\\s+");
                    persistentKeepalives.put(split[0], split[1]);
                }
            } catch (IOException ex) {
                log.error("Exception: " + ex);
            }
        }
        
        return persistentKeepalives;
    }
    
    public List<String> getPersistentKeepalivesAsList(String ifName) {
        List<String> persistentKeepalives = new ArrayList<>();
        
        executeSubcommand(Show.COMMAND, ifName, Wg.OPTION_PERSISTENT_KEEPALIVE);
        if (commandOutputString != null) {
            try {
                BufferedReader reader = new BufferedReader(new StringReader(commandOutputString));
                String line;
                while ((line = reader.readLine()) != null) {
                    persistentKeepalives.add(line);
                }
            } catch (IOException ex) {
                log.error("Exception: " + ex);
            }
        }
        
        return persistentKeepalives;
    }
    
    public List<String> getDumpFromInterface(String ifName) {
        List<String> lines = new ArrayList<>();
        
        executeSubcommand(Show.COMMAND, ifName, Wg.OPTION_DUMP);
        if (commandOutputString != null) {
            try {
                BufferedReader reader = new BufferedReader(new StringReader(commandOutputString));
                String line;
                while ((line = reader.readLine()) != null) {
                    lines.add(line);
                }
            } catch (IOException ex) {
                log.error("Exception: " + ex);
            }
        }
        
        return lines;
    }
    
    public static synchronized InterfaceDeviceManager getLinkDeviceManager() {
        return deviceMgr;
    }
    
    public int setWireguardNetworkDevicePrivateKey(String deviceName, String privateKey) {
        return new CommandLine(new Wg()).execute("set " + deviceName + " " + " private-key " + privateKey);
    }

    public void loadNativeLibrary() {
        try {
            StringBuilder nativeLibraryPathname = new StringBuilder(WG_NATIVE_LIBRARY_BASE_PATH);
            String osName = System.getProperty("os.name");
            String osArch = System.getProperty("os.arch");

            if (StringUtils.containsIgnoreCase(osName, OS_NAME_AIX)   || StringUtils.containsIgnoreCase(osName, OS_NAME_BSD_UNIX) ||
                StringUtils.containsIgnoreCase(osName, OS_NAME_LINUX) || StringUtils.containsIgnoreCase(osName, OS_NAME_OSX) ||
                StringUtils.containsIgnoreCase(osName, OS_NAME_WINDOWS)) {
                nativeLibraryPathname.append(osName.toLowerCase()).append("_");
            } else {
                nativeLibraryPathname.append(OS_NAME_DEFAULT.toLowerCase()).append("_");
            }

            if (StringUtils.contains(osArch, OS_ARCH_64) || StringUtils.contains(osArch, OS_ARCH_32)) {
                nativeLibraryPathname.append(osArch);
            } else {
                nativeLibraryPathname.append(OS_ARCH_DEFAULT);
            }

            NativeLoader.loadLibrary(WG_NATIVE_LIBRARY_NAME, nativeLibraryPathname.toString());
            libraryFileLoaded = true;
        } catch (IOException ex) {
            log.error("Load library exception: " + ex);
            System.err.println("Native code library failed to load.\n");
            commandErrorString = "Native code library failed to load.";
            commandExitCode = 1;
        }
    }
    
    /**
     * @param args the command line arguments
     */
    public void executeSubcommand(String... args) {
        commandOutputByteArrayStream.reset();
        
        CommandLine commandLine = new CommandLine(this);
        
        if (!libraryFileLoaded) {
            loadNativeLibrary();
        }
        
        if (libraryFileLoaded) {
            // If there are args, execute the subcommand, else run the basic show subcommand
            if (args.length > 0) {
                commandLine.execute(args);
            } else {
                String[] new_argv = {"show"};
                commandLine.execute(new_argv);
            }
        }
    }
    
    @Override
    public void run() {
        log.info("helpRequested = {}, versionRequested = {}\n", helpRequested, versionRequested);
        
        // check the command line arguments for the help and version options
        if (helpRequested) {
            showUsage();
        } else if (versionRequested) {
            commandOutputStreamWrite(String.format("wireguard-tools v%s - https://git.zx2c4.com/wireguard-tools/\n", Version.WIREGUARD_TOOLS_VERSION));
        }
        
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String... args) {
        Wg wg = new Wg();
        wg.executeSubcommand(args);
        System.out.print(wg.commandOutputStreamRead());
    }
}
