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

class InformationPointStoreConfigurationFacadeSpec extends Specification {

    def configuration = """
informationPointStore:
  persistenceEnabled: true
  storeFilePath: /tmp/file/informationpoints.json
"""

    def "Should correctly parse information point store configuration section"() {
        given:
        def agentConfigurationFacade = new AgentConfigurationFacade(new YAMLConfiguration(configuration))

        when:
        def ipStoreConfiguration = agentConfigurationFacade.informationPointStoreConfiguration

        then:
        ipStoreConfiguration.persistenceEnabled
        ipStoreConfiguration.storeFilePath == "/tmp/file/informationpoints.json"
    }
}