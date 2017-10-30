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

import com.google.common.base.Stopwatch;
import com.tomtom.james.common.api.script.RuntimeInformationPointParameter;
import com.tomtom.james.common.log.Logger;
import com.tomtom.james.informationpoint.annotations.InformationPointClassName;
import com.tomtom.james.informationpoint.annotations.InformationPointMethodName;
import com.tomtom.james.informationpoint.annotations.InformationPointSampleRate;
import com.tomtom.james.informationpoint.annotations.InformationPointScript;
import net.bytebuddy.implementation.bytecode.assign.Assigner;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static net.bytebuddy.asm.Advice.*;

/*
 * Note: advices are inlined so this has to be public.
 */
public class ContextAwareAdvice {

    public static final Logger LOG = Logger.getLogger(ContextAwareAdvice.class);
    public static final Random RND = new Random();

    @SuppressWarnings("unused")
    @OnMethodEnter
    static Stopwatch onEnter(@Origin("#t") String originTypeName,
                             @Origin("#m") String originMethodName) {
        try {
            if (LOG.isTraceEnabled()) {
                LOG.trace("onEnter: START ["
                        + "originTypeName=" + originTypeName
                        + ", originMethodName=" + originMethodName
                        + "]");
            }
            return Stopwatch.createStarted();
        } catch (Throwable t) {
            LOG.error("Error executing onEnter advice", t);
            throw t;
        } finally {
            LOG.trace("onEnter: END");
        }
    }

    @SuppressWarnings("unused")
    @OnMethodExit(onThrowable = Exception.class)
    static void onExit(@Enter Stopwatch stopwatch,
                       @InformationPointClassName String informationPointClassName,
                       @InformationPointMethodName String informationPointMethodName,
                       @InformationPointScript String script,
                       @InformationPointSampleRate int sampleRate,
                       @Origin Method origin,
                       @This(optional = true) Object instance,
                       @AllArguments Object[] arguments,
                       @Return(typing = Assigner.Typing.DYNAMIC) Object returned,
                       @Thrown Throwable thrown) {

        stopwatch.stop();
        Duration executionTime = stopwatch.elapsed();

        try {
            if (LOG.isTraceEnabled()) {
                LOG.trace("onExit: START "
                        + "[origin=" + origin
                        + ", informationPointClassName=" + informationPointClassName
                        + ", informationPointMethodName=" + informationPointMethodName
                        + ", script=" + (script != null)
                        + ", sampleRate=" + sampleRate
                        + ", instance=" + instance
                        + ", arguments=" + Arrays.asList(arguments)
                        + ", returned=" + returned
                        + ", thrown=" + thrown
                        + "]");
            }

            if ((sampleRate < 100) && (sampleRate < RND.nextDouble() * 100)) {
                if (LOG.isTraceEnabled()) {
                    LOG.trace("onExit: Sample skipped (sampleRate=" + sampleRate + ")");
                }
                return;
            }

            if (thrown == null) {
                LOG.trace("onExit: Invoking success handler");
                ScriptEngineSupplier.get().invokeSuccessHandler(
                        informationPointClassName,
                        informationPointMethodName,
                        script,
                        origin,
                        createParameterList(origin, arguments),
                        instance,
                        Thread.currentThread(),
                        executionTime,
                        getCallStack(),
                        returned
                );
            } else {
                LOG.trace("onExit: Invoking error handler");
                ScriptEngineSupplier.get().invokeErrorHandler(
                        informationPointClassName,
                        informationPointMethodName,
                        script,
                        origin,
                        createParameterList(origin, arguments),
                        instance,
                        Thread.currentThread(),
                        executionTime,
                        getCallStack(),
                        thrown
                );
            }
        } catch (Throwable t) {
            LOG.error("Error executing onExit advice", t);
            throw t;
        } finally {
            LOG.trace("onExit: END");
        }
    }

    public static String[] getCallStack() {
        int size = 100;
        int adviceStackEntryCount = 2;
        String[] callStack = new String[size];
        for (int i = 0; i < size; i++) {
            Class c = sun.reflect.Reflection.getCallerClass(i + adviceStackEntryCount);
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
}

