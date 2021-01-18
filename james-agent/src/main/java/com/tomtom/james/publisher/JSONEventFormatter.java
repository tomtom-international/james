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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.tomtom.james.common.api.publisher.Event;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Optional;

class JSONEventFormatter {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final boolean pretty;
    private String type;
    private Optional<String> environment;

    JSONEventFormatter(boolean pretty, String type, Optional<String> environment) {
        this.pretty = pretty;
        this.type = type;
        this.environment = environment;
        objectMapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
    }

    String format(Event evt) {
        LinkedHashMap<String, Object> result = new LinkedHashMap<>();
        result.put("@timestamp", evt.getTimestamp().toString());
        result.put("type", type);
        environment.ifPresent(env -> result.put("environment", env));
        result.putAll(evt.getContent());

        try {
            return pretty
                    ? objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(result)
                    : objectMapper.writeValueAsString(result);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
