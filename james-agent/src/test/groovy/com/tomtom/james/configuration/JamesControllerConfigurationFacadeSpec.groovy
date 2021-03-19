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

class JamesControllerConfigurationFacadeSpec extends Specification {

    def configuration = """
controllers:
  - id: james.controller.webservice
    properties:
      port: 7007
      minThreads: 1
      maxThreads: 8
  - id: james.controller.consul
    properties:
      host: \${TEST_CONSUL_HOST:-localhost}
      port: \${TEST_CONSUL_PORT:-8500}
      folderPath: james/test/information-points
"""

    def "Should correctly parse controller configuration section"() {
        given:
        def agentConfigurationFacade = new AgentConfigurationFacade(new YAMLConfiguration(configuration))

        when:
        def controllersConfigurations = agentConfigurationFacade.controllersConfigurations

        then:
        def conf1 = controllersConfigurations[0]
        def conf2 = controllersConfigurations[1]

        conf1.id == "james.controller.webservice"
        conf2.id == "james.controller.consul"
        conf2.properties.get().get("host").get().asString() == "localhost"
        conf2.properties.get().get("port").get().asInteger() == 8500
    }

}
