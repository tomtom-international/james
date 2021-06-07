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

package com.tomtom.james.informationpoint.advice;

import java.lang.reflect.Method;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;

import com.tomtom.james.common.api.informationpoint.InformationPoint;
import com.tomtom.james.common.api.script.RuntimeInformationPointParameter;
import com.tomtom.james.common.log.Logger;
import com.tomtom.james.newagent.MethodExecutionContextHelper;
import org.apache.logging.log4j.util.StackLocatorUtil;

/*
 * Note: advices are inlined so this has to be public.
 */
public final class ContextAwareAdvice {

    public static final Logger LOG = Logger.getLogger(ContextAwareAdvice.class);
    private static final String[] EMPTY_CALL_STACK = new String[0];

    private ContextAwareAdvice() {
    }

    @SuppressWarnings("unused")
    public static void onEnter(String originTypeName,
                               String originMethodName,
                               Method origin,
                               Object instance,
                               Object[] arguments) {
        try {
            LOG.trace(() -> "onEnter: START ["
                    + "originTypeName=" + originTypeName
                    + ", originMethodName=" + originMethodName
                    + "]");

            Optional<InformationPoint> informationPoint = InformationPointServiceSupplier.get()
                    .getInformationPoint(originTypeName, originMethodName);

            if (!informationPoint.isPresent()) {
                LOG.trace(() -> "onEnter: skipping");
                return;
            }

            final InformationPoint ip = informationPoint.get();
            if (!ip.getRequiresInitialContext()) {
                LOG.trace(() -> "onEnter: noInitialContextSupportRequired - skipping");
                return;
            }

            final String key = MethodExecutionContextHelper.createContextKey();
            LOG.trace(() -> "Initializing custom context setup for the call");
            final Object callContext = ScriptEngineSupplier.get().invokePrepareContext(
                    ip,
                    origin,
                    createParameterList(origin, arguments),
                    instance,
                    Thread.currentThread(),
                    key);

            MethodExecutionContextHelper.storeContextAsync(key, callContext);

        } catch (Throwable t) {
            LOG.error("Error executing onEnter advice", t);
            throw t;
        } finally {
            LOG.trace("onEnter: END");
        }
    }

    @SuppressWarnings("unused")
    public static void onExit(
            long _startTime,
            String informationPointClassName,
            String informationPointMethodName,
            Method origin,
            Object instance,
            Object[] arguments,
            Object returned,
            Throwable thrown) {
        Instant eventTime = Instant.now();
        Duration executionTime = Duration.ofNanos(System.nanoTime() - _startTime);
        boolean requireInitialContextCleanup = false;

        try {
            Optional<InformationPoint> optionalInformationPoint = InformationPointServiceSupplier.get()
                    .getInformationPoint(informationPointClassName, informationPointMethodName);
            if (!optionalInformationPoint.isPresent()) {
                LOG.trace(() -> "onExit: skipping because information point is gone");
                return;
            }
            final InformationPoint informationPoint = optionalInformationPoint.get();
            final long successExecutionThreshold = informationPoint.getSuccessExecutionThreshold();
            final double sampleRate = getSampleRate(thrown, informationPoint);

            LOG.trace(() -> "onExit: START "
                    + "[origin=" + origin
                    + ", informationPointClassName=" + informationPointClassName
                    + ", informationPointMethodName=" + informationPointMethodName
                    + ", baseScript=" + (informationPoint.getBaseScript().isPresent())
                    + ", script=" + (informationPoint.getScript().isPresent())
                    + ", sampleRate=" + informationPoint.getSampleRate()
                    + ", successSampleRate=" + informationPoint.getSuccessSampleRate()
                    + ", errorSampleRate=" + informationPoint.getErrorSampleRate()
                    + ", successExecutionThreshold=" + successExecutionThreshold
                    + ", instance=" + instance
                    + ", arguments=" + Arrays.asList(arguments)
                    + ", returned=" + returned
                    + ", thrown=" + thrown
                    + "]");

            requireInitialContextCleanup = informationPoint.getRequiresInitialContext();

            if ((sampleRate < 100) && (sampleRate < ThreadLocalRandom.current().nextDouble() * 100)) {
                LOG.trace(() -> "onExit: Sample skipped (sampleRate=" + sampleRate + ")");
                return;
            }

            final CompletableFuture<Object> initialContextAsyncProvider = requireInitialContextCleanup
                    ? MethodExecutionContextHelper.getContextAsync(MethodExecutionContextHelper.getKeyForCurrentFrame())
                    : CompletableFuture.completedFuture(null);

            final String[] callStack = informationPoint.getRequiresCallStack() ? getCallStack() : EMPTY_CALL_STACK;
            if (thrown == null) {
                if (executionTime.toMillis() < successExecutionThreshold) {
                    LOG.trace(() -> "onExit: ExecutionTime skipped (executionTime=" + executionTime.toMillis() + ")");
                    return;
                }
                LOG.trace(() -> "onExit: Invoking success handler");
                ScriptEngineSupplier.get().invokeSuccessHandler(
                        informationPoint,
                        origin,
                        createParameterList(origin, arguments),
                        instance,
                        Thread.currentThread(),
                        eventTime,
                        executionTime,
                        callStack,
                        returned,
                        initialContextAsyncProvider
                );
            } else {
                LOG.trace(() -> "onExit: Invoking error handler");
                ScriptEngineSupplier.get().invokeErrorHandler(
                        informationPoint,
                        origin,
                        createParameterList(origin, arguments),
                        instance,
                        Thread.currentThread(),
                        eventTime,
                        executionTime,
                        callStack,
                        thrown,
                        initialContextAsyncProvider
                );
            }
        } catch (Throwable t) {
            LOG.error("Error executing onExit advice", t);
            throw t;
        } finally {
            if (requireInitialContextCleanup) {
                MethodExecutionContextHelper.removeContextKey();
            }
            LOG.trace("onExit: END");
        }
    }

    private static double getSampleRate(Throwable thrown, InformationPoint informationPoint) {
        if (thrown == null) {
            return informationPoint.getSuccessSampleRate();
        } else {
            return informationPoint.getErrorSampleRate();
        }
    }

    public static String[] getCallStack() {
        int size = 100;
        int adviceStackEntryCount = 2;
        String[] callStack = new String[size];
        for (int i = 0; i < size; i++) {
            Class c = StackLocatorUtil.getCallerClass(i + adviceStackEntryCount);
            if (c == null) {
                return Arrays.copyOfRange(callStack, 0, i);
            }
            callStack[i] = c.getName();
        }
        return callStack;
    }

    public static List<RuntimeInformationPointParameter> createParameterList(Method method, Object[] args) {
        List<RuntimeInformationPointParameter> result = new ArrayList<>(method.getParameterCount());
        for (int i = 0; i < method.getParameterCount(); i++) {
            RuntimeInformationPointParameter p = new RuntimeInformationPointParameter(
                    method.getParameters()[i].getName(),
                    method.getParameters()[i].getType(),
                    args[i]
            );
            result.add(p);
        }
        return result;
    }

    @SuppressWarnings("unused")
    public static void onEnter(String originTypeName,
                               String originMethodName) {
        onEnter(originTypeName, originMethodName, null, null, null);
    }


    @SuppressWarnings("unused")
    public static void onExit(
            long _startTime,
            String informationPointClassName,
            String informationPointMethodName,
            Method origin,
            Object instance,
            Object[] arguments,
            byte returned,
            Throwable thrown) {
        onExit(_startTime, informationPointClassName, informationPointMethodName, origin, instance, arguments, Byte.valueOf(returned), thrown);
    }

    @SuppressWarnings("unused")
    public static void onExit(
            long _startTime,
            String informationPointClassName,
            String informationPointMethodName,
            Method origin,
            Object instance,
            Object[] arguments,
            short returned,
            Throwable thrown) {
        onExit(_startTime, informationPointClassName, informationPointMethodName, origin, instance, arguments, Short.valueOf(returned), thrown);
    }

    @SuppressWarnings("unused")
    public static void onExit(
            long _startTime,
            String informationPointClassName,
            String informationPointMethodName,
            Method origin,
            Object instance,
            Object[] arguments,
            int returned,
            Throwable thrown) {
        onExit(_startTime, informationPointClassName, informationPointMethodName, origin, instance, arguments, Integer.valueOf(returned), thrown);
    }

    @SuppressWarnings("unused")
    public static void onExit(
            long _startTime,
            String informationPointClassName,
            String informationPointMethodName,
            Method origin,
            Object instance,
            Object[] arguments,
            long returned,
            Throwable thrown) {
        onExit(_startTime, informationPointClassName, informationPointMethodName, origin, instance, arguments, Long.valueOf(returned), thrown);
    }

    @SuppressWarnings("unused")
    public static void onExit(
            long _startTime,
            String informationPointClassName,
            String informationPointMethodName,
            Method origin,
            Object instance,
            Object[] arguments,
            float returned,
            Throwable thrown) {
        onExit(_startTime, informationPointClassName, informationPointMethodName, origin, instance, arguments, Float.valueOf(returned), thrown);
    }

    @SuppressWarnings("unused")
    public static void onExit(
            long _startTime,
            String informationPointClassName,
            String informationPointMethodName,
            Method origin,
            Object instance,
            Object[] arguments,
            double returned,
            Throwable thrown) {
        onExit(_startTime, informationPointClassName, informationPointMethodName, origin, instance, arguments, Double.valueOf(returned), thrown);
    }

    @SuppressWarnings("unused")
    public static void onExit(
            long _startTime,
            String informationPointClassName,
            String informationPointMethodName,
            Method origin,
            Object instance,
            Object[] arguments,
            char returned,
            Throwable thrown) {
        onExit(_startTime, informationPointClassName, informationPointMethodName, origin, instance, arguments, Character.valueOf(returned), thrown);
    }

}

