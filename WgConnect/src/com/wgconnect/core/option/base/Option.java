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
package com.wgconnect.core.option.base;

/**
 * Option
 * 
 * The main interface for options.
 *
 * @author JagornetDhcp version: A. Gregory Rabil
 * @author WgConnect version: wgconnect@proton.me
 */
public interface Option extends Encodable, Decodable {

    /**
     * Gets the option code. Java int for unsigned short.
     *
     * @return the code for the option
     */
    public int getCode();

    /**
     * Gets the option name.
     *
     * @return the name of the option
     */
    public String getName();

    /**
     * Gets the length. Java int for unsigned short.
     *
     * @return the length of the option in bytes
     */
    public int getLength();

    /**
     * Return a string representation of this option.
     *
     * @return the string
     */
    @Override
    public String toString();

    /**
     * Return true if this is a V4 option, false otherwise.
     *
     * @return true if this is a V4 option, false otherwise.
     */
    public boolean isV4();
    
}
