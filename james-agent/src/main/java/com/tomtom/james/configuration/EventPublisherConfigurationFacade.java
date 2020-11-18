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

import com.tomtom.james.common.api.configuration.ConfigurationStructureException;
import com.tomtom.james.common.api.configuration.EventPublisherConfiguration;
import com.tomtom.james.common.api.configuration.StructuredConfiguration;

import java.util.Optional;

class EventPublisherConfigurationFacade implements EventPublisherConfiguration {

    private final StructuredConfiguration structuredConfiguration;

    EventPublisherConfigurationFacade(StructuredConfiguration structuredConfiguration) {
        this.structuredConfiguration = structuredConfiguration;
    }

    @Override
    public String getId() {
        return structuredConfiguration.get("id")
                .map(StructuredConfiguration::asString)
                .orElseThrow(() -> new ConfigurationStructureException("Missing id for " + structuredConfiguration));
    }

    @Override
    public int getAsyncWorkers() {
        return structuredConfiguration.get("asyncWorkers")
                .map(StructuredConfiguration::asInteger)
                .orElse(1);
    }

    @Override
    public int getMaxAsyncJobQueueCapacity() {
        return structuredConfiguration.get("maxAsyncJobQueueCapacity")
                .map(StructuredConfiguration::asInteger)
                .orElse(16_384);
    }

    @Override
    public Optional<StructuredConfiguration> getProperties() {
        return structuredConfiguration.get("properties");
    }
}
