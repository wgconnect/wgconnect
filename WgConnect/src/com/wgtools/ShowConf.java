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
 * ShowConf
 * 
 * @author: wgconnect@proton.me
 */
@CommandLine.Command(name = ShowConf.COMMAND, description = ShowConf.DESCRIPTION)
public class ShowConf extends WgSubcommand {
    
    private static final WgConnectLogger log = WgConnectLogger.getLogger(ShowConf.class);    

    public static final String COMMAND = "showconf";
    public static final String DESCRIPTION = "Shows the current configuration of a given WireGuard interface, for use with `setconf'";
    
    @CommandLine.Option(names = {"-h", "--help"}, defaultValue = "false")
    private boolean helpRequested;
    
    @Parameters(arity = "1")
    String interfaceName;
        
    public void showUsage() {
        try {
            parent.getCommandOutputStream().write(
                String.format("Usage: %s %s <interface>\n",
                    parent.spec.commandLine().getCommandName(), ShowConf.COMMAND).getBytes());
        } catch (IOException ex) {
            log.error("ShowConf showUsage exception: " + ex);
        }
    }

    @Override
    public void run() {
        if (helpRequested) {
            showUsage();
            parent.setCommandExitCode(0);
            return;
        }
        
        String[] args = { ShowConf.COMMAND, interfaceName };
        Object[] results = wgCommand(args);
        parent.setCommandExitCode((Integer)results[0]);
        parent.setCommandOutputString((String)results[1]);
        parent.setCommandErrorString((String)results[2]);
    }
}
