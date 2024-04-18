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

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.wgconnect.core.option.base.Option;
import com.wgconnect.core.option.machine.MsgSenderOption;
import com.wgconnect.core.option.machine.V6OptionFactory;
import com.wgconnect.core.util.Constants;
import com.wgconnect.core.util.Utils;
import com.wgconnect.core.util.WgConnectLogger;

import java.util.Arrays;
import org.apache.commons.lang3.StringUtils;


/**
 * V6Message
 *               
 * The following diagram illustrates the format of V6 messages sent
 * between clients and servers:
 *
 * <pre>
 *   0                   1                   2                   3
 *   0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
 *   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *   |  msg-type(1)  |  padding1 (1) |  padding2 (1) |  padding3 (1) |
 *   +-------------------------------+-------------------------------+
 *   |                        transaction-id(4)                      |
 *   +-------------------------------+-------------------------------+
 *   |                                                               |
 *   |                            options                            |
 *   |                           (variable)                          |
 *   |                                                               |
 *   +---------------------------------------------------------------+
 *
 *   msg-type             Identifies the V6 message type
 *
 *   transaction-id       The transaction ID for this message exchange.
 *
 *   options              Options carried in this message
 *
 *   The format of V6 options is:
 *
 *   0                   1                   2                   3
 *   0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
 *   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *   |          option-code          |           option-len          |
 *   +-------------------------------+-------------------------------+
 *   |                       option-opaqueData                       |
 *   |                      (option-len octets)                      |
 *   +---------------------------------------------------------------+
 * 
 *   option-code   An unsigned integer identifying the specific option
 *                 type carried in this option.
 * 
 *   option-len    An unsigned integer giving the length of the
 *                 option-opaqueData field in this option in octets.
 * 
 *   option-opaqueData   The opaqueData for the option; the format of this opaqueData
 *                 depends on the definition of the option.
 * </pre>
 *
 * @author JagornetDhcp version: A. Gregory Rabil
 * @author WgConnect version: wgconnect@proton.me
 */

public class V6Message implements Message {

    private static final WgConnectLogger log = WgConnectLogger.getLogger(V6Message.class);

    // true if the message was received on a unicast socket
    protected boolean unicast;

    // the IP and port on the local host on which the message is sent or received
    protected InetSocketAddress localAddress;

    // the IP and port on the remote host from which the message is received or sent
    protected InetSocketAddress remoteAddress;

    protected short messageType = 0; // need a short to hold unsigned byte
    protected short padding1 = 0;
    protected short padding2 = 0;
    protected short padding3 = 0;
    protected long transactionId = 0; // need a long to hold unsigned int
    protected Map<Integer, Option> options = new HashMap<>();

    /**
     * Construct a V6Message.
     * 
     * @param localAddress  InetSocketAddress on the local host on which
     *                      this message is received or sent
     * @param remoteAddress InetSocketAddress on the remote host on which
     *                      this message is sent or received
     */
    public V6Message(InetSocketAddress localAddress, InetSocketAddress remoteAddress) {
        this.localAddress = localAddress;
        this.remoteAddress = remoteAddress;
    }

    /**
     * Set the unicast flag for this message.
     * 
     * @param unicast
     */
    public void setUnicast(boolean unicast) {
        this.unicast = unicast;
    }

    /**
     * Check if this message was received via unicast.
     * 
     * @return true if unicast message, false otherwise
     */
    public boolean isUnicast() {
        return unicast;
    }

    /**
     * Encode this V6Message to wire format for sending.
     * 
     * @return a ByteBuffer containing the encoded V6Message
     * @throws IOException
     */
    public ByteBuffer encode() throws IOException {
        log.info("Encoding V6Message for: {}", Utils.socketAddressAsString(remoteAddress));

        int len = encodedOptionsLength();
        len += 8; // msg-type + padding1 + padding2 + padding3 + transactionId
        
        ByteBuffer buf = ByteBuffer.allocate(len);
        buf.put((byte) messageType);
        buf.put((byte) padding1);
        buf.put((byte) padding2);
        buf.put((byte) padding3);
        buf.putInt((int) transactionId);
        ByteBuffer b = encodeOptions();
        buf.put(b);
        buf.flip();

        log.info("V6Message encoded.");

        return buf;
    }

    /**
     * Encode the options of this V6Message to wire format for sending.
     * 
     * @return a ByteBuffer containing the encoded options
     * @throws IOException
     */
    protected ByteBuffer encodeOptions() throws IOException {
        // 1024 - 1(msgType) - 3(transId)
        ByteBuffer buf = ByteBuffer.allocate(1020);

        if (options != null) {
            for (Option option : options.values()) {
                buf.put(option.encode());
            }
        }

        return (ByteBuffer) buf.flip();
    }

    protected int encodedOptionsLength() {
        int len = 0;
        if (options != null) {
            for (Option option : options.values()) {
                len += (2 * Constants.INTEGER_SIZE); // code + length registers
                len += option.getLength();
            }       
        }

        return len;
    }
    
    /**
     * Decode a packet received on the wire into a V6Message object.
     * 
     * @param buf        ByteBuffer containing the packet to be decoded
     * @param localAddr  InetSocketAddress on the local host on which
     *                   packet was received
     * @param remoteAddr InetSocketAddress on the remote host from which
     *                   the packet was received
     * @return a decoded DhcpMessage object, or null if the packet could not be decoded
     * @throws IOException
     */

    public static V6Message decode(ByteBuffer buf, InetSocketAddress localAddr, InetSocketAddress remoteAddr)
        throws IOException {
        V6Message dhcpMessage = null;
        if ((buf != null) && buf.hasRemaining()) {
            log.info("Decoding packet: buf.hasRemaining = {}, buf.limit = {}, localAddr = {}, remoteAddr = {}",
                buf.hasRemaining(), buf.limit(), Utils.socketAddressAsString(localAddr),
                Utils.socketAddressAsString(remoteAddr));

            // we'll "peek" at the message type to use for this mini-factory
            buf.mark();
            byte msgtype = buf.get();
            log.info("Message type byte = {}", msgtype);
            
            if ((msgtype >= Constants.V6_MESSAGE_TYPE_SOLICIT) && (msgtype < Constants.V6_MESSAGE_TYPE_END)) {
                dhcpMessage = new V6Message(localAddr, remoteAddr);
            } else {
                log.error("Unknown message type: " + msgtype);
            }

            if (dhcpMessage != null) {
                // reset the buffer to point at the message type byte
                // because the message decoder will expect it
                buf.reset();
                dhcpMessage.decode(buf);
            }
        } else {
            String errmsg = "Buffer is null or empty";
            log.error(errmsg);
            throw new IOException(errmsg);
        }
        
        return dhcpMessage;
    }

    /**
     * Decode a datagram packet into this V6Message object.
     *  
     * @param buf ByteBuffer containing the packet to be decoded
     * @throws IOException
     */
    public void decode(ByteBuffer buf) throws IOException {
        log.info("Decoding V6Message from: {}", Utils.socketAddressAsString(remoteAddress));
        
        if ((buf != null) && buf.hasRemaining()) {
            decodeMessageType(buf);
            if (buf.hasRemaining()) {
                padding1 = buf.get();
                padding2 = buf.get();
                padding3 = buf.get();

                transactionId = buf.getInt();
                log.info("TransactionId = {}", transactionId);
                
                if (buf.hasRemaining()) {
                    decodeOptions(buf);
                } else {
                    String errmsg = "Failed to decode options: buffer is empty";
                    log.error(errmsg);
                    throw new IOException(errmsg);
                }
            } else {
                String errmsg = "Failed to decode transaction id: buffer is empty";
                log.error(errmsg);
                throw new IOException(errmsg);
            }
        } else {
            String errmsg = "Failed to decode message: buffer is empty";
            log.error(errmsg);
            throw new IOException(errmsg);
        }

        log.info("Message decoded.");
    }

    /**
     * Decode the message type.
     * 
     * @param buf ByteBuffer positioned at the message type in the packet
     * @throws IOException
     */
    protected void decodeMessageType(ByteBuffer buf) throws IOException {
        if ((buf != null) && buf.hasRemaining()) {
            setMessageType(Utils.getUnsignedByte(buf));
            log.info("MessageType = {}", Constants.getV6MsgTypeString(messageType));
        } else {
            String errmsg = "Failed to decode message type: buffer is empty";
            log.error(errmsg);
            throw new IOException(errmsg);
        }
    }

    /**
     * Decode the options.
     * @param buf ByteBuffer positioned at the start of the options in the packet
     * @return a Map of DhcpOptions keyed by the option code
     * @throws IOException
     */
    protected Map<Integer, Option> decodeOptions(ByteBuffer buf) throws IOException {
        while (buf.hasRemaining()) {
            if (buf.remaining() < Short.BYTES) {
                break;
            }
            
            int code = buf.getInt();
            if (code >= Constants.OPTION_MESSAGE_TYPE && code <= Constants.OPTION_EOF) {
                log.info("Option code = {}", code);

                Option option = V6OptionFactory.getDhcpOption(code);
                if (option != null) {
                    option.decode(buf);
                    options.put(option.getCode(), option);
                } else {
                    break;  // no more options, or one is malformed, so we're done
                }
            }
        }
        
        return options;
    }

    /**
     * Return the length of this V6Message in bytes.
     * @return an int containing a length of a least four(4)
     */
    public int getLength() {
        int len = 4;    // msg type (1) + transaction id (3)
        len += getOptionsLength();
        
        return len;
    }

    /**
     * Get the length of the options in this DhcpMessage in bytes.
     * @return an int containing the total length of all options
     */
    protected int getOptionsLength() {
        int len = 0;
        if (options != null) {
            for (Option option : options.values()) {
                len += 4;   // option code (2 bytes) + length (2 bytes) 
                len += option.getLength();
            }
        }
        
        return len;
    }

    public InetSocketAddress getLocalAddress() {
        return localAddress;
    }

    public void setLocalAddress(InetSocketAddress localAddress) {
        this.localAddress = localAddress;
    }

    public InetSocketAddress getRemoteAddress() {
        return remoteAddress;
    }

    public void setRemoteAddress(InetSocketAddress remoteAddress) {
        this.remoteAddress = remoteAddress;
    }

    @Override
    public short getMessageType() {
        return messageType;
    }

    @Override
    public void setMessageType(short messageType) {
        this.messageType = messageType;
    }

    @Override
    public short getMessageSender() {
        MsgSenderOption msgSender = (MsgSenderOption) options.get(Constants.OPTION_MESSAGE_SENDER);
        if (msgSender != null) {
            return msgSender.getUnsignedByte();
        }
        
        return 0;
    }

    @Override
    public void setMessageSender(short messageSender) {
        MsgSenderOption msgSenderOption = new MsgSenderOption(false);
        msgSenderOption.setUnsignedByte(messageSender);
        putOption(msgSenderOption);
    }
    
    public long getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(long transactionId) {
        this.transactionId = transactionId;
    }

    public boolean hasOption(int optionCode) {
        return options.containsKey(optionCode);
    }

    @Override
    public Option getOption(int optionCode) {
        return options.get(optionCode);
    }

    public void putOption(Option option) {
        if (option != null) {
            options.put(option.getCode(), option);
        }
    }

    public void putAllOptions(Map<Integer, Option> options) {
        this.options.putAll(options);
    }

    public Map<Integer, Option> getOptionMap() {
        return options;
    }

    public void setOptionMap(Map<Integer, Option> options) {
        this.options = options;
    }

    public Collection<Option> getOptions() {
        return options.values();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(Utils.LINE_SEPARATOR);
        
        sb.append(Constants.getV6MsgSenderString(getMessageSender()));
        sb.append(StringUtils.SPACE);
        sb.append(Constants.getV6MsgTypeString(getMessageType()));
        sb.append(" (xactId = ");
        sb.append(getTransactionId());
        sb.append(')');
        if ((messageType == Constants.V6_MESSAGE_TYPE_ADVERTISE) ||
            (messageType == Constants.V6_MESSAGE_TYPE_REPLY ||
            (messageType == Constants.V6_MESSAGE_TYPE_TUNNEL_PING))) {
            sb.append(" to ");
        } else {
            sb.append(" from ");
        }
        sb.append(Utils.socketAddressAsString(remoteAddress));
        
        return sb.toString();
    }

    public String toString(int localDst) {
        StringBuilder sb = new StringBuilder(Utils.LINE_SEPARATOR);
        
        sb.append(Constants.getV6MsgTypeString(getMessageType()));
        sb.append(" (xactId = ");
        sb.append(getTransactionId());
        sb.append(')');
        if (localDst == Constants.V6_MESSAGE_SENDER_CLIENT && getMessageSender() == Constants.V6_MESSAGE_SENDER_SERVER ||
            localDst == Constants.V6_MESSAGE_SENDER_SERVER && getMessageSender() == Constants.V6_MESSAGE_SENDER_CLIENT) {
            sb.append(" to ");
        } else {
            sb.append(" from ");
        }
        
        sb.append(Utils.socketAddressAsString(remoteAddress));
        
        return sb.toString();
    }
    
    public String toStringWithOptions(int localDst) {
        StringBuilder sb = new StringBuilder(this.toString(localDst));
        if ((options != null) && !options.isEmpty()) {
            sb.append(Utils.LINE_SEPARATOR);
            sb.append("MSG_OPTIONS");
            for (Option option : options.values()) {
                sb.append(option.toString());
            }
        }

        return sb.toString();
    }
}
