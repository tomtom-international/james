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

package com.tomtom.james.configuration

import spock.lang.Specification

class EventPublisherConfigurationFacadeSpec extends Specification {

    def configuration = """
publishers:
  - id: james.publisher.console
    properties:
      prettifyJSON: true
  - id: james.publisher.file
    asyncWorkers: 16
    maxAsyncJobQueueCapacity: 20000
    properties:
      path: ./filepublisher.out
"""

    def "Should correctly parse publishers configuration section"() {
        given:
        def agentConfigurationFacade = new AgentConfigurationFacade(new YAMLConfiguration(configuration))

        when:
        def publishersConfigurations = agentConfigurationFacade.publishersConfigurations

        then:
        def conf1 = publishersConfigurations[0]
        def conf2 = publishersConfigurations[1]

        conf1.id == "james.publisher.console"
        conf2.id == "james.publisher.file"
        conf2.asyncWorkers == 16
        conf2.maxAsyncJobQueueCapacity == 20_000
    }
}