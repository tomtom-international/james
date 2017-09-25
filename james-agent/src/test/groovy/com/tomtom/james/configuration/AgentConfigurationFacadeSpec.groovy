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

import com.tomtom.james.common.log.Logger
import spock.lang.Specification

class AgentConfigurationFacadeSpec extends Specification {

    def configuration = """
quiet: true
logLevel: trace

plugins:
  includeDirectories:
    - ./pluginDirectory1
    - ./pluginDirectory2
  includeFiles:
    - ./plugin1.jar
    - ./plugin2.jar
"""

    def "Should correctly parse the top level of configuration"() {
        given:
        def facade = new AgentConfigurationFacade(new YAMLConfiguration(configuration))

        when:
        def quiet = facade.quiet
        def logLevel = facade.logLevel
        def pluginIncludeDirectories = facade.pluginIncludeDirectories
        def pluginIncludeFiles = facade.pluginIncludeFiles

        then:
        quiet
        logLevel == Logger.Level.TRACE
        pluginIncludeDirectories == ["./pluginDirectory1", "./pluginDirectory2"]
        pluginIncludeFiles == ["./plugin1.jar", "./plugin2.jar"]
    }
}
