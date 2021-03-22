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

package com.tomtom.james.newagent;

import com.tomtom.james.common.log.Logger;
import com.tomtom.james.util.MoreExecutors;

import java.util.ArrayDeque;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

public class MethodExecutionContextHelper {
    private static final Logger LOG = Logger.getLogger(MethodExecutionContextHelper.class);

    public static ThreadLocal<ArrayDeque<String>> keysStack = ThreadLocal.withInitial(() -> new ArrayDeque<>(8));
    private static WeakHashMap<String, Object> contextStore = new WeakHashMap<>();

    private static ExecutorService contextStoreAccessExecutor = MoreExecutors
            .createNamedDaemonExecutorService("james-context-access-%d", 1);

    private static ExecutorService contextCallbackExecutor = MoreExecutors
            .createNamedDaemonExecutorService("james-context-callback-%d", 5);

    public static void shutdown() {
        try {
            contextStoreAccessExecutor.shutdown();
            contextStoreAccessExecutor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            LOG.warn("Executor contextStoreAccessExecutor shutdown interrupted " + e);
        }
        try {
            contextCallbackExecutor.shutdown();
            contextCallbackExecutor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            LOG.warn("Executor contextCallbackExecutor shutdown interrupted " + e);
        }
    }

    public static String createContextKey() {
        final String contextKey = UUID.randomUUID().toString();
        keysStack.get().push(contextKey);
        return contextKey;
    }

    public static String getKeyForCurrentFrame() {
        return keysStack.get().peek();
    }

    public static String removeContextKey() {
        return keysStack.get().pop();
    }

    public static CompletableFuture<Object> storeContextAsync(final String key, final Object value) {
        final CompletableFuture result = new CompletableFuture();
        contextStoreAccessExecutor.submit(() -> {
            contextStore.put(key, value);
            CompletableFuture.supplyAsync(() -> result.complete(value), contextCallbackExecutor);
        });
        return result;
    }

    public static CompletableFuture<Object> getContextAsync(final String key) {
        final CompletableFuture result = new CompletableFuture();
        contextStoreAccessExecutor.submit(() -> {
            if (contextStore.containsKey(key)) {
                final Object context = contextStore.get(key);
                CompletableFuture.supplyAsync(() -> result.complete(context), contextCallbackExecutor);
                return;
            }

            CompletableFuture.supplyAsync(() -> {
                final String msg = String.format("Key '%s' not found in context container", key);
                result.completeExceptionally(new IllegalArgumentException(msg));
                return null;
            }, contextCallbackExecutor);
        });
        return result;
    }
}
