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

import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.Optional;

class FilePublisherConfiguration {

    private final StructuredConfiguration configurationProperties;

    FilePublisherConfiguration(EventPublisherConfiguration eventPublisherConfiguration) {
        configurationProperties = eventPublisherConfiguration.getProperties()
                .orElseGet(StructuredConfiguration.Empty::new);
    }

    String getPath() {
        return configurationProperties.get("path")
                .map(StructuredConfiguration::asString)
                .orElse(defaultFilePath());
    }

    boolean isPrettifyJSON() {
        return configurationProperties.get("prettifyJSON")
                .map(StructuredConfiguration::asBoolean)
                .orElse(false);
    }

    private String defaultFilePath() {
        Path path = FileSystems.getDefault()
                .getPath(System.getProperty("user.home"), "james-publisher-file-output.json");
        return path.normalize().toString();
    }

    public String getEventType() {
        return configurationProperties.get("eventType")
                            .map(StructuredConfiguration::asString)
                            .orElse("james");
    }

    public Optional<String> getEnvironment() {
        return configurationProperties.get("environment")
                            .map(StructuredConfiguration::asString);
    }
}
