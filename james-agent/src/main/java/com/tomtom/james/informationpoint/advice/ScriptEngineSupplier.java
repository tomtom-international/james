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

import com.tomtom.james.common.api.script.ScriptEngine;

/*
 * Note: advices are inlined so this has to be public.
 */
public class ScriptEngineSupplier {

    private static ScriptEngine registeredEngine;

    public static ScriptEngine get() {
        if (registeredEngine != null) {
            return registeredEngine;
        } else {
            throw new IllegalStateException("ScriptEngine not registered");
        }
    }

    public static void register(ScriptEngine scriptEngine) {
        registeredEngine = scriptEngine;
    }
}
