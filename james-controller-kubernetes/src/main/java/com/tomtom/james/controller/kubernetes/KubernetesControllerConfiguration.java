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

package com.tomtom.james.controller.kubernetes;

import com.tomtom.james.common.api.configuration.ConfigurationStructureException;
import com.tomtom.james.common.api.configuration.JamesControllerConfiguration;
import com.tomtom.james.common.api.configuration.StructuredConfiguration;
import io.kubernetes.client.openapi.Pair;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

class KubernetesControllerConfiguration {

    private final StructuredConfiguration configuration;

    KubernetesControllerConfiguration(JamesControllerConfiguration jamesControllerConfiguration) {
        configuration = jamesControllerConfiguration.getProperties()
                                                    .orElseGet(StructuredConfiguration.Empty::new);
    }

    String getNamespace() {
        return configuration
            .get("namespace")
            .map(StructuredConfiguration::asString)
            .orElseThrow(() -> new ConfigurationStructureException("Missing namespace property"));
    }

    Map<String, String> getLabels() {
        return configuration
            .get("labels")
            .map(StructuredConfiguration::asMap)
            .orElseGet(Collections::emptyMap)
            .entrySet()
            .stream()
            .map(v -> new Pair(v.getKey(), v.getValue().asString()))
            .collect(Collectors.toMap(Pair::getName, Pair::getValue));
    }

    String getUrl() {
        return configuration
            .get("url")
            .map(StructuredConfiguration::asString)
            .orElse("");
    }

    String getToken() {
        return configuration
            .get("token")
            .map(StructuredConfiguration::asString)
            .orElse("");
    }
}
