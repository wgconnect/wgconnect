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
package com.wgconnect.core.message;

import com.wgconnect.core.option.base.Option;

/**
 * Message
 * 
 * @author JagornetDhcp version: A. Gregory Rabil
 * @author WgConnect version: wgconnect@proton.me
 */
public interface Message {
	
    /**
     * Sets the message type.
     *
     * @param msgType the new message type
     */
	public void setMessageType(short msgType);
	
    /**
     * Gets the message type.
     *
     * @return the message type
     */
	public short getMessageType();
	
    /**
     * Sets the message sender.
     *
     * @param msgSender
     */
	public void setMessageSender(short msgSender);
    
    /**
     * Gets the message sender.
     *
     * @return the message sender
     */
	public short getMessageSender();
	
    /**
     * Gets the WgConnect option.
     *
     * @param optionCode the option code
     * @return the WgConnect option
     */
	public Option getOption(int optionCode);
}
