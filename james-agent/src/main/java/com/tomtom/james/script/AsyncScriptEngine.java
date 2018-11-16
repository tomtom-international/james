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

import com.tomtom.james.common.api.QueueBacked;
import com.tomtom.james.common.api.informationpoint.InformationPoint;
import com.tomtom.james.common.api.script.RuntimeInformationPointParameter;
import com.tomtom.james.common.api.script.ScriptEngine;
import com.tomtom.james.common.log.Logger;
import com.tomtom.james.util.AsyncRunner;
import com.tomtom.james.util.MoreExecutors;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

class AsyncScriptEngine implements ScriptEngine, QueueBacked {

    private static final Logger LOG = Logger.getLogger(AsyncScriptEngine.class);

    private final BlockingQueue<Runnable> jobQueue;
    private final ScriptEngine delegate;
    private final ExecutorService executorService;
    private final AtomicInteger droppedJobsCount = new AtomicInteger();

    AsyncScriptEngine(ScriptEngine delegate, int numberOfWorkers, int jobQueueSize) {
        this.delegate = Objects.requireNonNull(delegate);
        this.executorService = MoreExecutors.createNamedDaemonExecutorService(
                "async-script-engine-thread-pool-%d", numberOfWorkers);
        this.jobQueue = new ArrayBlockingQueue<>(jobQueueSize, true);
        LOG.trace(() -> "Script engine worker pool created with " + numberOfWorkers + " threads");
        IntStream.range(0, numberOfWorkers).forEach(i ->
                executorService.submit(new AsyncRunner<>(jobQueue)));
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
                                     Duration executionTime,
                                     String[] callStack,
                                     Object returnValue,
                                     CompletableFuture<Object> initialContextProvider) {
        if (!jobQueue.offer(() ->
                delegate.invokeSuccessHandler(
                        informationPoint,
                        origin,
                        parameters,
                        instance,
                        currentThread,
                        executionTime,
                        callStack,
                        returnValue,
                        initialContextProvider))) {
            droppedJobsCount.incrementAndGet();
        }
    }

    @Override
    public void invokeErrorHandler(InformationPoint informationPoint,
                                   Method origin,
                                   List<RuntimeInformationPointParameter> parameters,
                                   Object instance,
                                   Thread currentThread,
                                   Duration executionTime,
                                   String[] callStack,
                                   Throwable errorCause,
                                   CompletableFuture<Object> initialContextProvider) {
        if (!jobQueue.offer(() ->
                delegate.invokeErrorHandler(
                        informationPoint,
                        origin,
                        parameters,
                        instance,
                        currentThread,
                        executionTime,
                        callStack,
                        errorCause,
                        initialContextProvider))) {
            droppedJobsCount.incrementAndGet();
        }
    }

    @Override
    public void close() {
        LOG.trace("Shutting down script engine executor...");
        try {
            executorService.shutdownNow();
            executorService.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            LOG.trace("Executor shutdown interrupted", e);
        } finally {
            if (jobQueue.isEmpty()) {
                LOG.trace("Executor shutdown completed.");
            } else {
                LOG.warn(() -> "Executor shutdown completed with " + jobQueue.size() + " jobs left in the queue");
            }
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
}
