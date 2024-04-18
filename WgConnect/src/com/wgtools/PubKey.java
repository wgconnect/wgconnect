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
 * PubKey
 * 
 * @author: wgconnect@proton.me
 */
@CommandLine.Command(name = PubKey.COMMAND, description = PubKey.DESCRIPTION)
public class PubKey extends WgSubcommand {
    
    private static final WgConnectLogger log = WgConnectLogger.getLogger(PubKey.class);    

    public static final String COMMAND = "pubkey";
    public static final String DESCRIPTION = "Generates a public key from a private key";
    
    @CommandLine.Option(names = {"-h", "--help"}, defaultValue = "false")
    private boolean helpRequested;
    
    @Parameters(arity = "1")
    String privateKey;
    
    public void showUsage() {
        try {
            parent.getCommandOutputStream().write(
                String.format("Usage: %s %s <private key>\n",  parent.spec.commandLine().getCommandName(), PubKey.COMMAND).getBytes());
        } catch (IOException ex) {
            log.error("PubKey showUsage exception: " + ex);
        }
    }
    
    @Override
    public void run() {
        parent.setCommandExitCode(0);
        if (helpRequested || privateKey == null) {
            showUsage();
            return;
        }
        
        String[] args = { PubKey.COMMAND, privateKey };
        Object[] results = wgCommand(args);
        parent.setCommandExitCode((Integer)results[0]);
        parent.setPublicKey((String)results[1]);
        parent.setCommandErrorString((String)results[2]);
    }
}
