/*
 * Copyright 2012-2013 NgSql Group.
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  
 *      http://www.apache.org/licenses/LICENSE-2.0
 *  
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ng12306.sql.net.connection;

import java.io.IOException;
import java.net.Socket;
import java.nio.channels.SocketChannel;

import org.ng12306.sql.net.buffer.BufferQueue;

/**
 * 
* [前端链接工厂]
* @author: lvbo
* @date: 2013-4-23 下午9:42:39
* @version: 1.0
 */
public abstract class FrontendConnectionFactory {

	protected int socketRecvBuffer = 8 * 1024;
    protected int socketSendBuffer = 16 * 1024;
    protected int packetHeaderSize = 4;
    protected int maxPacketSize = 16 * 1024 * 1024;
    protected int writeQueueCapcity = 16;
    protected long idleTimeout = 8 * 3600 * 1000L;
    protected String charset = "utf8";

    protected abstract FrontendConnection getConnection(SocketChannel channel);

    public FrontendConnection make(SocketChannel channel) throws IOException {
        Socket socket = channel.socket();
        socket.setReceiveBufferSize(socketRecvBuffer);
        socket.setSendBufferSize(socketSendBuffer);
        socket.setTcpNoDelay(true);
        socket.setKeepAlive(true);
        FrontendConnection c = getConnection(channel);
        c.setPacketHeaderSize(packetHeaderSize);
        c.setMaxPacketSize(maxPacketSize);
        c.setWriteQueue(new BufferQueue(writeQueueCapcity));
        c.setIdleTimeout(idleTimeout);
        c.setCharset(charset);
        return c;
    }

    public int getSocketRecvBuffer() {
        return socketRecvBuffer;
    }

    public void setSocketRecvBuffer(int socketRecvBuffer) {
        this.socketRecvBuffer = socketRecvBuffer;
    }

    public int getSocketSendBuffer() {
        return socketSendBuffer;
    }

    public void setSocketSendBuffer(int socketSendBuffer) {
        this.socketSendBuffer = socketSendBuffer;
    }

    public int getPacketHeaderSize() {
        return packetHeaderSize;
    }

    public void setPacketHeaderSize(int packetHeaderSize) {
        this.packetHeaderSize = packetHeaderSize;
    }

    public int getMaxPacketSize() {
        return maxPacketSize;
    }

    public void setMaxPacketSize(int maxPacketSize) {
        this.maxPacketSize = maxPacketSize;
    }

    public int getWriteQueueCapcity() {
        return writeQueueCapcity;
    }

    public void setWriteQueueCapcity(int writeQueueCapcity) {
        this.writeQueueCapcity = writeQueueCapcity;
    }

    public long getIdleTimeout() {
        return idleTimeout;
    }

    public void setIdleTimeout(long idleTimeout) {
        this.idleTimeout = idleTimeout;
    }

    public String getCharset() {
        return charset;
    }

    public void setCharset(String charset) {
        this.charset = charset;
    }
}