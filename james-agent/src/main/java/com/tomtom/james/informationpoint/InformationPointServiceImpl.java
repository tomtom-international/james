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

package com.tomtom.james.informationpoint;

import com.google.common.annotations.VisibleForTesting;
import com.tomtom.james.common.api.informationpoint.InformationPoint;
import com.tomtom.james.common.api.informationpoint.InformationPointService;
import com.tomtom.james.common.log.Logger;
import com.tomtom.james.store.InformationPointStore;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.utility.JavaModule;

import java.lang.instrument.Instrumentation;
import java.util.*;

public class InformationPointServiceImpl implements InformationPointService {

    private static final Logger LOG = Logger.getLogger(InformationPointServiceImpl.class);

    private final InformationPointStore store;
    private final AdviceOperations adviceOperations;
    private final List<InformationPoint> informationPoints;

    public InformationPointServiceImpl(InformationPointStore store,
                                       Instrumentation instrumentation) {
        this(store, instrumentation, new ByteBuddyAdviceOperations(createAgentBuilder(), instrumentation));
    }

    @VisibleForTesting
    InformationPointServiceImpl(InformationPointStore store,
                                Instrumentation instrumentation,
                                AdviceOperations adviceOperations) {
        this.store = Objects.requireNonNull(store);
        this.adviceOperations = Objects.requireNonNull(adviceOperations);
        informationPoints = new ArrayList<>(store.restore());
        informationPoints.forEach(adviceOperations::installAdvice);
    }

    @Override
    public Collection<InformationPoint> getInformationPoints() {
        return Collections.unmodifiableCollection(informationPoints);
    }

    @Override
    public Optional<InformationPoint> getInformationPoint(String className, String methodName) {
        return informationPoints.stream()
                .filter(point -> point.getClassName().equals(className) && point.getMethodName().equals(methodName))
                .findFirst();
    }

    @Override
    public void addInformationPoint(InformationPoint informationPoint) {
        adviceOperations.installAdvice(informationPoint);
        informationPoints.add(informationPoint);
        store.store(informationPoints);
    }

    @Override
    public void removeInformationPoint(InformationPoint informationPoint) {
        adviceOperations.uninstallAdvice(informationPoint);
        informationPoints.remove(informationPoint);
        store.store(informationPoints);
    }

    private static AgentBuilder createAgentBuilder() {
        AgentBuilder builder = new AgentBuilder.Default()
                .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
                .with(AgentBuilder.InitializationStrategy.NoOp.INSTANCE)
                .with(AgentBuilder.TypeStrategy.Default.REDEFINE)
                .with(new LoggingTransformationListener());
        if (Boolean.parseBoolean(System.getProperty("james.verboseAgentBuilder", "false"))) {
            builder = builder.with(AgentBuilder.Listener.StreamWriting.toSystemOut());
        }
        return builder;
    }

    private static class LoggingTransformationListener extends AgentBuilder.Listener.Adapter {
        @Override
        public void onTransformation(TypeDescription typeDescription, ClassLoader classLoader, JavaModule module,
                                     boolean loaded, DynamicType dynamicType) {
            LOG.trace(() -> "Class transformation applied to " + typeDescription.getName());
        }

        @Override
        public void onError(String typeName, ClassLoader classLoader, JavaModule module, boolean loaded,
                            Throwable throwable) {
            if (throwable.getMessage().contains("com.singularity.ee.agent.appagent")) {
                LOG.debug(() -> "Unable to transform class " + typeName + " instrumented by AppDynamics");
            } else {
                LOG.warn(() -> "Class transformation of " + typeName + " failed", throwable);
            }
        }
    }
}
