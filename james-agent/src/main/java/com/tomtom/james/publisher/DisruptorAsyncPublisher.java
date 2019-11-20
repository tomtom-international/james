/*
 * Copyright 2017 TomTom International B.V. Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under the License.
 */

package com.tomtom.james.publisher;

import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.common.annotations.VisibleForTesting;
import com.lmax.disruptor.EventTranslator;
import com.lmax.disruptor.TimeoutException;
import com.lmax.disruptor.dsl.Disruptor;
import com.tomtom.james.common.api.QueueBacked;
import com.tomtom.james.common.api.configuration.EventPublisherConfiguration;
import com.tomtom.james.common.api.publisher.Event;
import com.tomtom.james.common.api.publisher.EventPublisher;
import com.tomtom.james.common.log.Logger;
import com.tomtom.james.disruptor.JobEvent;
import com.tomtom.james.disruptor.JobEventHandler;
import com.tomtom.james.util.MoreExecutors;

class DisruptorAsyncPublisher implements EventPublisher, QueueBacked {

    private static final String PLUGIN_ID = "james.publisher.disruptor";

    private static final Logger LOG = Logger.getLogger(DisruptorAsyncPublisher.class);

    private final EventPublisher delegate;
    private final AtomicInteger droppedJobsCount = new AtomicInteger();
    private final Disruptor<JobEvent> disruptor;
    private final int bufferSize;
    private final ExecutorService executor;

    private final AtomicBoolean isRunning = new AtomicBoolean(true);
    DisruptorAsyncPublisher(
        EventPublisher delegate,
        String threadPoolNameFormat,
        int numberOfWorkers,
        int maxQueueCapacity) {

        // Specify the size of the ring buffer, must be power of 2.
        bufferSize = nextPowerOf2(maxQueueCapacity);


        // Construct the Disruptor
        executor = MoreExecutors.createNamedDaemonExecutorService(threadPoolNameFormat, numberOfWorkers);
        disruptor = new Disruptor<>(new JobEvent.Factory(), bufferSize, executor);
        // Start the Disruptor, starts all threads running
        disruptor.handleEventsWith(new JobEventHandler());
        disruptor.start();

        // Get the ring buffer from the Disruptor to be used for publishing.
        this.delegate = Objects.requireNonNull(delegate);
        LOG.trace(() -> "Async worker pool for " + delegate.getId() + " created with " + numberOfWorkers + (numberOfWorkers > 1 ? " threads" : " thread"));
    }

    private int nextPowerOf2(int maxQueueCapacity) {
        int adjustedCapacity = maxQueueCapacity == 1 ? 1 : Integer.highestOneBit(maxQueueCapacity - 1) * 2;
        if(adjustedCapacity!=maxQueueCapacity){
            LOG.warn(String.format("Adjusting %d to nearest power of 2 ->  %d", maxQueueCapacity, adjustedCapacity));
        }
        return adjustedCapacity;
    }

    @Override
    public String getId() {

        return PLUGIN_ID;
    }

    @Override
    public void initialize(
        EventPublisherConfiguration configuration) {

        // do nothing
    }

    @Override
    public void publish(
        Event evt) {

        if (isRunning.get() && !disruptor.getRingBuffer().tryPublishEvent(translateEvent(evt))) {
            droppedJobsCount.incrementAndGet();
        }
    }

    private EventTranslator<JobEvent> translateEvent(Event evt) {
        return (
            JobEvent event,
            long sequence) -> event.setJob(publishToDelegate(evt));
    }

    private Runnable publishToDelegate(Event evt) {
        return () -> delegate.publish(evt);
    }

    @Override
    public void close() {
        LOG.trace("Shutting down executor...");
        isRunning.set(false);
        try {
            disruptor.shutdown(5, TimeUnit.SECONDS);
            executor.shutdownNow();
            executor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (TimeoutException | InterruptedException e) {
            LOG.trace("Executor shutdown interrupted", e);
        } finally {

            LOG.trace("Executor shutdown completed.");
            delegate.close();
        }
    }

    @Override
    public int getJobQueueSize() {
        return bufferSize - getJobQueueRemainingCapacity();
    }

    @Override
    public int getJobQueueRemainingCapacity() {
        return (int) disruptor.getRingBuffer().remainingCapacity();
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
