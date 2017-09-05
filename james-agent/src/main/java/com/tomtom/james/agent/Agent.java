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

import com.google.common.io.Resources;
import com.tomtom.james.common.api.informationpoint.InformationPointService;
import com.tomtom.james.common.api.publisher.EventPublisher;
import com.tomtom.james.common.api.script.ScriptEngine;
import com.tomtom.james.common.log.Logger;
import com.tomtom.james.configuration.AgentConfiguration;
import com.tomtom.james.configuration.AgentConfigurationFactory;
import com.tomtom.james.configuration.ConfigurationInitializationException;
import com.tomtom.james.informationpoint.InformationPointServiceImpl;
import com.tomtom.james.publisher.EventPublisherFactory;
import com.tomtom.james.script.ScriptEngineFactory;
import com.tomtom.james.store.InformationPointStore;
import com.tomtom.james.store.InformationPointStoreFactory;

import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.net.URL;
import java.nio.charset.StandardCharsets;

@SuppressWarnings("unused")
class Agent {

    private static final Logger LOG = Logger.getLogger(Agent.class);

    public static void premain(String agentArgs, Instrumentation instrumentation) {
        setupAgent(instrumentation);
    }

    public static void agentmain(String agentArgs, Instrumentation instrumentation) {
        setupAgent(instrumentation);
    }

    private static void setupAgent(Instrumentation instrumentation) {
        try {
            AgentConfiguration configuration = AgentConfigurationFactory.create();
            Logger.setCurrentLogLevel(configuration.getLogLevel());
            printBanner(configuration);

            PluginManager pluginManager = new PluginManager(configuration.getPluginIncludeDirectories(), configuration.getPluginIncludeFiles());
            EventPublisher publisher = EventPublisherFactory.create(pluginManager, configuration.getPublishersConfigurations());
            InformationPointStore store = InformationPointStoreFactory.create(configuration.getInformationPointStoreConfiguration());
            ToolkitManager toolkitManager = new ToolkitManager(pluginManager, configuration.getToolkitsConfigurations());
            ScriptEngine engine = ScriptEngineFactory.create(publisher, configuration, toolkitManager);
            ControllersManager controllersManager = new ControllersManager(pluginManager, configuration.getControllersConfigurations());
            InformationPointService informationPointService = new InformationPointServiceImpl(store, instrumentation);
            controllersManager.initializeControllers(informationPointService, engine, publisher);

            ShutdownHook shutdownHook = new ShutdownHook(controllersManager, engine, publisher, configuration);
            Runtime.getRuntime().addShutdownHook(shutdownHook);
            LOG.info("Agent initialization complete.");
        } catch (ConfigurationInitializationException e) {
            LOG.fatal("Failed to initialize agent: " + e.getMessage());
        } catch (Throwable t) {
            LOG.fatal("Unexpected exception during initialization", t);
        }
    }

    private static void printBanner(AgentConfiguration agentConfiguration) {
        if (!agentConfiguration.isQuiet()) {
            URL bannerURL = Resources.getResource(Agent.class, "banner.txt");
            try {
                Resources.readLines(bannerURL, StandardCharsets.UTF_8).forEach(System.err::println);
            } catch (IOException e) {
                LOG.warn("Error reading banner resource, looks like something is wrong with the agent jar", e);
            }
        }
    }
}
