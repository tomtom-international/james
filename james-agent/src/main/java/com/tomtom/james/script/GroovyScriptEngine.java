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

import com.google.common.base.Stopwatch;
import com.tomtom.james.agent.ToolkitManager;
import com.tomtom.james.common.api.publisher.EventPublisher;
import com.tomtom.james.common.api.script.RuntimeInformationPointParameter;
import com.tomtom.james.common.api.script.ScriptEngine;
import com.tomtom.james.common.log.Logger;
import groovy.lang.GroovyShell;
import groovy.lang.MissingMethodException;
import org.codehaus.groovy.control.CompilationFailedException;
import org.codehaus.groovy.control.CompilerConfiguration;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

class GroovyScriptEngine implements ScriptEngine {

    private static final Logger LOG = Logger.getLogger(GroovyScriptEngine.class);

    private static final String SUCCESS_HANDLER_FUNCTION = "onSuccess";
    private static final String ERROR_HANDLER_FUNCTION = "onError";

    private final EventPublisher publisher;
    private final ToolkitManager toolkitManager;
    private final GroovyShell groovyShell;
    private final ConcurrentHashMap<String, InformationPointHandler> handlersCache = new ConcurrentHashMap<>();

    GroovyScriptEngine(EventPublisher publisher, ToolkitManager toolkitManager) {
        this.publisher = Objects.requireNonNull(publisher);
        this.toolkitManager = Objects.requireNonNull(toolkitManager);
        CompilerConfiguration compilerConfiguration = new CompilerConfiguration();
        compilerConfiguration.setScriptBaseClass(InformationPointHandler.class.getName());
        this.groovyShell = new GroovyShell(Thread.currentThread().getContextClassLoader(), compilerConfiguration);
    }

    @Override
    public void invokeSuccessHandler(String informationPointClassName,
                                     String informationPointMethodName,
                                     String script,
                                     Method origin,
                                     List<RuntimeInformationPointParameter> parameters,
                                     Object instance,
                                     Thread currentThread,
                                     Duration executionTime,
                                     String[] callStack,
                                     Object returnValue) {
        LOG.trace(() -> "Invoking success handler for " + informationPointClassName + "#" + informationPointMethodName);
        try {
            Stopwatch stopwatch = Stopwatch.createStarted();
            InformationPointHandler handler = createOrGetCachedHandler(script);
            SuccessHandlerContext handlerContext = new SuccessHandlerContext(
                    informationPointClassName, informationPointMethodName, origin, parameters, instance,
                    currentThread, executionTime, callStack, returnValue);
            handler.invokeMethod(SUCCESS_HANDLER_FUNCTION, new Object[]{handlerContext});
            LOG.trace(() -> "Success handler invocation took " + Duration.ofNanos(stopwatch.elapsed(TimeUnit.NANOSECONDS)));
        } catch (CompilationFailedException e) {
            LOG.error(() -> "Script compilation failed when calling success handler for "
                    + informationPointClassName + "#" + informationPointMethodName, e);
        } catch (MissingMethodException mme) {
            LOG.error(() -> "Success handler function missing for "
                    + informationPointClassName + "#" + informationPointMethodName, mme);
        } catch (Throwable t) {
            LOG.error(() -> "Success handler invocation failed for "
                    + informationPointClassName + "#" + informationPointMethodName, t);
        }
    }

    @Override
    public void invokeErrorHandler(String informationPointClassName,
                                   String informationPointMethodName,
                                   String script,
                                   Method origin,
                                   List<RuntimeInformationPointParameter> parameters,
                                   Object instance,
                                   Thread currentThread,
                                   Duration executionTime,
                                   String[] callStack,
                                   Throwable errorCause) {
        LOG.trace(() -> "Invoking error handler for " + informationPointClassName + "#" + informationPointMethodName);
        try {
            Stopwatch stopwatch = Stopwatch.createStarted();
            InformationPointHandler handler = createOrGetCachedHandler(script);
            ErrorHandlerContext handlerContext = new ErrorHandlerContext(
                    informationPointClassName, informationPointMethodName, origin, parameters, instance,
                    currentThread, executionTime, callStack, errorCause);
            handler.invokeMethod(ERROR_HANDLER_FUNCTION, new Object[]{handlerContext});
            LOG.trace(() -> "Error handler invocation took " + Duration.ofNanos(stopwatch.elapsed(TimeUnit.NANOSECONDS)));
        } catch (CompilationFailedException cfe) {
            LOG.error(() -> "Script compilation failed when calling error handler for "
                    + informationPointClassName + "#" + informationPointMethodName, cfe);
        } catch (MissingMethodException mme) {
            LOG.error(() -> "Error handler function missing for "
                    + informationPointClassName + "#" + informationPointMethodName, mme);
        } catch (Throwable t) {
            LOG.error(() -> "Error handler invocation failed for "
                    + informationPointClassName + "#" + informationPointMethodName, t);
        }
    }

    @Override
    public void close() {
        // Do nothing
    }

    private InformationPointHandler createOrGetCachedHandler(String script)
            throws CompilationFailedException {
        return handlersCache.computeIfAbsent(script, scriptTextKey -> {
            InformationPointHandler handler = (InformationPointHandler) groovyShell.parse(scriptTextKey);
            handler.setEventPublisher(publisher);
            handler.setToolkitManager(toolkitManager);
            return handler;
        });
    }

}
