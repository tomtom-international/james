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

import com.tomtom.james.common.api.configuration.EventPublisherConfiguration
import com.tomtom.james.common.api.publisher.Event
import spock.lang.Specification

class ConsolePublisherSpec extends Specification {

    def out = Mock(PrintStream)
    def configuration = Mock(EventPublisherConfiguration)

    def setup() {
        configuration.getProperties() >> Optional.empty()
    }

    def "Should print JSON formatted event"() {
        given:
        ConsolePublisher publisher = new ConsolePublisher(out)
        publisher.initialize(configuration)

        when:
        publisher.publish(new Event("event body"))
        publisher.close()

        then:
        1 * out.println { it.contains('"content":{"message":"event body"}') }

        then:
        1 * out.flush()
    }
}
