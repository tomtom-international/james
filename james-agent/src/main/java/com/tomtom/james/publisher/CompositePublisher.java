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
import com.tomtom.james.common.api.Closeable;
import com.tomtom.james.common.api.QueueBacked;
import com.tomtom.james.common.api.configuration.EventPublisherConfiguration;
import com.tomtom.james.common.api.publisher.Event;
import com.tomtom.james.common.api.publisher.EventPublisher;

import java.util.List;

class CompositePublisher implements EventPublisher, QueueBacked {

    private static final String PLUGIN_ID = "james.publisher.composite";

    private final List<EventPublisher> delegates;

    CompositePublisher(List<EventPublisher> delegates) {
        this.delegates = delegates;
    }

    @Override
    public String getId() {
        return PLUGIN_ID;
    }

    @Override
    public void initialize(EventPublisherConfiguration configuration) {
        // do nothing
    }

    public void close() {
        delegates.parallelStream().forEach(Closeable::close);
    }

    @Override
    public void publish(Event evt) {
        delegates.forEach(delegate -> delegate.publish(evt));
    }

    @Override
    public int getJobQueueSize() {
        return delegates.stream()
                .mapToInt(delegate -> delegate instanceof QueueBacked ? ((QueueBacked) delegate).getJobQueueSize() : 0)
                .sum();
    }

    @Override
    public int getJobQueueRemainingCapacity() {
        return delegates.stream()
                .mapToInt(delegate -> delegate instanceof QueueBacked ? ((QueueBacked) delegate).getJobQueueRemainingCapacity() : 0)
                .sum();
    }

    @Override
    public int getDroppedJobsCount() {
        return delegates.stream()
                .mapToInt(delegate -> delegate instanceof QueueBacked ? ((QueueBacked) delegate).getDroppedJobsCount() : 0)
                .sum();
    }

    @VisibleForTesting
    List<EventPublisher> getDelegates() {
        return delegates;
    }
}
