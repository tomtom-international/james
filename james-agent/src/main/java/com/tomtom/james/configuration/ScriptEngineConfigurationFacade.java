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

class ScriptEngineConfigurationFacade implements ScriptEngineConfiguration {

    private final StructuredConfiguration configuration;

    ScriptEngineConfigurationFacade(StructuredConfiguration configuration) {
        this.configuration = configuration;
    }

    @Override
    public int getAsyncWorkers() {
        return configuration.get("asyncWorkers")
                .map(StructuredConfiguration::asInteger)
                .orElse(Runtime.getRuntime().availableProcessors());
    }

    @Override
    public int getMaxAsyncJobQueueCapacity() {
        return configuration.get("maxAsyncJobQueueCapacity")
                .map(StructuredConfiguration::asInteger)
                .orElse(10_000);
    }

}