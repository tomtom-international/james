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

package com.tomtom.james.newagent

import spock.lang.Specification

import java.util.concurrent.TimeUnit

import static org.awaitility.Awaitility.await

class MethodExecutionContextHelperSpec extends Specification {

    def "Should store context per thread"() {
        given:
        def keys = new String[2]

        when:
        def th1 = new Thread( { runOnThread(keys, 0) })
        def th2 = new Thread( { runOnThread(keys, 1) })
        th1.start()
        th2.start()

        await().atMost(5, TimeUnit.SECONDS).until({ !th1.isAlive() && !th2.isAlive() })

        def context_value1 = MethodExecutionContextHelper.getContextAsync(keys[0]).get()
        def context_value2 = MethodExecutionContextHelper.getContextAsync(keys[1]).get()
        then:
        Integer.valueOf(100) == context_value1
        Integer.valueOf(101) == context_value2
    }

    static def runOnThread(String[] storage, int index) {
        String key = MethodExecutionContextHelper.createContextKey()
        storage[index] = key
        MethodExecutionContextHelper.storeContextAsync(key, Integer.valueOf(100 + index))
        MethodExecutionContextHelper.removeContextKey()
    }
}
