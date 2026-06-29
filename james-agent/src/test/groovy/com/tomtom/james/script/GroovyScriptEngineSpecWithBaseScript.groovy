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

package com.tomtom.james.script

import com.tomtom.james.agent.ToolkitManager
import com.tomtom.james.common.api.informationpoint.InformationPoint
import com.tomtom.james.common.api.informationpoint.Metadata
import com.tomtom.james.common.api.publisher.Event
import com.tomtom.james.common.api.publisher.EventPublisher
import com.tomtom.james.common.api.script.RuntimeInformationPointParameter
import com.tomtom.james.common.log.Logger
import spock.lang.Specification

import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.concurrent.CompletableFuture

class GroovyScriptEngineSpecWithBaseScript extends Specification {

    def script = """
import com.tomtom.james.script.*

def onSuccess(SuccessHandlerContext context) {
    publishEvent(createEvent(context))
}

def onError(ErrorHandlerContext context) {
    publishEvent(createEvent(context))
}
"""

    def baseScript = """
import com.tomtom.james.common.api.publisher.Event;
import com.tomtom.james.script.*

abstract class CustomInformationPointHandler extends InformationPointHandler {

    def onPrepareContext(PrepareContextHandlerContext context) {
        return "Key:" + context.key
    }

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
            content["arg(\${it.name})"] = it.value
        }
        if (context.initialContext != null) {
            content['initialContext'] = context.initialContext
        }
        return super.createEvent(content, context);
    }
}
"""

    def eventPublisher = Mock(EventPublisher)
    def toolkitManager = Mock(ToolkitManager)

    def informationPointClassName = "informationPointClassName"
    def informationPointMethodName = "informationPointClassName"
    def instant = Instant.ofEpochSecond(1)
    def duration = Duration.of(1, ChronoUnit.SECONDS)
    def instance = Mock(Object)
    def param1 = Mock(RuntimeInformationPointParameter)
    def param2 = Mock(RuntimeInformationPointParameter)
    def returnValue = Mock(Object)
    def errorCause = Mock(Throwable)
    def callStack = null
    def origin = null
    def currentThread = Mock(Thread)
    def informationPoint = Mock(InformationPoint)

    def setup() {
        Logger.setCurrentLogLevel(Logger.Level.TRACE)

        param1.getName() >> "param1-name"
        param1.getValue() >> "param1-value"
        param2.getName() >> "param2-name"
        param2.getValue() >> "param2-value"

        informationPoint.getClassName() >> informationPointClassName
        informationPoint.getMethodName() >> informationPointMethodName
        informationPoint.getBaseScript() >> Optional.of(baseScript)
        informationPoint.getScript() >> Optional.of(script)
        informationPoint.getMetadata() >> new Metadata()
    }

    void cleanup() {
        Logger.setCurrentLogLevel(Logger.Level.WARN)
    }

    def "Should publish an event when success handler invoked"() {
        given:
        def engine = new GroovyScriptEngine(eventPublisher, toolkitManager)

        when:
        engine.invokeSuccessHandler(informationPoint, origin, [param1, param2],
                instance, currentThread, instant, duration, callStack, returnValue, CompletableFuture.completedFuture(null))

        then:
        1 * eventPublisher.publish({ Event evt ->
            evt.content == [
                    custom            : "value",
                    result            : "success",
                    className         : informationPointClassName,
                    methodName        : informationPointMethodName,
                    "arg(param1-name)": "param1-value",
                    "arg(param2-name)": "param2-value"
            ]
        })
    }

    def "Should publish an event when error handler invoked"() {
        given:
        def engine = new GroovyScriptEngine(eventPublisher, toolkitManager)

        when:
        engine.invokeErrorHandler(informationPoint, origin, [param1, param2],
                instance, currentThread, instant, duration, callStack, errorCause, CompletableFuture.completedFuture(null))

        then:
        1 * eventPublisher.publish({ Event evt ->
            evt.content == [
                    custom            : "value",
                    result            : "error",
                    className         : informationPointClassName,
                    methodName        : informationPointMethodName,
                    "arg(param1-name)": "param1-value",
                    "arg(param2-name)": "param2-value"
            ]
        })
    }

    def "Should pass metadata to event if present"() {
        given:
        def engine = new GroovyScriptEngine(eventPublisher, toolkitManager)
        def metadata = new Metadata()
        metadata.put("_key", "value")
        def informationPoint2 = Mock(InformationPoint)
        informationPoint2.getClassName() >> informationPointClassName
        informationPoint2.getMethodName() >> informationPointMethodName
        informationPoint2.getBaseScript() >> Optional.of(baseScript)
        informationPoint2.getScript() >> Optional.of(script)
        informationPoint2.getMetadata() >> metadata

        when:
        engine.invokeSuccessHandler(informationPoint2, origin, [param1, param2],
                instance, currentThread, instant, duration, callStack, returnValue, CompletableFuture.completedFuture(null))

        then:
        1 * eventPublisher.publish({ Event evt ->
            evt.content == [
                    custom            : "value",
                    result            : "success",
                    className         : informationPointClassName,
                    methodName        : informationPointMethodName,
                    "arg(param1-name)": "param1-value",
                    "arg(param2-name)": "param2-value",
                    "@metadata"       : metadata
            ]
        })
    }

    def "Should publish an event with context"() {
        given:
        def engine = new GroovyScriptEngine(eventPublisher, toolkitManager)

        when:
        def context = engine.invokePrepareContext(informationPoint, origin, [param1, param2], instance, currentThread, "my_key")
        engine.invokeSuccessHandler(informationPoint, origin, [param1, param2],
                instance, currentThread, instant, duration, callStack, returnValue, CompletableFuture.completedFuture(context))

        then:
        1 * eventPublisher.publish({ Event evt ->
            evt.content == [
                    custom            : "value",
                    result            : "success",
                    className         : informationPointClassName,
                    methodName        : informationPointMethodName,
                    initialContext    : "Key:my_key",
                    "arg(param1-name)": "param1-value",
                    "arg(param2-name)": "param2-value"
            ]
        })
    }

    def "Should use updated base script when base script changes for same script text"() {
        given:
        def baseScriptV1 = """
import com.tomtom.james.common.api.publisher.Event
import com.tomtom.james.script.*

abstract class HandlerV1 extends InformationPointHandler {
    void publishEvent(Event evt) {
        evt.withEntry("baseVersion", "v1")
        super.publishEvent(evt)
    }
}
"""
        def baseScriptV2 = """
import com.tomtom.james.common.api.publisher.Event
import com.tomtom.james.script.*

abstract class HandlerV2 extends InformationPointHandler {
    void publishEvent(Event evt) {
        evt.withEntry("baseVersion", "v2")
        super.publishEvent(evt)
    }
}
"""
        def sharedScript = """
import com.tomtom.james.script.*
import com.tomtom.james.common.api.publisher.Event

def onSuccess(SuccessHandlerContext context) {
    publishEvent(new Event([:]))
}
"""
        def ipV1 = Mock(InformationPoint)
        ipV1.getClassName() >> informationPointClassName
        ipV1.getMethodName() >> informationPointMethodName
        ipV1.getBaseScript() >> Optional.of(baseScriptV1)
        ipV1.getScript() >> Optional.of(sharedScript)
        ipV1.getMetadata() >> new Metadata()

        def ipV2 = Mock(InformationPoint)
        ipV2.getClassName() >> informationPointClassName
        ipV2.getMethodName() >> informationPointMethodName
        ipV2.getBaseScript() >> Optional.of(baseScriptV2)
        ipV2.getScript() >> Optional.of(sharedScript)
        ipV2.getMetadata() >> new Metadata()

        def engine = new GroovyScriptEngine(eventPublisher, toolkitManager)

        when:
        engine.invokeSuccessHandler(ipV1, origin, [], instance, currentThread, instant, duration, callStack, returnValue, CompletableFuture.completedFuture(null))
        engine.invokeSuccessHandler(ipV2, origin, [], instance, currentThread, instant, duration, callStack, returnValue, CompletableFuture.completedFuture(null))

        then:
        1 * eventPublisher.publish({ Event evt -> evt.content["baseVersion"] == "v1" })
        1 * eventPublisher.publish({ Event evt -> evt.content["baseVersion"] == "v2" })
    }

}
