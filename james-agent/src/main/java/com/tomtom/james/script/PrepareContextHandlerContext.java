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
import java.util.List;

public class PrepareContextHandlerContext extends InformationPointContext {

    final String key;

    public PrepareContextHandlerContext(
            String informationPointClassName,
            String informationPointMethodName,
            Method origin,
            Object runtimeInstance,
            List<RuntimeInformationPointParameter> runtimeParameters,
            Thread currentThread,
            String key) {

        super(  informationPointClassName,
                informationPointMethodName,
                origin,
                runtimeInstance,
                runtimeParameters,
                currentThread);
        this.key = key;
    }


    @SuppressWarnings("unused")
    public String getKey() {
        return key;
    }
}
