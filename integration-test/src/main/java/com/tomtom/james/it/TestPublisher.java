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

package com.tomtom.james.it;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tomtom.james.common.api.configuration.EventPublisherConfiguration;
import com.tomtom.james.common.api.publisher.Event;
import com.tomtom.james.common.api.publisher.EventPublisher;
import com.tomtom.james.common.log.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Objects;

public class TestPublisher implements EventPublisher {

    private static final Logger LOG = Logger.getLogger(TestPublisher.class);

    private final Path tmpDir = Paths.get(System.getProperty("java.io.tmpdir"));
    private final Path tmpFile = tmpDir.resolve("James-TestPublisher-out.txt");
    private final String pluginID = "integrationTest.publisher";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String getId() {
        return pluginID;
    }

    @Override
    public void initialize(EventPublisherConfiguration configuration) {
        LOG.trace("Test publisher initialized, writing to " + tmpFile);
    }

    @Override
    public void publish(Event evt) {
        try {
            String evtJson = objectMapper.writeValueAsString(evt.getContent());
            LOG.trace(() -> "Publishing event: " + evtJson);
            Files.write(tmpFile, (evtJson + "\n").getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            LOG.warn("Error publishing an event", e);
        }
    }

    @Override
    public void close() {
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TestPublisher that = (TestPublisher) o;
        return Objects.equals(pluginID, that.pluginID);
    }

    @Override
    public int hashCode() {
        return Objects.hash(pluginID);
    }
}
