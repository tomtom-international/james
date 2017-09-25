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

import com.tomtom.james.common.api.Identifiable;
import com.tomtom.james.common.api.configuration.ToolkitConfiguration;
import com.tomtom.james.common.api.toolkit.Toolkit;
import com.tomtom.james.common.log.Logger;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class ToolkitManager {

    private static final Logger LOG = Logger.getLogger(ToolkitManager.class);
    private final Map<String, Toolkit> toolkits;

    ToolkitManager(PluginManager pluginManager, Collection<ToolkitConfiguration> toolkitsConfigurations) {
        toolkits = toolkitsConfigurations.stream()
                .map(configuration -> createAndInitializeToolkit(pluginManager, configuration))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toMap(Identifiable::getId, instance -> instance));
    }

    public Optional<Toolkit> getToolkit(String toolkitId) {
        return Optional.ofNullable(toolkits.get(toolkitId));
    }

    private static Optional<Toolkit> createAndInitializeToolkit(PluginManager pluginManager,
                                                                ToolkitConfiguration configuration) {
        Optional<Toolkit> toolkit = pluginManager.createToolkitPluginInstance(configuration);
        if (toolkit.isPresent()) {
            LOG.trace(() -> "Loaded toolkit plugin " + configuration.getId());
            toolkit.get().initialize(configuration);
        } else {
            LOG.warn(() -> "Error loading toolkit " + configuration.getId() + ", plugin not found");
        }
        return toolkit;
    }
}
