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
import com.tomtom.james.common.api.informationpoint.InformationPoint;
import com.tomtom.james.common.api.publisher.EventPublisher;
import com.tomtom.james.common.api.script.RuntimeInformationPointParameter;
import com.tomtom.james.common.api.script.ScriptEngine;
import com.tomtom.james.common.log.Logger;
import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyShell;
import groovy.lang.MissingMethodException;
import org.codehaus.groovy.control.CompilationFailedException;
import org.codehaus.groovy.control.CompilerConfiguration;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

class GroovyScriptEngine implements ScriptEngine {

    private static final Logger LOG = Logger.getLogger(GroovyScriptEngine.class);

    private static final String SUCCESS_HANDLER_FUNCTION = "onSuccess";
    private static final String ERROR_HANDLER_FUNCTION = "onError";
    private static final String PREPARE_CONTEXT = "onPrepareContext";

    private final EventPublisher publisher;
    private final ToolkitManager toolkitManager;
    private final GroovyShell groovyShell;
    private final ConcurrentHashMap<String, InformationPointHandler> handlersCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, GroovyShell> shellsCache = new ConcurrentHashMap<>();
    private final ClassLoader contextClassLoader;

    GroovyScriptEngine(EventPublisher publisher, ToolkitManager toolkitManager) {
        this.publisher = Objects.requireNonNull(publisher);
        this.toolkitManager = Objects.requireNonNull(toolkitManager);
        this.contextClassLoader = Thread.currentThread().getContextClassLoader();
        this.groovyShell = createGroovyShell(contextClassLoader, InformationPointHandler.class);
    }

    @Override
    public Object invokePrepareContext(InformationPoint informationPoint,
                                       Method origin,
                                       List<RuntimeInformationPointParameter> parameters,
                                       Object instance,
                                       Thread currentThread,
                                       String contextKey) {
        LOG.trace(() -> "Invoking prepareContext handler for " + informationPoint.getClassName() + "#" + informationPoint.getMethodName());

        try {
            InformationPointHandler handler = createOrGetCachedHandler(informationPoint);
            PrepareContextHandlerContext contextPrepareArgs = new PrepareContextHandlerContext(
                    informationPoint.getClassName(),
                    informationPoint.getMethodName(),
                    origin,
                    instance,
                    parameters,
                    currentThread,
                    contextKey);

            final Object result = handler.invokeMethod(PREPARE_CONTEXT, new Object[]{contextPrepareArgs});
            LOG.trace(() -> "Context preparation " + informationPoint.getClassName() + "#" + informationPoint.getMethodName() + " completed!");
            return result;
        } catch (CompilationFailedException e) {
            LOG.error(() -> "Script compilation failed when calling prepareContext handler for "
                    + informationPoint.getClassName() + "#" + informationPoint.getMethodName(), e);
        } catch (MissingMethodException mme) {
            LOG.error(() -> "Success handler function missing for "
                    + informationPoint.getClassName() + "#" + informationPoint.getMethodName(), mme);
        } catch (Throwable t) {
            LOG.error(() -> "Success handler invocation failed for "
                    + informationPoint.getClassName() + "#" + informationPoint.getMethodName(), t);
        }
        return null;

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
        LOG.trace(() -> "Invoking success handler for " + informationPoint.getClassName() + "#" + informationPoint.getMethodName());
        try {
            Stopwatch stopwatch = Stopwatch.createStarted();
            InformationPointHandler handler = createOrGetCachedHandler(informationPoint);
            SuccessHandlerContext handlerContext = new SuccessHandlerContext(
                    informationPoint.getClassName(), informationPoint.getMethodName(), origin, parameters, instance,
                    currentThread, executionTime, callStack, returnValue, initialContextProvider.get());
            handler.invokeMethod(SUCCESS_HANDLER_FUNCTION, new Object[]{handlerContext});
            stopwatch.stop();
            LOG.trace(() -> "Success handler invocation took " + stopwatch.elapsed());
        } catch (CompilationFailedException e) {
            LOG.error(() -> "Script compilation failed when calling success handler for "
                    + informationPoint.getClassName() + "#" + informationPoint.getMethodName(), e);
        } catch (MissingMethodException mme) {
            LOG.error(() -> "Success handler function missing for "
                    + informationPoint.getClassName() + "#" + informationPoint.getMethodName(), mme);
        } catch (Throwable t) {
            LOG.error(() -> "Success handler invocation failed for "
                    + informationPoint.getClassName() + "#" + informationPoint.getMethodName(), t);
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
        LOG.trace(() -> "Invoking error handler for " + informationPoint.getClassName() + "#" + informationPoint.getMethodName());
        try {
            Stopwatch stopwatch = Stopwatch.createStarted();
            InformationPointHandler handler = createOrGetCachedHandler(informationPoint);
            ErrorHandlerContext handlerContext = new ErrorHandlerContext(
                    informationPoint.getClassName(), informationPoint.getMethodName(), origin, parameters, instance,
                    currentThread, executionTime, callStack, errorCause, initialContextProvider.get());
            handler.invokeMethod(ERROR_HANDLER_FUNCTION, new Object[]{handlerContext});
            stopwatch.stop();
            LOG.trace(() -> "Error handler invocation took " + stopwatch.elapsed());
        } catch (CompilationFailedException cfe) {
            LOG.error(() -> "Script compilation failed when calling error handler for "
                    + informationPoint.getClassName() + "#" + informationPoint.getMethodName(), cfe);
        } catch (MissingMethodException mme) {
            LOG.error(() -> "Error handler function missing for "
                    + informationPoint.getClassName() + "#" + informationPoint.getMethodName(), mme);
        } catch (Throwable t) {
            LOG.error(() -> "Error handler invocation failed for "
                    + informationPoint.getClassName() + "#" + informationPoint.getMethodName(), t);
        }
    }

    @Override
    public void close() {
        // Do nothing
    }

    private InformationPointHandler createOrGetCachedHandler(InformationPoint informationPoint)
            throws CompilationFailedException {
        InformationPointHandler handlerFromCache = handlersCache.computeIfAbsent(informationPoint.getScript().get(), scriptTextKey -> {
            InformationPointHandler handler = (InformationPointHandler) getOrCreateShell(informationPoint.getBaseScript()).parse(scriptTextKey);
            handler.setEventPublisher(publisher);
            handler.setToolkitManager(toolkitManager);
            return handler;
        });
        handlerFromCache.setMetadata(informationPoint.getMetadata());
        return handlerFromCache;
    }

    private GroovyShell getOrCreateShell(Optional<String> baseScript) {
        if (baseScript.isPresent()) {
            return shellsCache.computeIfAbsent(baseScript.get(), baseScriptTextKey -> createGroovyShell(contextClassLoader, baseScriptTextKey));
        } else {
            return groovyShell;
        }
    }

    private GroovyShell createGroovyShell(ClassLoader classLoader, String baseClassScript) {
        GroovyClassLoader groovyClassLoader = new GroovyClassLoader(classLoader);
        Class baseClass = groovyClassLoader.parseClass(baseClassScript);
        return createGroovyShell(groovyClassLoader, baseClass);
    }

    private GroovyShell createGroovyShell(ClassLoader classLoader, Class baseClass) {
        CompilerConfiguration compilerConfiguration = new CompilerConfiguration();
        compilerConfiguration.setScriptBaseClass(baseClass.getName());
        return new GroovyShell(classLoader, compilerConfiguration);
    }

}
