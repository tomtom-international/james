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

package com.tomtom.james.publisher;

import com.tomtom.james.common.api.configuration.EventPublisherConfiguration;
import com.tomtom.james.common.api.configuration.StructuredConfiguration;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

class StreamPublisherConfiguration {

    private final StructuredConfiguration configurationProperties;

    StreamPublisherConfiguration(EventPublisherConfiguration eventPublisherConfiguration) {
        configurationProperties = eventPublisherConfiguration.getProperties()
                .orElseGet(StructuredConfiguration.Empty::new);
    }

    protected StructuredConfiguration getConfigurationProperties() {
        return configurationProperties;
    }

    boolean isPrettifyJSON() {
        return configurationProperties.get("prettifyJSON")
                .map(StructuredConfiguration::asBoolean)
                .orElse(false);
    }

    public Optional<String> getEventType() {
        return configurationProperties.get("eventType")
                            .map(StructuredConfiguration::asString);
    }

    public Optional<String> getEnvironment() {
        return configurationProperties.get("environment")
                            .map(StructuredConfiguration::asString);
    }

    public Map<String, String> getFields() {
        return configurationProperties
            .get("fields")
            .map(StructuredConfiguration::asMap)
            .orElseGet(Collections::emptyMap)
            .entrySet()
            .stream()
            .collect(Collectors.toMap(Map.Entry::getKey, v -> v.getValue().asString()));
    }
}
