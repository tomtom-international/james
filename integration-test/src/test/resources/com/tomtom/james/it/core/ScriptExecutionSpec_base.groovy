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

package com.tomtom.james.it.core

import com.tomtom.james.common.api.publisher.Event
import com.tomtom.james.script.ErrorHandlerContext
import com.tomtom.james.script.InformationPointHandler
import com.tomtom.james.script.InformationPointHandlerContext

abstract class CustomInformationPointHandler extends InformationPointHandler {

    void publishEvent(Event evt) {
        evt.withEntry("custom", "value");
        super.publishEvent(evt);
    }

    Event createEvent(Map<String, Object> content, InformationPointHandlerContext context) {
        content << [
                result     : context instanceof ErrorHandlerContext ? "error" : "success",
                className  : context.informationPointClassName,
                methodName : context.informationPointMethodName,
        ]
        context.parameters.each {
            content["arg(${it.name})"] = it.value
        }
        return super.createEvent(content, context)
    }
}