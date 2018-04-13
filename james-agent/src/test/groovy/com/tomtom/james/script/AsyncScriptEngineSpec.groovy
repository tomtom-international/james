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

import com.tomtom.james.common.api.script.RuntimeInformationPointParameter
import com.tomtom.james.common.api.script.ScriptEngine
import spock.lang.Specification

import java.time.Duration
import java.time.temporal.ChronoUnit
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.TimeUnit

import static org.awaitility.Awaitility.await

class AsyncScriptEngineSpec extends Specification {

    def delegate = Mock(ScriptEngine)
    def informationPointClassName = "informationPointClassName"
    def informationPointMethodName = "informationPointClassName"
    def script = "// script"
    def duration = Duration.of(1, ChronoUnit.SECONDS)
    def instance = Mock(Object)
    def param1 = Mock(RuntimeInformationPointParameter)
    def param2 = Mock(RuntimeInformationPointParameter)
    def returnValue = Mock(Object)
    def errorCause = Mock(Throwable)
    def callStack = null
    def origin = null
    def currentThread = Mock(Thread)

    def successCallerThreadNames = new ArrayBlockingQueue<String>(10000)
    def errorCallerThreadNames = new ArrayBlockingQueue<String>(10000)

    def setup() {
        delegate.invokeSuccessHandler(*_) >> { successCallerThreadNames.add(Thread.currentThread().getName()) }
        delegate.invokeErrorHandler(*_) >> { errorCallerThreadNames.add(Thread.currentThread().getName()) }
    }

    def "Should invoke success handler via the delegate in the background"() {
        given:
        def scriptEngine = new AsyncScriptEngine(delegate, 5, 100)

        when:
        10.times {
            scriptEngine.invokeSuccessHandler(informationPointClassName, informationPointMethodName, script, origin,
                    [param1, param2], instance, currentThread, duration, callStack, returnValue)
        }
        await().atMost(5, TimeUnit.SECONDS).until { successCallerThreadNames.size() == 10 }

        then:
        successCallerThreadNames.size() == 10
        successCallerThreadNames.findAll({ it.contains("async-script-engine-thread-pool") }).size() == 10
    }

    def "Should invoke error handler via the delegate in the background"() {
        given:
        def scriptEngine = new AsyncScriptEngine(delegate, 5, 100)

        when:
        10.times {
            scriptEngine.invokeErrorHandler(informationPointClassName, informationPointMethodName, script, origin,
                    [param1, param2], instance, currentThread, duration, callStack, errorCause)
        }
        await().atMost(5, TimeUnit.SECONDS).until { errorCallerThreadNames.size() == 10 }

        then:
        errorCallerThreadNames.size() == 10
        errorCallerThreadNames.findAll({ it.contains("async-script-engine-thread-pool") }).size() == 10
    }

}
