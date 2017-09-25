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

import com.tomtom.james.common.api.configuration.StructuredConfiguration;

import java.nio.file.FileSystems;
import java.nio.file.Path;

class InformationPointStoreConfigurationFacade implements InformationPointStoreConfiguration {

    private final StructuredConfiguration configuration;

    InformationPointStoreConfigurationFacade(StructuredConfiguration configuration) {
        this.configuration = configuration;
    }

    @Override
    public boolean isPersistenceEnabled() {
        return configuration.get("persistenceEnabled")
                .map(StructuredConfiguration::asBoolean)
                .orElse(false);
    }

    @Override
    public String getStoreFilePath() {
        return configuration.get("storeFilePath")
                .map(StructuredConfiguration::asString)
                .orElse(defaultStoreFilePath());
    }

    private String defaultStoreFilePath() {
        Path path = FileSystems.getDefault().getPath(
                System.getProperty("user.home"),
                "james-information-points-store.json");
        return path.normalize().toString();
    }
}
