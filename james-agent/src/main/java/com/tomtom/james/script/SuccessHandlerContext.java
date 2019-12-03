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

import com.google.common.base.MoreObjects;
import com.tomtom.james.common.api.script.RuntimeInformationPointParameter;

import java.lang.reflect.Method;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

public final class SuccessHandlerContext extends InformationPointHandlerContext {

    private final Object returnValue;

    SuccessHandlerContext(String informationPointClassName,
                          String informationPointMethodName,
                          Method origin,
                          List<RuntimeInformationPointParameter> runtimeParameters,
                          Object runtimeInstance,
                          Thread currentThread,
                          Instant eventTime,
                          Duration executionTime,
                          String[] callStack,
                          Object returnValue,
                          Object initialContext) {
        super(informationPointClassName, informationPointMethodName, origin, runtimeParameters,
                runtimeInstance, currentThread, eventTime, executionTime, callStack, initialContext);
        this.returnValue = returnValue;
    }

    @SuppressWarnings("unused")
    public Object getReturnValue() {
        return returnValue;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("informationPointClassName", informationPointClassName)
                .add("informationPointMethodName", informationPointMethodName)
                .add("origin", origin)
                .add("runtimeInstance", runtimeInstance)
                .add("runtimeParameters", runtimeParameters)
                .add("currentThread", currentThread)
                .add("eventTime", eventTime)
                .add("executionTime", executionTime)
                .add("callStack", callStack)
                .add("returnValue", returnValue)
                .toString();
    }
}
