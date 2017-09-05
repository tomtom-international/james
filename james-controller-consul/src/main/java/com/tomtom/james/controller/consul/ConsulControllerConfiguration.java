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

package com.tomtom.james.controller.consul;

import com.tomtom.james.common.api.configuration.ConfigurationStructureException;
import com.tomtom.james.common.api.configuration.JamesControllerConfiguration;
import com.tomtom.james.common.api.configuration.StructuredConfiguration;

class ConsulControllerConfiguration {

    private final StructuredConfiguration configuration;

    ConsulControllerConfiguration(JamesControllerConfiguration jamesControllerConfiguration) {
        configuration = jamesControllerConfiguration.getProperties()
                .orElseGet(StructuredConfiguration.Empty::new);
    }

    String getHost() {
        return configuration
                .get("host")
                .map(StructuredConfiguration::asString)
                .orElse("localhost");
    }

    int getPort() {
        return configuration
                .get("port")
                .map(StructuredConfiguration::asInteger)
                .orElse(8500);
    }

    String getFolderPath() {
        return configuration
                .get("folderPath")
                .map(StructuredConfiguration::asString)
                .orElseThrow(() -> new ConfigurationStructureException("Missing folderPath property"));
    }
}
