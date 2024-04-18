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
package com.wgconnect.config;

import com.wgconnect.core.util.WgConnectLogger;

/**
 * ConfigException.
 * 
 * @author JagornetDhcp version:
 * @author WgConnect version: wgconnect@proton.me
 */
public class ConfigException extends Exception {

    private static final WgConnectLogger log = WgConnectLogger.getLogger(ConfigException.class);

    private static final long serialVersionUID = 2214032364910282642L;

    /**
     * Instantiates a new ConfigException exception.
     */
    public ConfigException() {
        super();
    }

    /**
     * Instantiates a new ConfigException exception.
     *
     * @param message the message
     */
    public ConfigException(String message) {
        super(message);
    }

    /**
     * Instantiates a new ConfigException exception.
     *
     * @param message the message
     * @param throwable the throwable
     */
    public ConfigException(String message, Throwable throwable) {
        super(message, throwable);
    }

    /**
     * Instantiates a new ConfigException exception.
     *
     * @param throwable the throwable
     */
    public ConfigException(Throwable throwable) {
        super(throwable);
    }
}
