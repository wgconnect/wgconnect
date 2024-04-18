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
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

/**
 * Show
 * 
 * @author: wgconnect@proton.me
 */
@Command(name = Show.COMMAND, description = Show.DESCRIPTION)
public class Show extends WgSubcommand {
    
    private static final WgConnectLogger log = WgConnectLogger.getLogger(Show.class);    

    public static final String COMMAND = "show";
    public static final String DESCRIPTION = "Shows the current configuration and device information";

    @CommandLine.Option(names = {"-h", "--help"}, defaultValue = "false")
    private boolean helpRequested;
    
    @Parameters(arity = "0..*")
    private String[] params;
            
    public void showUsage() {
        try {
            parent.getCommandOutputStream().write(
                String.format(
                    "Usage: %s %s { <interface> | all | interfaces } [public-key | private-key | listen-port | fwmark | peers | " +
                    "preshared-keys | endpoints | allowed-ips | latest-handshakes | transfer | persistent-keepalive | dump]\n",
                    parent.spec.commandLine().getCommandSpec().name(), Show.COMMAND).getBytes());
        } catch (IOException ex) {
            log.error("Show showUsage exception: " + ex);
        }
    }

    @Override
    public void run() {
        if (helpRequested) {
            showUsage();
            parent.setCommandExitCode(0);
            return;
        }

        String[] args = { Show.COMMAND };
        if (params != null)
            args = ArrayUtils.addAll(args, params);
        
        Object[] results = wgCommand(args);
        parent.setCommandExitCode((Integer)results[0]);
        parent.setCommandOutputString((String)results[1]);
        parent.setCommandErrorString((String)results[2]);
    }
}
