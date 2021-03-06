//
// MessagePack-RPC for Java
//
// Copyright (C) 2010 Kazuki Ohta
//
//    Licensed under the Apache License, Version 2.0 (the "License");
//    you may not use this file except in compliance with the License.
//    You may obtain a copy of the License at
//
//        http://www.apache.org/licenses/LICENSE-2.0
//
//    Unless required by applicable law or agreed to in writing, software
//    distributed under the License is distributed on an "AS IS" BASIS,
//    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//    See the License for the specific language governing permissions and
//    limitations under the License.
//
package org.msgpack.rpc.client.transport;

import java.util.ArrayList;
import java.util.List;

import org.msgpack.rpc.client.EventLoop;
import org.msgpack.rpc.client.Session;

/**
 * TCPTransport sends/receives the data through TCP, by using underlying
 * TCPSocket layer.
 *
 * This class also hides the latency of establishing the connection. If the
 * connection is not established. the sending messages are temporarily queued.
 * Then, they are actually sent to the network when it's connected.
 */
public class TCPTransport extends Transport {
    protected final TCPSocket socket;
    protected Boolean isConnecting;
    protected Boolean isConnected;
    protected List<Object> pendingMessages;

    public TCPTransport(Session session, EventLoop loop) {
        super(session, loop);
        this.isConnecting = false;
        this.isConnected = false;
        this.socket = new TCPSocket(session.getAddress(), loop, this);
        this.pendingMessages = new ArrayList<Object>();
    }

    /**
     * Send the message to the remote server.
     *
     * This method tries to hide the connection latency.
     * If it's already connected, then the message is sent immediately.
     * Otherwise, the message is temporarily buffered in the
     * this.pendingMessages. Once it's connected, the buffered messages are
     * sent to the server.
     * 
     * @param msg the message to send.
     * @throws Exception
     */
    @Override
    public void sendMessage(Object msg) throws Exception {
        boolean isTryConnect = false;
        boolean isTrySend = false;
        synchronized(this) {
            if (isConnected) {
                isTrySend = true;
            } else {
                if (!isConnecting) {
                    isTryConnect = true;
                    isConnecting = true;
                }
                pendingMessages.add(msg);
            }
        }
        if (isTryConnect)
            socket.tryConnect();
        if (isTrySend)
            socket.trySend(msg);
    }

    /**
     * Close the connection associated with this transport.
     */
    @Override
    public void tryClose() {
        synchronized(this) {
            isConnecting = false;
            isConnected = false;
            pendingMessages.clear();
        }
        socket.tryClose();
    }
    
    /**
     * The callback function, called when the connection is established.
     * @throws Exception
     */
    public void onConnected() throws Exception {
        // send the pending messages
        Object[] msgs;
        synchronized(this) {
            isConnecting = false;
            isConnected = true;
            msgs = pendingMessages.toArray();
            pendingMessages.clear();
        }
        for (Object msg : msgs)
            socket.trySend(msg);
    }
    
    /**
     * The callback called when the message arrives
     * @param replyObject the received object, already unpacked.
     * @throws Exception
     */
    public void onMessageReceived(Object replyObjects) throws Exception {
        session.onMessageReceived(replyObjects);
    }

    /**
     * The callback function, called when the connection failed.
     */
    public void onConnectFailed() {
        tryClose();
        session.onConnectFailed();
    }
    
    /**
     * The callback called when the connection closed.
     */
    public void onClosed() {
        tryClose();
        session.onClosed();
    }
    
    /**
     * The callback called when the error occurred.
     * @param e occurred exception.
     */
    public void onFailed(Exception e) {
        tryClose();
        session.onFailed(e);
    }
}
