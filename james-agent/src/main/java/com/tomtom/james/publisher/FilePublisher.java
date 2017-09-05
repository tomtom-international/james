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
import com.tomtom.james.common.api.publisher.Event;
import com.tomtom.james.common.api.publisher.EventPublisher;
import com.tomtom.james.common.log.Logger;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Objects;

public class FilePublisher implements EventPublisher {

    private static final Logger LOG = Logger.getLogger(FilePublisher.class);

    private static final String PLUGIN_ID = "james.publisher.file";

    private Writer writer;
    private JSONEventFormatter formatter;

    @Override
    public String getId() {
        return PLUGIN_ID;
    }

    @Override
    public void initialize(EventPublisherConfiguration eventPublisherConfiguration) {
        FilePublisherConfiguration configuration = new FilePublisherConfiguration(eventPublisherConfiguration);
        writer = createWriter(configuration);
        formatter = new JSONEventFormatter(configuration.isPrettifyJSON());
    }

    @Override
    public void publish(Event evt) {
        if (writer != null) {
            try {
                writer.write(formatter.format(Objects.requireNonNull(evt)));
                writer.write(System.lineSeparator());
                writer.flush();
            } catch (IOException e) {
                LOG.error("Write failed", e);
                writer = null;
            }
        }
    }

    @Override
    public void close() {
        if (writer != null) {
            try {
                writer.close();
            } catch (IOException e) {
                LOG.warn("File close failed", e);
            } finally {
                writer = null;
            }
        }
    }

    private Writer createWriter(FilePublisherConfiguration configuration) {
        try {
            String filePath = configuration.getPath();
            BufferedWriter writer = Files.newBufferedWriter(Paths.get(filePath), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            LOG.trace(() -> "Using file " + filePath + " for event publishing");
            return writer;
        } catch (IOException e) {
            LOG.error("Failed to create file: ", e);
            return null;
        }
    }
}
