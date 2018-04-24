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

package com.tomtom.james.agent;

import com.tomtom.james.common.api.ClassScanner;
import com.tomtom.james.common.api.Closeable;
import com.tomtom.james.common.api.QueueBacked;
import com.tomtom.james.common.api.configuration.JamesControllerConfiguration;
import com.tomtom.james.common.api.controller.JamesController;
import com.tomtom.james.common.api.informationpoint.InformationPointService;
import com.tomtom.james.common.api.publisher.EventPublisher;
import com.tomtom.james.common.api.script.ScriptEngine;
import com.tomtom.james.common.log.Logger;

import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;

public class ControllersManager implements Closeable {

    private static final Logger LOG = Logger.getLogger(ControllersManager.class);

    private final PluginManager pluginManager;
    private final Collection<JamesControllerConfiguration> controllerConfigurations;
    private Collection<JamesController> initializedControllers;

    public ControllersManager(PluginManager pluginManager,
                              Collection<JamesControllerConfiguration> controllerConfigurations) {
        this.pluginManager = pluginManager;
        this.controllerConfigurations = controllerConfigurations;
    }

    public void initializeControllers(InformationPointService informationPointService,
                                      ClassScanner classScanner,
                                      ScriptEngine scriptEngine,
                                      EventPublisher eventPublisher,
                                      QueueBacked jamesObjectiveQueue,
                                      QueueBacked newClassesQueue,
                                      QueueBacked newInformationPointQueue,
                                      QueueBacked removeInformationPointQueue) {
        initializedControllers = controllerConfigurations
                .stream()
                .map(configuration -> createAndInitializeController(
                        pluginManager,
                        configuration,
                        informationPointService,
                        classScanner,
                        scriptEngine,
                        eventPublisher,
                        jamesObjectiveQueue,
                        newClassesQueue,
                        newInformationPointQueue,
                        removeInformationPointQueue))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }

    public void close() {
        if (initializedControllers != null) {
            initializedControllers.parallelStream().forEach(Closeable::close);
        }
    }

    private static Optional<JamesController> createAndInitializeController(
            PluginManager pluginManager,
            JamesControllerConfiguration configuration,
            InformationPointService informationPointService,
            ClassScanner classScanner,
            ScriptEngine scriptEngine,
            EventPublisher eventPublisher,
            QueueBacked jamesObjectiveQueue,
            QueueBacked newClassesQueue,
            QueueBacked newInformationPointQueue,
            QueueBacked removeInformationPointQueue) {

        Optional<JamesController> ep = pluginManager.createControllerPluginInstance(configuration);
        if (ep.isPresent()) {
            LOG.trace(() -> "Loaded controller plugin " + configuration.getId());
            ep.get().initialize(configuration,
                    informationPointService,
                    classScanner,
                    scriptEngine,
                    eventPublisher,
                    jamesObjectiveQueue,
                    newClassesQueue,
                    newInformationPointQueue,
                    removeInformationPointQueue);
            LOG.trace("----------------------- DONE ----------------------");
        } else {
            LOG.warn(() -> "Error loading controller " + configuration.getId() + ", plugin not found");
        }
        return ep;
    }
}
