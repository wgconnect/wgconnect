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

import java.io.IOException;

import org.apache.commons.lang3.ArrayUtils;

import picocli.CommandLine;
import picocli.CommandLine.Parameters;

/**
 * Set
 * 
 * @author: wgconnect@proton.me
 */
@CommandLine.Command(name = Set.COMMAND, description = Set.DESCRIPTION)
public class Set extends WgSubcommand {
    
    private static final WgConnectLogger log = WgConnectLogger.getLogger(Set.class);    

    public static final String COMMAND = "set";
    public static final String DESCRIPTION = "Change the current configuration, add peers, remove peers, or change peers";
    
    @CommandLine.Option(names = {"-h", "--help"}, defaultValue = "false")
    private boolean helpRequested;
    
    @Parameters(arity="2..*")
    String[] params;
    
    public void showUsage() {
        try {
            parent.getCommandOutputStream().write(
                String.format("Usage: %s %s <interface> [listen-port <port>] [fwmark <mark>] [private-key <file path>]" +
                    " [peer <base64 public key> [remove] [preshared-key <file path>] [endpoint <ip>:<port>] " + 
                    "[persistent-keepalive <interval seconds>] [allowed-ips <ip1>/<cidr1>[,<ip2>/<cidr2>]...] ]...\n",
            parent.spec.commandLine().getCommandName(), Set.COMMAND).getBytes());
        } catch (IOException ex) {
            log.error("Set showUsage exception: " + ex);
        }
    }
    
    @Override
    public void run() {
        parent.setCommandExitCode(0);
        if (helpRequested) {
            showUsage();
            return;
        }

        String[] args = { Set.COMMAND };
        if (params != null)
            args = ArrayUtils.addAll(args, params);
        Object[] results = wgCommand(args);
        parent.setCommandExitCode((Integer)results[0]);
        parent.setCommandOutputString((String)results[1]);
        parent.setCommandErrorString((String)results[2]);
    }
}
