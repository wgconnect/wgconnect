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
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.wgconnect.core.option.machine.MsgTypeOption;
import com.wgconnect.core.option.machine.V4OptionFactory;
import com.wgconnect.core.util.Constants;
import com.wgconnect.core.util.Utils;
import com.wgconnect.core.util.WgConnectLogger;
import com.wgconnect.core.option.base.Option;
import com.wgconnect.core.option.machine.MsgSenderOption;

import org.apache.commons.lang3.StringUtils;

/**
 * V4Message
 *               
 * The following diagram illustrates the format of V4 messages sent
 * between clients and servers:
 *
 * <pre>
 *   0                   1                   2                   3
 *   0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
 *   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *   |     op (1)    |   htype (1)   |  padding1 (1) |  padding2 (1) |
 *   +---------------+---------------+---------------+---------------+
 *   |                            xid (4)                            |
 *   +-------------------------------+-------------------------------+
 *   |                          clientAddr (4)                       |
 *   +-------------------------------+-------------------------------+
 *   |                          clientPort (4)                       |
 *   +---------------------------------------------------------------+
 *   |                          serverAddr (4)                       |
 *   +-------------------------------+-------------------------------+
 *   |                          serverPort (4)                       |
 *   +---------------------------------------------------------------+
 *   |                                                               |
 *   |                          options (variable)                   |
 *   +---------------------------------------------------------------+
 * </pre>
 *
 * @author JagornetDhcp version: A. Gregory Rabil
 * @author WgConnect version: wgconnect@proton.me
 */

public class V4Message implements Message {

    private static final WgConnectLogger log = WgConnectLogger.getLogger(V4Message.class);

    // true if the message was received on a unicast socket, false otherwise
    protected boolean unicast;

    // the IP and port on the local host on which the message is sent or received
    protected InetSocketAddress localAddress;

    // the IP and port on the remote host from which the message is received or sent
    protected InetSocketAddress remoteAddress;

    protected short op = 0;	// need a short to hold unsigned byte
    protected short htype = 0;
    protected short padding1 = 0;
    protected short padding2 = 0;
    protected long transactionId = 0; // need a long to hold unsigned int
    protected InetAddress clientAddr = Constants.V4_ZEROADDR;
    protected long clientPort = 0;
    protected InetAddress serverAddr = Constants.V4_ZEROADDR;
    protected long serverPort = 0;
    protected static byte[] magicCookie = new byte[]{(byte) 99, (byte) 130, (byte) 83, (byte) 99};
    protected Map<Integer, Option> options = new HashMap<>();

    /**
     * Construct a V4Message.
     *
     * @param localAddress  InetSocketAddress on the local host on which this message is received or sent
     * @param remoteAddress InetSocketAddress on the remote host on which this message is sent or received
     */
    public V4Message(InetSocketAddress localAddress, InetSocketAddress remoteAddress) {
        this.localAddress = localAddress;
        this.remoteAddress = remoteAddress;
        log.setDebugEnabled(false);
    }

    public void setUnicast(boolean unicast) {
        this.unicast = unicast;
    }

    public boolean isUnicast() {
        return unicast;
    }

    /**
     * Encode this V4Message to wire format for sending.
     *
     * @return a ByteBuffer containing the encoded V4Message
     * @throws IOException
     */
    public ByteBuffer encode() throws IOException {
        if (log.isDebugEnabled()) {
            log.info("Encoding V4Message for: " + Utils.socketAddressAsString(remoteAddress));
        }

        int len = encodedOptionsLength();
        len += 24; // op + htype + padding1 + padding2 + transactionId + clientAddr + clientPort + serverAddr + serverPort
        
        ByteBuffer buf = ByteBuffer.allocate(len);
        buf.put((byte) op);
        buf.put((byte) htype);
        buf.put((byte) padding1);
        buf.put((byte) padding2);
        buf.putInt((int) transactionId);

        if (clientAddr != null) {
            buf.put(clientAddr.getAddress());
        } else {
            buf.put(Constants.V4_ZEROADDR.getAddress());
        }
        
        buf.putInt((int) clientPort);

        if (serverAddr != null) {
            buf.put(serverAddr.getAddress());
        } else {
            buf.put(Constants.V4_ZEROADDR.getAddress());
        }
        
        buf.putInt((int) serverPort);
        
        buf.put(encodeOptions());
        int msglen = buf.position();
        if (log.isDebugEnabled()) {
            log.info("V4 Message is " + msglen + " bytes");
        }
        buf.flip();

        if (log.isDebugEnabled()) {
            log.info("V4 Message encoded.");
        }
        
        return buf;
    }

    /**
     * Encode the options of this Message to wire format for sending.
     *
     * @return a ByteBuffer containing the encoded options
     * @throws IOException
     */
    protected ByteBuffer encodeOptions() throws IOException {
        // 1024 - 24(op(1) + htype(1) + padding1(1) + padding2(1) + transactionId(4) + clientAddr(4) + clientPort(4) + serverAddr(4) + serverPort(4))
        ByteBuffer buf = ByteBuffer.allocate(1000);
        
        if (options != null) {
            buf.put(magicCookie);
            for (Option option : options.values()) {
                buf.put(option.encode());
            }
            buf.put((byte) Constants.OPTION_EOF); // end option
        }
                
        return (ByteBuffer) buf.flip();
    }

    protected int encodedOptionsLength() {
        int len = 0;
        if (options != null) {
            len += magicCookie.length;
            
            for (Option option : options.values()) {
                len += (2 * Constants.INTEGER_SIZE); // code + length registers
                len += option.getLength();
            }
            
            len += Constants.INTEGER_SIZE; // end option
        }

        return len;
    }
    
    /**
     * Decode a packet received on the wire into a V4Message object.
     *
     * @param buf ByteBuffer containing the packet to be decoded
     * @param localAddr InetSocketAddress on the local host on which packet was received
     * @param remoteAddr InetSocketAddress on the remote host from which the packet was received
     * @return a decoded V4Message object, or null if the packet could not be decoded
     * @throws IOException
     */
    public static V4Message decode(ByteBuffer buf, InetSocketAddress localAddr, InetSocketAddress remoteAddr)
        throws IOException {
        V4Message message = null;
        if ((buf != null) && buf.hasRemaining()) {
            if (log.isDebugEnabled()) {
                log.info("Decoding packet: size = {} localAddr = {} remoteAddr = {}",
                    buf.limit(), Utils.socketAddressAsString(localAddr), Utils.socketAddressAsString(remoteAddr));
            }
            
            // we'll "peek" at the message type to use for this mini-factory
            buf.mark();
            byte _op = buf.get();
            if (log.isDebugEnabled()) {
                log.info("op byte = {}", _op);
            }

            // allow for reply messages for use by client
            if ((_op == Constants.V4_OP_REQUEST) || (_op == Constants.V4_OP_REPLY)) {
                message = new V4Message(localAddr, remoteAddr);
            } else {
                log.error("Unsupported op code: " + _op);
            }

            if (message != null) {
                // reset the buffer to point at the message type byte because the message decoder will expect it
                buf.reset();
                message.decode(buf);
            }
        } else {
            String errmsg = "Buffer is null or empty";
            log.error(errmsg);
            throw new IOException(errmsg);
        }

        return message;
    }

    /**
     * Decode a datagram packet into this V4Message object.
     *
     * @param buf ByteBuffer containing the packet to be decoded
     * @throws IOException
     */
    public void decode(ByteBuffer buf) throws IOException {
        log.info("Decoding V4 message from: {}", Utils.socketAddressAsString(remoteAddress));
        
        if ((buf != null) && buf.hasRemaining()) {
            op = buf.get();
            log.info("op = {}", op);

            htype = buf.get();
            log.info("htype = {}", htype);
            
            padding1 = buf.get();
            padding2 = buf.get();

            transactionId = buf.getInt();
            log.info("TransactionId = {}", transactionId);
            
            byte[] ipbuf = new byte[4];
            buf.get(ipbuf);
            clientAddr = InetAddress.getByAddress(ipbuf);
            clientPort = buf.getInt();
            
            buf.get(ipbuf);
            serverAddr = InetAddress.getByAddress(ipbuf);
            serverPort = buf.getInt();
            
            if (log.isDebugEnabled()) {
                log.info("clientAddr = {}, clientPort = {}", clientAddr.getHostAddress(), clientPort);
                log.info("serverAddr = {}, serverPort = {}", serverAddr.getHostAddress(), serverPort);
            }
            
            byte[] cookieBuf = new byte[4];
            buf.get(cookieBuf);
            if (!Arrays.equals(cookieBuf, magicCookie)) {
                String errmsg = "Failed to decode V4 message: invalid magic cookie";
                log.error(errmsg);
                throw new IOException(errmsg);
            }

            decodeOptions(buf);
        } else {
            String errmsg = "Failed to decode message: buffer is empty";
            log.error(errmsg);
            throw new IOException(errmsg);
        }

        if (log.isDebugEnabled()) {
            log.info("V4 message decoded.");
        }
    }

    /**
     * Decode the options.
     *
     * @param buf ByteBuffer positioned at the start of the options in the packet
     * @return a Map of Options keyed by the option code
     * @throws IOException
     */
    protected Map<Integer, Option> decodeOptions(ByteBuffer buf) throws IOException {
        while (buf.remaining() >= Constants.INTEGER_SIZE) {
            int code = buf.getInt();
            if (log.isDebugEnabled()) {
                log.info("Option code = {}", code);
            }
 
            Option option = V4OptionFactory.getOption(code);
            if (option != null) {
                option.decode(buf);
                options.put(option.getCode(), option);
            } else {
                break;  // no more options, or one is malformed, so we're done
            }
        }
        
        return options;
    }

    /**
     * Return the length of this Message in bytes.
     *
     * @return an int containing a length of a least four(4)
     */
    public int getLength() {
        // the "magic cookie" and at least the message type option and the end option: = 4 + 3 + 1
        return 8 + getOptionsLength();
    }

    /**
     * Return the length of the options in this Message in bytes.
     *
     * @return an int containing the total length of all options
     */
    protected int getOptionsLength() {
        int len = 0;
        if (options != null) {
            for (Option option : options.values()) {
                len += 8;   // option code (4 byte) + length (4 byte) 
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
        StringBuilder sb = new StringBuilder();
        
        sb.append(Constants.getV4MsgSenderString(getMessageSender()));
        sb.append(StringUtils.SPACE);
        sb.append(Constants.getV4MsgTypeString(getMessageType()));
        if (getOp() == Constants.V4_OP_REPLY || getOp() == Constants.V4_OP_REQUEST) {
            sb.append(" to ");
        } else {
            sb.append(" from ");
        }
        sb.append(Utils.socketAddressAsString(remoteAddress));

        sb.append(" (htype = ");
        sb.append(getHtype());
        sb.append(", xid = ");
        sb.append(getTransactionId());
        sb.append(", clientAddr = ");
        sb.append(getClientAddr().getHostAddress());
        sb.append(", clientPort = ");
        sb.append(getClientPort());
        sb.append(", serverAddr = ");
        sb.append(getServerAddr().getHostAddress());
        sb.append(", serverPort = ");
        sb.append(getServerPort());
        sb.append(')');
        
        return sb.toString();
    }

    public String toString(int localDst) {
        StringBuilder sb = new StringBuilder();
        
        sb.append(Constants.getV4MsgSenderString(getMessageSender()));
        sb.append(StringUtils.SPACE);
        sb.append(Constants.getV4MsgTypeString(getMessageType()));
        
        if (localDst == Constants.V4_MESSAGE_SENDER_CLIENT && getMessageSender() == Constants.V4_MESSAGE_SENDER_SERVER ||
            localDst == Constants.V4_MESSAGE_SENDER_SERVER && getMessageSender() == Constants.V4_MESSAGE_SENDER_CLIENT) {
            sb.append(" from ");
        } else {
            sb.append(" to ");
        }
        
        sb.append(Utils.socketAddressAsString(remoteAddress));

        sb.append(" (htype = ");
        sb.append(getHtype());
        sb.append(", xid = ");
        sb.append(getTransactionId());
        sb.append(", clientAddr = ");
        sb.append(getClientAddr().getHostAddress());
        sb.append(", clientPort = ");
        sb.append(getClientPort());
        sb.append(", serverAddr = ");
        sb.append(getServerAddr().getHostAddress());
        sb.append(", serverPort = ");
        sb.append(getServerPort());
        sb.append(')');
        
        return sb.toString();
    }
    
    public String toStringWithOptions(int localDst) {
        StringBuilder sb = new StringBuilder(toString(localDst));
        if ((options != null) && !options.isEmpty()) {
            sb.append(Utils.LINE_SEPARATOR);
            sb.append("options");
            for (Option option : options.values()) {
                sb.append(option.toString());
            }
        }
        
        return sb.toString();
    }

    public short getOp() {
        return op;
    }

    public void setOp(short op) {
        this.op = op;
    }

    public short getHtype() {
        return htype;
    }

    public void setHtype(short htype) {
        this.htype = htype;
    }

    public long getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(long transactionId) {
        this.transactionId = transactionId;
    }

    public InetAddress getClientAddr() {
        return clientAddr;
    }

    public void setClientAddr(InetAddress clientAddr) {
        this.clientAddr = clientAddr;
    }

    public long getClientPort() {
        return clientPort;
    }
    
    public void setClientPort(long clientPort) {
        this.clientPort = clientPort;
    }
   
    public InetAddress getServerAddr() {
        return serverAddr;
    }

    public void setServerAddr(InetAddress serverAddr) {
        this.serverAddr = serverAddr;
    }

    public long getServerPort() {
        return serverPort;
    }
    
    public void setServerPort(long serverPort) {
        this.serverPort = serverPort;
    }

    @Override
    public short getMessageType() {
        MsgTypeOption msgType = (MsgTypeOption) options.get(Constants.OPTION_MESSAGE_TYPE);
        if (msgType != null) {
            return msgType.getUnsignedByte();
        }

        return 0;
    }

    @Override
    public void setMessageType(short msgType) {
        MsgTypeOption msgTypeOption = new MsgTypeOption(true);
        msgTypeOption.setUnsignedByte(msgType);
        putOption(msgTypeOption);
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
    public void setMessageSender(short msgSender) {
        MsgSenderOption msgSenderOption = new MsgSenderOption(true);
        msgSenderOption.setUnsignedByte(msgSender);
        putOption(msgSenderOption);
    }
}
