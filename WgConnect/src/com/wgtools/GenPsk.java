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

/**
 * GenPsk
 * 
 * @author: wgconnect@proton.me
 */
@CommandLine.Command(name = GenPsk.COMMAND, description = GenPsk.DESCRIPTION)
public class GenPsk extends WgSubcommand {
    
    private static final WgConnectLogger log = WgConnectLogger.getLogger(GenPsk.class);    

    public static final String COMMAND = "genpsk";
    public static final String DESCRIPTION = "Generates a new preshared key";
    
    @CommandLine.Option(names = {"-h", "--help"}, defaultValue = "false")
    private boolean helpRequested;
    
    public void showUsage() {
        try {
            parent.getCommandOutputStream().write(
                String.format("Usage: %s %s\n",  parent.spec.commandLine().getCommandName(), GenPsk.COMMAND).getBytes());
        } catch (IOException ex) {
            log.error("GenPsk showUsage exception: " + ex);
        }
    }
    
    @Override
    public void run() {
        parent.setCommandExitCode(0);
        if (helpRequested) {
            showUsage();
            return;
        }
        
        String[] args = { GenPsk.COMMAND };
        Object[] results = wgCommand(args);
        parent.setCommandExitCode((Integer)results[0]);
        parent.setPreSharedKey((String)results[1]);
        parent.setCommandErrorString((String)results[2]);
    }   
}
