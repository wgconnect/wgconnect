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

import picocli.CommandLine;
import picocli.CommandLine.Parameters;

/**
 * SyncConf
 * 
 * @author: wgconnect@proton.me
 */
@CommandLine.Command(name = SyncConf.COMMAND, description = SyncConf.DESCRIPTION)
public class SyncConf extends WgSubcommand {
    
    private static final WgConnectLogger log = WgConnectLogger.getLogger(SyncConf.class);    

    public static final String COMMAND = "syncconf";
    public static final String DESCRIPTION = "Synchronizes a configuration file to a WireGuard interface";

    @CommandLine.Option(names = {"-h", "--help"}, defaultValue = "false")
    private boolean helpRequested;
    
    @Parameters(index = "0", arity = "1", description = "interface name")
    String interfaceName;
    
    @Parameters(index = "1", arity = "1", description = "configuration filename")
    String configurationFilename;
    
    public void showUsage() {
        try {
            parent.getCommandOutputStream().write(
                String.format("Usage: %s %s <interface> <configuration filename>\n",
                    parent.spec.commandLine().getCommandName(), SyncConf.COMMAND).getBytes());
        } catch (IOException ex) {
            log.error("SyncConf showUsage exception: " + ex);
        }
    }
    
    @Override
    public void run() {
        if (helpRequested) {
            showUsage();
            parent.setCommandExitCode(0);
            return;
        }
        
        String[] args = { SyncConf.COMMAND, interfaceName, configurationFilename };
        Object[] results = wgCommand(args);
        parent.setCommandExitCode((Integer)results[0]);
        parent.setCommandOutputString((String)results[1]);
        parent.setCommandErrorString((String)results[2]);
    }   
}
