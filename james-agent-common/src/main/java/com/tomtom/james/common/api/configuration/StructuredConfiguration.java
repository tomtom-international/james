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

package com.tomtom.james.common.api.configuration;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface StructuredConfiguration {

    Optional<StructuredConfiguration> get(String path);
    Optional<StructuredConfiguration> get(String... pathSegments);

    List<StructuredConfiguration> asList();
    Map<String, StructuredConfiguration> asMap();

    String asString();
    boolean asBoolean();
    int asInteger();
    long asLong();

    class Empty implements StructuredConfiguration {
        @Override
        public Optional<StructuredConfiguration> get(String path) {
            return Optional.empty();
        }

        @Override
        public Optional<StructuredConfiguration> get(String... pathSegments) {
            return Optional.empty();
        }

        @Override
        public String asString() {
            throw new IllegalStateException();
        }

        @Override
        public boolean asBoolean() {
            throw new IllegalStateException();
        }

        @Override
        public int asInteger() {
            throw new IllegalStateException();
        }

        @Override
        public long asLong() {
            throw new IllegalStateException();
        }

        @Override
        public List<StructuredConfiguration> asList() {
            throw new IllegalStateException();
        }

        @Override
        public Map<String, StructuredConfiguration> asMap() {
            throw new IllegalStateException();
        }
    }
}
