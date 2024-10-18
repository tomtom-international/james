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

package com.tomtom.james.script;

import java.lang.reflect.Method;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import com.lmax.disruptor.TimeoutException;
import com.lmax.disruptor.dsl.Disruptor;
import com.tomtom.james.common.api.QueueBacked;
import com.tomtom.james.common.api.informationpoint.InformationPoint;
import com.tomtom.james.common.api.script.RuntimeInformationPointParameter;
import com.tomtom.james.common.api.script.ScriptEngine;
import com.tomtom.james.common.log.Logger;
import com.tomtom.james.disruptor.JobEvent;
import com.tomtom.james.disruptor.JobEventHandler;
import com.tomtom.james.util.MoreExecutors;

class DisruptorAsyncScriptEngine implements ScriptEngine, QueueBacked {

    private static final Logger LOG = Logger.getLogger(DisruptorAsyncScriptEngine.class);

    private final ScriptEngine delegate;
    private final AtomicInteger droppedJobsCount = new AtomicInteger();

    private final Disruptor<JobEvent> disruptor;
    private final int bufferSize;
    private final ExecutorService executor;
    private final AtomicBoolean isRunning = new AtomicBoolean(true);

    DisruptorAsyncScriptEngine(ScriptEngine delegate, int numberOfWorkers, int jobQueueSize) {


        // Specify the size of the ring buffer, must be power of 2.
        bufferSize = nextPowerOf2(jobQueueSize);

        // Construct the Disruptor
        this.executor = MoreExecutors.createNamedDaemonExecutorService(
                "async-script-engine-thread-pool-%d", numberOfWorkers);
        disruptor =
                new Disruptor<>(new JobEvent.Factory(), bufferSize, executor);
        // Start the Disruptor, starts all threads running
        final JobEventHandler[] workHandlers = new JobEventHandler[numberOfWorkers];
        for (int i = 0; i < numberOfWorkers; i++) {
            workHandlers[i] = new JobEventHandler();
        }
        disruptor.handleEventsWithWorkerPool(workHandlers);
        disruptor.start();

        this.delegate = Objects.requireNonNull(delegate);
        LOG.trace(() -> "Script engine worker pool created with " + numberOfWorkers + " threads");
    }

    @Override
    public Object invokePrepareContext(InformationPoint informationPoint,
                                       Method origin,
                                       List<RuntimeInformationPointParameter> parameters,
                                       Object instance,
                                       Thread currentThread,
                                       String contextKey) {
        return delegate.invokePrepareContext(informationPoint, origin, parameters, instance, currentThread, contextKey);
    }

    @Override
    public void invokeSuccessHandler(InformationPoint informationPoint,
                                     Method origin,
                                     List<RuntimeInformationPointParameter> parameters,
                                     Object instance,
                                     Thread currentThread,
                                     Instant eventTime,
                                     Duration executionTime,
                                     String[] callStack,
                                     Object returnValue,
                                     CompletableFuture<Object> initialContextProvider) {

        if (isRunning.get() && !disruptor.getRingBuffer().tryPublishEvent((
                JobEvent event,
                long sequence) -> event.setJob(() ->
                delegate.invokeSuccessHandler(
                informationPoint,
                origin,
                parameters,
                instance,
                currentThread,
                eventTime,
                executionTime,
                callStack,
                returnValue,
                initialContextProvider)))) {
            LOG.warn("Dropping success handler execution for " + informationPoint);
            droppedJobsCount.incrementAndGet();
        }
    }

    @Override
    public void invokeErrorHandler(InformationPoint informationPoint,
                                   Method origin,
                                   List<RuntimeInformationPointParameter> parameters,
                                   Object instance,
                                   Thread currentThread,
                                   Instant eventTime,
                                   Duration executionTime,
                                   String[] callStack,
                                   Throwable errorCause,
                                   CompletableFuture<Object> initialContextProvider) {


        if (isRunning.get() && !disruptor.getRingBuffer().tryPublishEvent((
                JobEvent event,
                long sequence) -> event.setJob(() ->
                delegate.invokeErrorHandler(
                        informationPoint,
                        origin,
                        parameters,
                        instance,
                        currentThread,
                        eventTime,
                        executionTime,
                        callStack,
                        errorCause,
                        initialContextProvider)))) {
            droppedJobsCount.incrementAndGet();
            LOG.warn("Dropping error handler execution for " + informationPoint);
        }

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


    private int nextPowerOf2(int maxQueueCapacity) {
        int adjustedCapacity = maxQueueCapacity == 1 ? 1 : Integer.highestOneBit(maxQueueCapacity - 1) * 2;
        if(adjustedCapacity!=maxQueueCapacity){
            LOG.warn(String.format("Adjusting %d to nearest power of 2 ->  %d", maxQueueCapacity, adjustedCapacity));
        }
        return adjustedCapacity;
    }
}
