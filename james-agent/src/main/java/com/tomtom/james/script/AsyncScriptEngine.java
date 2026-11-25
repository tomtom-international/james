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
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import com.tomtom.james.common.api.QueueBacked;
import com.tomtom.james.common.api.informationpoint.InformationPoint;
import com.tomtom.james.common.api.script.RuntimeInformationPointParameter;
import com.tomtom.james.common.api.script.ScriptEngine;
import com.tomtom.james.common.log.Logger;
import com.tomtom.james.util.MoreExecutors;

/**
 * Runs handlers on a fixed pool sharing one bounded queue, so a handler that blocks (e.g. a toolkit HTTP call)
 * holds only its own worker; when the queue is full the invocation is dropped and counted.
 */
class AsyncScriptEngine implements ScriptEngine, QueueBacked {

    private static final Logger LOG = Logger.getLogger(AsyncScriptEngine.class);

    private final ScriptEngine delegate;
    private final AtomicInteger droppedJobsCount = new AtomicInteger();

    private final BlockingQueue<Runnable> queue;
    private final ThreadPoolExecutor executor;
    private final AtomicBoolean isRunning = new AtomicBoolean(true);

    AsyncScriptEngine(ScriptEngine delegate, int numberOfWorkers, int jobQueueSize) {
        this.delegate = Objects.requireNonNull(delegate);
        queue = new ArrayBlockingQueue<>(jobQueueSize);
        executor = new ThreadPoolExecutor(numberOfWorkers, numberOfWorkers, 0L, TimeUnit.MILLISECONDS, queue,
                MoreExecutors.createNamedDaemonThreadFactory("async-script-engine-thread-pool-%d"));
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
        if (isRunning.get() && !submit(() -> delegate.invokeSuccessHandler(
                informationPoint,
                origin,
                parameters,
                instance,
                currentThread,
                eventTime,
                executionTime,
                callStack,
                returnValue,
                initialContextProvider))) {
            LOG.warn("Dropping success handler execution for " + informationPoint);
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
        if (isRunning.get() && !submit(() -> delegate.invokeErrorHandler(
                informationPoint,
                origin,
                parameters,
                instance,
                currentThread,
                eventTime,
                executionTime,
                callStack,
                errorCause,
                initialContextProvider))) {
            LOG.warn("Dropping error handler execution for " + informationPoint);
        }
    }

    private boolean submit(Runnable job) {
        try {
            executor.execute(() -> {
                try {
                    job.run();
                } catch (Exception e) {
                    LOG.error("Unhandled exception in script engine worker.", e);
                }
            });
            return true;
        } catch (RejectedExecutionException e) {
            droppedJobsCount.incrementAndGet();
            return false;
        }
    }

    @Override
    public void close() {
        LOG.trace("Shutting down executor...");
        isRunning.set(false);
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                LOG.trace("Executor shutdown timed out, interrupting workers");
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        } finally {
            LOG.trace("Executor shutdown completed.");
            delegate.close();
        }
    }

    @Override
    public int getJobQueueSize() {
        return queue.size();
    }

    @Override
    public int getJobQueueRemainingCapacity() {
        return queue.remainingCapacity();
    }

    @Override
    public int getDroppedJobsCount() {
        return droppedJobsCount.get();
    }
}
