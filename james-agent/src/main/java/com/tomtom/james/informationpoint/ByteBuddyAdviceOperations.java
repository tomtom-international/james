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

import com.google.common.base.Stopwatch;
import com.tomtom.james.common.api.informationpoint.InformationPoint;
import com.tomtom.james.common.log.Logger;
import com.tomtom.james.informationpoint.advice.ContextAwareAdvice;
import com.tomtom.james.informationpoint.annotations.InformationPointClassName;
import com.tomtom.james.informationpoint.annotations.InformationPointMethodName;
import com.tomtom.james.informationpoint.annotations.InformationPointSampleRate;
import com.tomtom.james.informationpoint.annotations.InformationPointScript;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.agent.builder.ResettableClassFileTransformer;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.asm.AsmVisitorWrapper;

import java.lang.instrument.Instrumentation;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static net.bytebuddy.matcher.ElementMatchers.hasSuperType;
import static net.bytebuddy.matcher.ElementMatchers.named;

class ByteBuddyAdviceOperations implements AdviceOperations {

    private static final Logger LOG = Logger.getLogger(ByteBuddyAdviceOperations.class);
    private final AgentBuilder agentBuilder;
    private final Instrumentation instrumentation;
    private final ConcurrentHashMap<Key, ResettableClassFileTransformer> installedInformationPoints =
            new ConcurrentHashMap<>();

    ByteBuddyAdviceOperations(AgentBuilder agentBuilder, Instrumentation instrumentation) {
        this.agentBuilder = Objects.requireNonNull(agentBuilder);
        this.instrumentation = Objects.requireNonNull(instrumentation);
    }

    @Override
    public void installAdvice(InformationPoint informationPoint) {
        installedInformationPoints.computeIfAbsent(new Key(informationPoint), key -> {
            Stopwatch stopwatch = Stopwatch.createStarted();
            ResettableClassFileTransformer transformer = agentBuilder
                    .type(named(key.getClassName()).or(hasSuperType(named(key.getClassName()))))
                    .transform((builder, typeDescription, classLoader, module) -> {
                        AsmVisitorWrapper visitorWrapper = Advice
                                .withCustomMapping()
                                .bind(InformationPointClassName.class, informationPoint.getClassName())
                                .bind(InformationPointMethodName.class, informationPoint.getMethodName())
                                .bind(InformationPointSampleRate.class, informationPoint.getSampleRate())
                                .bind(InformationPointScript.class, informationPoint.getScript().orElseThrow(() ->
                                        new IllegalStateException("Script is not defined for " + informationPoint)))
                                .to(ContextAwareAdvice.class)
                                .on(named(key.getMethodName()));
                        return builder.visit(visitorWrapper);
                    })
                    .installOn(instrumentation);
            stopwatch.stop();
            LOG.trace(() -> "Advice installed at " + key + " in " + stopwatch.elapsed());
            return transformer;
        });
    }

    @Override
    public void uninstallAdvice(InformationPoint informationPoint) {
        Key key = new Key(informationPoint);
        ResettableClassFileTransformer transformer = installedInformationPoints.remove(key);
        if (transformer != null) {
            Stopwatch stopwatch = Stopwatch.createStarted();
            boolean wasApplied = transformer.reset(instrumentation, AgentBuilder.RedefinitionStrategy.RETRANSFORMATION);
            stopwatch.stop();
            if (wasApplied) {
                LOG.trace(() -> "Advice uninstalled at " + key + " in " + stopwatch.elapsed());
            } else {
                LOG.warn(() -> "Attempt to uninstall advice at " + key + " failed (transformer reset was not applied");
            }
        }
    }

    private static class Key {

        private final String className;
        private final String methodName;

        Key(String className, String methodName) {
            this.className = className;
            this.methodName = methodName;
        }

        Key(InformationPoint ip) {
            this.className = ip.getClassName();
            this.methodName = ip.getMethodName();
        }

        public String getClassName() {
            return className;
        }

        public String getMethodName() {
            return methodName;
        }

        @Override
        public String toString() {
            return className + "#" + methodName;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Key key = (Key) o;
            return Objects.equals(className, key.className) &&
                    Objects.equals(methodName, key.methodName);
        }

        @Override
        public int hashCode() {
            return Objects.hash(className, methodName);
        }
    }
}
