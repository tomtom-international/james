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

import com.tomtom.james.common.api.script.RuntimeInformationPointParameter;

import java.lang.reflect.Method;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

public abstract class InformationPointHandlerContext extends InformationPointContext {

    final Instant eventTime;
    final Duration executionTime;
    final String[] callStack;
    final Object initialContext;

    InformationPointHandlerContext(String informationPointClassName,
                                   String informationPointMethodName,
                                   Method origin,
                                   List<RuntimeInformationPointParameter> runtimeParameters,
                                   Object runtimeInstance,
                                   Thread currentThread,
                                   Instant eventTime,
                                   Duration executionTime,
                                   String[] callStack,
                                   Object initialContext) {
        super(informationPointClassName, informationPointMethodName, origin, runtimeInstance, runtimeParameters, currentThread);
        this.eventTime = eventTime;
        this.executionTime = executionTime;
        this.callStack = callStack;
        this.initialContext = initialContext;
    }

    @SuppressWarnings("unused")
    public Instant getEventTime() {
        return eventTime;
    }

    @SuppressWarnings("unused")
    public Duration getExecutionTime() {
        return executionTime;
    }

    @SuppressWarnings("unused")
    public String[] getCallStack() {
        return callStack;
    }

    @SuppressWarnings("unused")
    public Object getInitialContext() {
        return initialContext;
    }
}
