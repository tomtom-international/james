/*
 * Copyright 2017 TomTom International B.V.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tomtom.james.controller.webservice;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

class WebServer {

    private static final int SERVER_SOCKET_BACKLOG = 0; // system default

    private final InetSocketAddress address;
    private final Executor executor;
    private final Map<String, HttpHandler> handlers;

    WebServer(InetSocketAddress address, Map<String, HttpHandler> handlers, Executor executor) {
        this.address = address;
        this.handlers = handlers;
        this.executor = executor;
    }

    void start() throws IOException {
        HttpServer server = HttpServer.create(address, SERVER_SOCKET_BACKLOG);
        handlers.forEach(server::createContext);
        server.setExecutor(executor);
        DaemonWrapperThread wrapper = new DaemonWrapperThread(server);
        wrapper.start();
    }

    /**
     * HttpServer's dispatcher thread is non-daemon and is blocking termination of JVM on the end of main().
     * This hack makes a daemon thread the parent of HttpServer dispatcher thread, so that daemon flag is inherited
     * by the dispatcher and all connection handling threads.
     */
    private static class DaemonWrapperThread extends Thread {
        private final HttpServer server;

        DaemonWrapperThread(HttpServer server) {
            this.server = server;
            setDaemon(true);
            setName("james-controller-webservice-daemon-thread");
        }

        @Override
        public void run() {
            server.start();
        }
    }

    private static class ServerThreadFactory implements ThreadFactory {
        private final AtomicInteger threadNumber = new AtomicInteger(0);

        @Override
        public Thread newThread(Runnable runnable) {
            String threadName = "james-controller-webservice-server-pool-" + threadNumber.incrementAndGet();
            return new Thread(runnable, threadName);
        }
    }
}
