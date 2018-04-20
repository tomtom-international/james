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
import com.tomtom.james.common.api.ClassScanner;
import com.tomtom.james.common.api.QueueBacked;
import com.tomtom.james.common.api.configuration.JamesControllerConfiguration;
import com.tomtom.james.common.api.controller.JamesController;
import com.tomtom.james.common.api.informationpoint.InformationPointService;
import com.tomtom.james.common.api.publisher.EventPublisher;
import com.tomtom.james.common.api.script.ScriptEngine;
import com.tomtom.james.common.log.Logger;
import com.tomtom.james.controller.webservice.handlers.v1.ClassScannerHandler;
import com.tomtom.james.controller.webservice.handlers.v1.InformationPointHandler;
import com.tomtom.james.controller.webservice.handlers.v1.QueueHandler;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("unused")
public class WebserviceController implements JamesController {

    private static final Logger LOG = Logger.getLogger(WebserviceController.class);

    @Override
    public String getId() {
        return "james.controller.webservice";
    }

    @Override
    public void initialize(JamesControllerConfiguration jamesControllerConfiguration,
                           InformationPointService informationPointService,
                           ClassScanner classScanner,
                           ScriptEngine scriptEngine,
                           EventPublisher eventPublisher,
                           QueueBacked jamesObjectiveQueue,
                           QueueBacked newClassesQueue,
                           QueueBacked newInformationPointQueue,
                           QueueBacked removeInformationPointQueue) {

        LOG.trace(" initialization ");
        WebserviceControllerConfiguration configuration = new WebserviceControllerConfiguration(jamesControllerConfiguration);
        InetSocketAddress listeningAddress = new InetSocketAddress(configuration.getPort());
        Map<String, HttpHandler> handlers = createHandlers(informationPointService,
                classScanner,
                scriptEngine,
                eventPublisher,
                jamesObjectiveQueue,
                newClassesQueue,
                newInformationPointQueue,
                removeInformationPointQueue);
        Executor executor = createExecutor(configuration);

        WebServer webServer = new WebServer(listeningAddress, handlers, executor);
        try {
            webServer.start();
            LOG.trace(() -> "HTTP server started, listening at port " + configuration.getPort());
        } catch (IOException e) {
            LOG.error(() -> "Error starting HTTP server: " + e.getMessage());
        }
    }

    @Override
    public void close() {
        // do nothing
    }

    private Map<String, HttpHandler> createHandlers(InformationPointService informationPointService,
                                                    ClassScanner classScanner,
                                                    ScriptEngine scriptEngine,
                                                    EventPublisher eventPublisher,
                                                    QueueBacked jamesObjectiveQueue,
                                                    QueueBacked newClassesQueue,
                                                    QueueBacked newInformationPointQueue,
                                                    QueueBacked removeInformationPointQueue) {
        HashMap<String, HttpHandler> handlers = new HashMap<>();
        handlers.put("/v1/information-point", new InformationPointHandler(informationPointService));
        handlers.put("/v1/queue", new QueueHandler(scriptEngine, eventPublisher, jamesObjectiveQueue, newClassesQueue, newInformationPointQueue, removeInformationPointQueue));
        handlers.put("/v1/class-scanner/", new ClassScannerHandler(classScanner));
        return handlers;
    }

    private Executor createExecutor(WebserviceControllerConfiguration configuration) {
        return new ThreadPoolExecutor(configuration.getMinThreads(), configuration.getMaxThreads(),
                60L, TimeUnit.SECONDS,
                new SynchronousQueue<>());
    }

}
