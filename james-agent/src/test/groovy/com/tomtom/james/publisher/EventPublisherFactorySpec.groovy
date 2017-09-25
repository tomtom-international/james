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

import com.tomtom.james.agent.PluginManager
import com.tomtom.james.common.api.configuration.ConfigurationStructureException
import com.tomtom.james.common.api.configuration.EventPublisherConfiguration
import com.tomtom.james.common.api.publisher.EventPublisher
import spock.lang.Specification

class EventPublisherFactorySpec extends Specification {

    def pluginManager = Mock(PluginManager)
    def config1 = Mock(EventPublisherConfiguration)
    def config2 = Mock(EventPublisherConfiguration)
    def publisher1 = Mock(EventPublisher)
    def publisher2 = Mock(EventPublisher)

    def setup() {
        config1.getAsyncWorkers() >> 4
        config1.getMaxAsyncJobQueueCapacity() >> 10_000
        config2.getAsyncWorkers() >> 4
        config2.getMaxAsyncJobQueueCapacity() >> 10_000
        pluginManager.createEventPublisherPluginInstance(config1) >> Optional.of(publisher1)
        pluginManager.createEventPublisherPluginInstance(config2) >> Optional.of(publisher2)
    }

    def "Should raise an exception when given no publishers in configuration"() {
        when:
        EventPublisherFactory.create(pluginManager, [])

        then:
        thrown(ConfigurationStructureException)
    }

    def "Should create async publisher when given single publisher configuration"() {
        when:
        def publisher = EventPublisherFactory.create(pluginManager, [config1])

        then:
        publisher instanceof AsyncPublisher
        (publisher as AsyncPublisher).delegate.is(publisher1)
    }


    def "Should create composite publisher when given multiple publisher configurations"() {
        when:
        def publisher = EventPublisherFactory.create(pluginManager, [config1, config2])

        then:
        publisher instanceof CompositePublisher
        (publisher as CompositePublisher).delegates.size() == 2
        (publisher as CompositePublisher).delegates.each { it instanceof AsyncPublisher }
        ((publisher as CompositePublisher).delegates[0] as AsyncPublisher).delegate.is(publisher1)
        ((publisher as CompositePublisher).delegates[1] as AsyncPublisher).delegate.is(publisher2)
    }
}
