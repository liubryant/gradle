/*
 * Copyright 2009 the original author or authors.
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
package org.gradle.listener.remote;

import org.gradle.listener.Event;
import org.gradle.listener.ListenerBroadcast;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.Channels;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RemoteReceiver implements Closeable {
    private static final Logger LOGGER = LoggerFactory.getLogger(RemoteReceiver.class);
    private final ListenerBroadcast<?> broadcaster;
    private final ServerSocketChannel serverSocket;
    private final ExceptionListener exceptionListener;
    private final ExecutorService executor;

    public RemoteReceiver(ListenerBroadcast<?> broadcaster) throws IOException {
        this(broadcaster, null);
    }

    public RemoteReceiver(ListenerBroadcast<?> broadcaster, ExceptionListener exceptionListener) throws IOException {
        if (broadcaster == null) {
            throw new NullPointerException();
        }

        this.broadcaster = broadcaster;
        this.exceptionListener = exceptionListener;
        serverSocket = ServerSocketChannel.open();
        serverSocket.socket().bind(new InetSocketAddress(0));
        executor = Executors.newCachedThreadPool();
        executor.submit(new Receiver());
    }

    public int getBoundPort() {
        return serverSocket.socket().getLocalPort();
    }

    public void close() throws IOException {
        serverSocket.close();
        executor.shutdownNow();
    }

    private class Handler implements Runnable {
        private final SocketChannel socket;

        public Handler(SocketChannel socket) {
            this.socket = socket;
        }

        public void run() {
            try {
                InputStream inputStream = new BufferedInputStream(Channels.newInputStream(socket));
                while (true) {
                    Event message = Event.receive(inputStream);

                    try {
                        broadcaster.dispatch(message);
                    } catch (Exception e) {
                        if (exceptionListener != null) {
                            exceptionListener.receiverThrewException(e);
                        }
                    }
                }
            } catch (Exception e) {
                if (e instanceof IOException && e.getMessage().startsWith(
                        "An existing connection was forcibly closed by the remote host")) {
                    // Ignore
                    return;
                }
                LOGGER.warn("Could not handle remote event connection.", e);
            }
        }
    }

    private class Receiver implements Runnable {
        public void run() {
            try {
                while (true) {
                    SocketChannel socket = serverSocket.accept();
                    executor.submit(new Handler(socket));
                }
            } catch (AsynchronousCloseException e) {
                // Ignore
            } catch (IOException e) {
                LOGGER.warn("Could not accept remote event connection.", e);
            }
        }
    }

    public static interface ExceptionListener {
        public void receiverThrewException(Throwable throwable);
    }
}

