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

package com.tomtom.james.agent

import com.tomtom.james.common.api.configuration.ToolkitConfiguration
import com.tomtom.james.common.api.toolkit.Toolkit
import spock.lang.Specification

class ToolkitManagerSpec extends Specification {

    def pluginManager = Mock(PluginManager)
    def conf1 = Mock(ToolkitConfiguration)
    def conf2 = Mock(ToolkitConfiguration)
    def configurations = [conf1, conf2]
    def toolkit1 = Mock(Toolkit)
    def toolkit2 = Mock(Toolkit)

    def setup() {
        toolkit1.id >> "james.test.toolkit1"
        toolkit2.id >> "james.test.toolkit2"
        pluginManager.createToolkitPluginInstance(conf1) >> Optional.of(toolkit1)
        pluginManager.createToolkitPluginInstance(conf2) >> Optional.of(toolkit2)
    }

    def "Should find correct, initialized toolkit by id"() {
        when:
        def toolkitManager = new ToolkitManager(pluginManager, configurations)
        def toolkitOptional = toolkitManager.getToolkit("james.test.toolkit1")

        then:
        1 * toolkit1.initialize(conf1)
        1 * toolkit2.initialize(conf2)
        toolkitOptional.isPresent()
        toolkitOptional.get().is(toolkit1)
    }
}
