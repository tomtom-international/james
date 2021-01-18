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

import com.google.common.annotations.VisibleForTesting;
import com.tomtom.james.common.api.configuration.EventPublisherConfiguration;
import com.tomtom.james.common.api.publisher.Event;
import com.tomtom.james.common.api.publisher.EventPublisher;

import java.io.PrintStream;
import java.util.Objects;

public class ConsolePublisher implements EventPublisher {

    private static final String PLUGIN_ID = "james.publisher.console";

    private final PrintStream printStream;
    private JSONEventFormatter formatter;

    public ConsolePublisher() {
        this(System.out);
    }

    @VisibleForTesting
    ConsolePublisher(PrintStream printStream) {
        this.printStream = printStream;
    }

    @Override
    public String getId() {
        return PLUGIN_ID;
    }

    @Override
    public void initialize(EventPublisherConfiguration eventPublisherConfiguration) {
        ConsolePublisherConfiguration configuration = new ConsolePublisherConfiguration(eventPublisherConfiguration);
        formatter = new JSONEventFormatter(configuration.isPrettifyJSON(), configuration.getEventType(), configuration.getEnvironment());
    }

    @Override
    public void publish(Event evt) {
        printStream.println(formatter.format(Objects.requireNonNull(evt)));
    }

    @Override
    public void close() {
        printStream.flush();
    }

}
