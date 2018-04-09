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

package com.tomtom.james.publisher

import com.tomtom.james.common.api.publisher.Event
import com.tomtom.james.common.api.publisher.EventPublisher
import spock.lang.Specification

import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.TimeUnit

import static org.awaitility.Awaitility.await

class AsyncPublisherSpec extends Specification {

    def delegate = Mock(EventPublisher)
    def callerThreadNames = new ArrayBlockingQueue<String>()

    def setup() {
        delegate.publish(_) >> { evt -> callerThreadNames.add(Thread.currentThread().getName()) }
    }

    def "Should publish events via the delegate in the background"() {
        given:
        def publisher = new AsyncPublisher(delegate, "bgthread-%d", 5, 1000)

        when:
        10.times {
            publisher.publish(new Event(""))
        }
        await().atMost(1, TimeUnit.SECONDS).until { callerThreadNames.size() == 10 }
        publisher.close()

        then:
        callerThreadNames.size() == 10
        callerThreadNames.findAll({ it.contains("bgthread") }).size() == 10
    }

}
