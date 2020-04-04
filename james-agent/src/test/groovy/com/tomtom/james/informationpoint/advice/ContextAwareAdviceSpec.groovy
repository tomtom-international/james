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

package com.tomtom.james.informationpoint.advice

import com.tomtom.james.common.api.informationpoint.InformationPoint
import com.tomtom.james.common.api.informationpoint.InformationPointService
import com.tomtom.james.common.api.script.RuntimeInformationPointParameter
import com.tomtom.james.common.api.script.ScriptEngine
import com.tomtom.james.common.log.Logger
import spock.lang.Specification

import java.lang.reflect.Method
import java.time.Duration
import java.time.Instant
import java.util.concurrent.CompletableFuture

class ContextAwareAdviceSpec extends Specification {

    def scriptEngine = Mock(ScriptEngine)
    def ipService = Mock(InformationPointService)
    def originClassName = "originClassName"
    def originMethodName = "originMethodName"
    def informationPointClassName = "informationPointClassName"
    def informationPointMethodName = "informationPointClassName"
    def informationPointMethodNameWithContext = "informationPointClassNameWithContext"
    def script = "// script"
    def sampleRate = 100
    def informationPoint = Mock(InformationPoint)
    def contextAwareInformationPoint = Mock(InformationPoint)

    def setup() {
        Logger.setCurrentLogLevel(Logger.Level.TRACE)
        ScriptEngineSupplier.register(scriptEngine)
        InformationPointServiceSupplier.register(ipService)
        ipService.getInformationPoint(informationPointClassName, informationPointMethodName) >> Optional.of(informationPoint)
        ipService.getInformationPoint(informationPointClassName, informationPointMethodNameWithContext) >> Optional.of(contextAwareInformationPoint)
        informationPoint.getSuccessSampleRate() >> sampleRate
        informationPoint.getErrorSampleRate() >> sampleRate
        informationPoint.getSuccessExecutionThreshold() >> -1
        informationPoint.getRequiresInitialContext() >> Boolean.FALSE
        informationPoint.getBaseScript() >> Optional.empty()
        informationPoint.getScript() >> Optional.of("script")
        contextAwareInformationPoint.getRequiresInitialContext() >> Boolean.TRUE
        contextAwareInformationPoint.getSuccessSampleRate() >> sampleRate
        contextAwareInformationPoint.getErrorSampleRate() >> sampleRate
        contextAwareInformationPoint.getBaseScript() >> Optional.empty()
        contextAwareInformationPoint.getScript() >> Optional.of("script")
    }

    void cleanup() {
        Logger.setCurrentLogLevel(Logger.Level.WARN)
    }

    def "Should call success handler after successful method execution"() {
        given:
        def method = Object.class.getMethod("equals", Object.class)
        def currentThread = Thread.currentThread()

        when:
        def startTime = System.nanoTime()
        ContextAwareAdvice.onEnter(informationPointClassName, informationPointMethodName)
        ContextAwareAdvice.onExit(startTime, informationPointClassName, informationPointMethodName,
                method, new Object(), ["arg0"] as Object[], "returned", null)

        then:
        1 * scriptEngine.invokeSuccessHandler(
                informationPoint,
                _ as Method,
                _ as List<RuntimeInformationPointParameter>,
                _ as Object,
                currentThread,
                _ as Instant,
                _ as Duration,
                { Arrays.asList(it as String[]).contains(ContextAwareAdviceSpec.getName()) },
                "returned",
                _ as CompletableFuture<Object>
        )
    }

    def "Should call error handler after method throws exception"() {
        given:
        def method = Object.class.getMethod("equals", Object.class)
        def thrown = new RuntimeException("message")
        def currentThread = Thread.currentThread()

        when:
        ipService.getInformationPoint(informationPointClassName, informationPointMethodName) >> Optional.of(informationPoint)
        def startTime = System.nanoTime()
        ContextAwareAdvice.onEnter(informationPointClassName, informationPointMethodName)
        ContextAwareAdvice.onExit(startTime, informationPointClassName, informationPointMethodName,
                method, new Object(), ["arg0"] as Object[], null, thrown)

        then:
        1 * scriptEngine.invokeErrorHandler(
                informationPoint,
                _ as Method,
                _ as List<RuntimeInformationPointParameter>,
                _ as Object,
                currentThread,
                _ as Instant,
                _ as Duration,
                _ as String[],
                thrown,
                _ as CompletableFuture<Object>
        )
    }

    def "Should not call error handler after method throws exception when error sample rate to low"() {
        given:
        def method = Object.class.getMethod("equals", Object.class)
        def thrown = new RuntimeException("message")
        def currentThread = Thread.currentThread()

        when:
        ipService.getInformationPoint(informationPointClassName, informationPointMethodName) >> Optional.of(informationPoint)
        def startTime = System.nanoTime()
        ContextAwareAdvice.onEnter(informationPointClassName, informationPointMethodName)
        ContextAwareAdvice.onExit(startTime, informationPointClassName, informationPointMethodName,
                method, new Object(), ["arg0"] as Object[], null, thrown)

        then:
        (1.._) * informationPoint.getErrorSampleRate() >> 0
        0 * scriptEngine.invokeErrorHandler(
                informationPoint,
                _ as Method,
                _ as List<RuntimeInformationPointParameter>,
                _ as Object,
                currentThread,
                _ as Instant,
                _ as Duration,
                _ as String[],
                thrown,
                _ as CompletableFuture<Object>
        )
    }

    def "Should not call success handler after successful method execution when error sample rate to low"() {
        given:
        def method = Object.class.getMethod("equals", Object.class)
        def currentThread = Thread.currentThread()

        when:
        def startTime = System.nanoTime()
        ContextAwareAdvice.onEnter(informationPointClassName, informationPointMethodName)
        ContextAwareAdvice.onExit(startTime, informationPointClassName, informationPointMethodName,
                method, new Object(), ["arg0"] as Object[], "returned", null)

        then:
        (1.._) * informationPoint.getSuccessSampleRate() >> 0
        0 * scriptEngine.invokeSuccessHandler(
                informationPoint,
                _ as Method,
                _ as List<RuntimeInformationPointParameter>,
                _ as Object,
                currentThread,
                _ as Instant,
                _ as Duration,
                _ as String[],
                "returned",
                _ as CompletableFuture<Object>
        )
    }

    def "Should initialize context on scripts defining onPrepareContext method"() {
        given:
        def method = Object.class.getMethod("equals", Object.class)
        def currentThread = Thread.currentThread()

        when:
        def startTime = System.nanoTime()

        def target = new Object()
        ContextAwareAdvice.onEnter(informationPointClassName, informationPointMethodNameWithContext, method, target, ["arg0"])
        ContextAwareAdvice.onExit(startTime, informationPointClassName, informationPointMethodNameWithContext,
                method, target, ["arg0"] as Object[], "returned", null)

        then:
        1 * scriptEngine.invokePrepareContext(
                contextAwareInformationPoint,
                _ as Method,
                _ as List<RuntimeInformationPointParameter>,
                _ as Object,
                currentThread,
                _ as String)
        1 * scriptEngine.invokeSuccessHandler(
                contextAwareInformationPoint,
                _ as Method,
                _ as List<RuntimeInformationPointParameter>,
                _ as Object,
                currentThread,
                _ as Instant,
                _ as Duration,
                _ as String[],
                "returned",
                _ as CompletableFuture<Object>
        )
    }


    def "Should not call handler when threshold is not met"() {
        given:
        def method = Object.class.getMethod("equals", Object.class)
        def currentThread = Thread.currentThread()

        when:
        informationPoint.getSuccessExecutionThreshold() >> 10000
        def startTime = System.nanoTime()
        ContextAwareAdvice.onEnter(informationPointClassName, informationPointMethodName)
        ContextAwareAdvice.onExit(startTime, informationPointClassName, informationPointMethodName,
                method, new Object(), ["arg0"] as Object[], "returned", null)

        then:
        0 * scriptEngine.invokeSuccessHandler(
                informationPoint,
                _ as Method,
                _ as List<RuntimeInformationPointParameter>,
                _ as Object,
                currentThread,
                _ as Instant,
                _ as Duration,
                _ as String[],
                "returned",
                _ as CompletableFuture<Object>
        )
    }

    def "Should call handler when threshold is not met"() {
        given:
        def method = Object.class.getMethod("equals", Object.class)
        def currentThread = Thread.currentThread()

        when:
        def startTime = System.nanoTime()
        ContextAwareAdvice.onEnter(informationPointClassName, informationPointMethodName)
        ContextAwareAdvice.onExit(startTime, informationPointClassName, informationPointMethodName,
                method, new Object(), ["arg0"] as Object[], "returned", null)

        then:
        1 * scriptEngine.invokeSuccessHandler(
                informationPoint,
                _ as Method,
                _ as List<RuntimeInformationPointParameter>,
                _ as Object,
                currentThread,
                _ as Instant,
                _ as Duration,
                _ as String[],
                "returned",
                _ as CompletableFuture<Object>
        )
    }

}
