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

import com.tomtom.james.common.api.script.RuntimeInformationPointParameter
import com.tomtom.james.common.api.script.ScriptEngine
import spock.lang.Specification

import java.lang.reflect.Method
import java.time.Duration

class ContextAwareAdviceSpec extends Specification {

    def scriptEngine = Mock(ScriptEngine)
    def originClassName = "originClassName"
    def originMethodName = "originMethodName"
    def informationPointClassName = "informationPointClassName"
    def informationPointMethodName = "informationPointClassName"
    def script = "// script"
    def sampleRate = 100

    def setup() {
        ScriptEngineSupplier.register(scriptEngine)
    }

    def "Should call success handler after successful method execution"() {
        given:
        def method = Object.class.getMethod("equals", Object.class)
        def currentThread = Thread.currentThread();

        when:
        def stopwatch = ContextAwareAdvice.onEnter(originClassName, originMethodName)
        ContextAwareAdvice.onExit(stopwatch, informationPointClassName, informationPointMethodName, script, sampleRate,
                method, new Object(), ["arg0"] as Object[], "returned", null)

        then:
        1 * scriptEngine.invokeSuccessHandler(
                informationPointClassName,
                informationPointMethodName,
                script,
                _ as Method,
                _ as List<RuntimeInformationPointParameter>,
                _ as Object,
                currentThread,
                _ as Duration,
                _ as String[],
                "returned"
        )
    }

    def "Should call error handler after method throws exception"() {
        given:
        def method = Object.class.getMethod("equals", Object.class)
        def thrown = new RuntimeException("message")
        def currentThread = Thread.currentThread()

        when:
        def stopwatch = ContextAwareAdvice.onEnter(originClassName, originMethodName)
        ContextAwareAdvice.onExit(stopwatch, informationPointClassName, informationPointMethodName, script, sampleRate,
                method, new Object(), ["arg0"] as Object[], null, thrown)

        then:
        1 * scriptEngine.invokeErrorHandler(
                informationPointClassName,
                informationPointMethodName,
                script,
                _ as Method,
                _ as List<RuntimeInformationPointParameter>,
                _ as Object,
                currentThread,
                _ as Duration,
                _ as String[],
                thrown
        )
    }

}
