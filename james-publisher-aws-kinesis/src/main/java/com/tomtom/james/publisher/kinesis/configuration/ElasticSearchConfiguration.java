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

package com.tomtom.james.publisher.kinesis.configuration;

import com.tomtom.james.common.api.configuration.StructuredConfiguration;

import java.util.Optional;

public class ElasticSearchConfiguration {

    private final StructuredConfiguration configuration;

    ElasticSearchConfiguration(StructuredConfiguration configuration) {
        this.configuration = configuration;
    }

    public String getEventType() {
        return configuration.get("eventType")
                .map(StructuredConfiguration::asString)
                .orElse("james");
    }

    public Optional<String> getEnvironment() {
        return configuration.get("environment")
                .map(StructuredConfiguration::asString);
    }

}
