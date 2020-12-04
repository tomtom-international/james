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

package com.tomtom.james.configuration;

import com.tomtom.james.common.api.configuration.EventPublisherConfiguration;
import com.tomtom.james.common.api.configuration.JamesControllerConfiguration;
import com.tomtom.james.common.api.configuration.StructuredConfiguration;
import com.tomtom.james.common.api.configuration.ToolkitConfiguration;
import com.tomtom.james.common.log.Logger;

import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;

class AgentConfigurationFacade implements AgentConfiguration {

    private final StructuredConfiguration configuration;

    AgentConfigurationFacade(StructuredConfiguration configuration) {
        this.configuration = configuration;
    }

    @Override
    public Collection<JamesControllerConfiguration> getControllersConfigurations() {
        return configuration.get("controllers")
                .map(StructuredConfiguration::asList)
                .orElseGet(Collections::emptyList)
                .stream()
                .map(JamesControllerConfigurationFacade::new)
                .collect(Collectors.toList());
    }

    @Override
    public Collection<EventPublisherConfiguration> getPublishersConfigurations() {
        return configuration.get("publishers")
                .map(StructuredConfiguration::asList)
                .orElseGet(Collections::emptyList)
                .stream()
                .map(EventPublisherConfigurationFacade::new)
                .collect(Collectors.toList());
    }

    @Override
    public Collection<ToolkitConfiguration> getToolkitsConfigurations() {
        return configuration.get("toolkits")
                .map(StructuredConfiguration::asMap)
                .orElseGet(Collections::emptyMap)
                .entrySet()
                .stream()
                .map(entry -> new ToolkitConfigurationFacade(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }

    @Override
    public Collection<String> getPluginIncludeDirectories() {
        return configuration.get("plugins.includeDirectories")
                .map(StructuredConfiguration::asList)
                .orElseGet(Collections::emptyList)
                .stream()
                .map(StructuredConfiguration::asString)
                .collect(Collectors.toList());
    }

    @Override
    public Collection<String> getPluginIncludeFiles() {
        return configuration.get("plugins.includeFiles")
                .map(StructuredConfiguration::asList)
                .orElseGet(Collections::emptyList)
                .stream()
                .map(StructuredConfiguration::asString)
                .collect(Collectors.toList());
    }

    @Override
    public ClassScannerConfiguration getClassScannerConfiguration() {
        return configuration.get("classScanner")
                .map(ClassScannerConfigurationFacade::new)
                .orElseGet(() -> new ClassScannerConfigurationFacade(new StructuredConfiguration.Empty()));
    }

    @Override
    public JamesHQConfiguration getJamesHQConfiguration() {
        return configuration.get("jamesHQ")
                .map(JamesHQConfigurationFacade::new)
                .orElseGet(() -> new JamesHQConfigurationFacade(new StructuredConfiguration.Empty()));
    }

    @Override
    public InformationPointStoreConfiguration getInformationPointStoreConfiguration() {
        return configuration.get("informationPointStore")
                .map(InformationPointStoreConfigurationFacade::new)
                .orElseGet(() -> new InformationPointStoreConfigurationFacade(new StructuredConfiguration.Empty()));
    }

    @Override
    public ScriptEngineConfiguration getScriptEngineConfiguration() {
        return configuration.get("scriptEngine")
                .map(ScriptEngineConfigurationFacade::new)
                .orElseGet(() -> new ScriptEngineConfigurationFacade(new StructuredConfiguration.Empty()));
    }

    @Override
    public boolean isQuiet() {
        return configuration.get("quiet")
                .map(StructuredConfiguration::asBoolean)
                .orElse(false);
    }

    @Override
    public Integer getShutdownDelay() {
        return configuration.get("shutdownDelay")
                            .map(StructuredConfiguration::asInteger)
                            .orElse(1000);
    }

    @Override
    public boolean isShutdownHookEnabled() {
        return configuration.get("shutdownHookEnabled")
                            .map(StructuredConfiguration::asBoolean)
                            .orElse(true);
    }

    @Override
    public Logger.Level getLogLevel() {
        return configuration.get("logLevel")
                .map(StructuredConfiguration::asString)
                .map(String::toUpperCase)
                .map(Logger.Level::valueOf)
                .orElse(Logger.Level.WARN);
    }

}
