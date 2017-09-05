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

class CompositePublisherSpec extends Specification {

    def delegate1 = Mock(EventPublisher)
    def delegate2 = Mock(EventPublisher)

    def "Should republish an event to all delegates"() {
        given:
        def publisher = new CompositePublisher([delegate1, delegate2])
        def event = new Event("content")

        when:
        publisher.publish(event)

        then:
        1 * delegate1.publish(event)
        1 * delegate2.publish(event)
    }
}
