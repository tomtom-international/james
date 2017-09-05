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
import com.tomtom.james.common.api.QueueBacked;
import com.tomtom.james.common.api.configuration.EventPublisherConfiguration;
import com.tomtom.james.common.api.publisher.Event;
import com.tomtom.james.common.api.publisher.EventPublisher;
import com.tomtom.james.common.log.Logger;
import com.tomtom.james.util.AsyncRunner;
import com.tomtom.james.util.MoreExecutors;

import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

class AsyncPublisher implements EventPublisher, QueueBacked {

    private static final String PLUGIN_ID = "james.publisher.async";

    private static final Logger LOG = Logger.getLogger(AsyncPublisher.class);

    private final BlockingQueue<Runnable> jobQueue;
    private final EventPublisher delegate;
    private final ExecutorService executorService;
    private final AtomicInteger droppedJobsCount = new AtomicInteger();

    AsyncPublisher(EventPublisher delegate, String threadPoolNameFormat, int numberOfWorkers, int maxQueueCapacity) {
        this.delegate = Objects.requireNonNull(delegate);
        this.executorService = MoreExecutors.createNamedDaemonExecutorService(threadPoolNameFormat, numberOfWorkers);
        this.jobQueue = new ArrayBlockingQueue<>(maxQueueCapacity, true);
        LOG.trace(() -> "Async worker pool for " + delegate.getId() + " created with " + numberOfWorkers +
                (numberOfWorkers > 1 ? " threads" : " thread"));
        IntStream.range(0, numberOfWorkers).forEach(i ->
                executorService.submit(new AsyncRunner<>(jobQueue)));
    }

    @Override
    public String getId() {
        return PLUGIN_ID;
    }

    @Override
    public void initialize(EventPublisherConfiguration configuration) {
        // do nothing
    }

    @Override
    public void publish(Event evt) {
        try {
            if (jobQueue.remainingCapacity() > 0) {
                jobQueue.put(() -> delegate.publish(evt));
            } else {
                droppedJobsCount.incrementAndGet();
            }
        } catch (InterruptedException e) {
            LOG.trace("Publishing interrupted", e);
        }
    }

    @Override
    public void close() {
        LOG.trace("Shutting down executor...");
        try {
            executorService.shutdownNow();
            executorService.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            LOG.trace("Executor shutdown interrupted", e);
        } finally {
            if (!jobQueue.isEmpty()) {
                LOG.trace(() -> "Flushing pending messages, queue size = " + jobQueue.size());
                jobQueue.forEach(Runnable::run);
            }
            LOG.trace("Executor shutdown completed.");
            delegate.close();
        }
    }

    @Override
    public int getJobQueueSize() {
        return jobQueue.size();
    }

    @Override
    public int getJobQueueRemainingCapacity() {
        return jobQueue.remainingCapacity();
    }

    @Override
    public int getDroppedJobsCount() {
        return droppedJobsCount.get();
    }

    @VisibleForTesting
    EventPublisher getDelegate() {
        return delegate;
    }
}
