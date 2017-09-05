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

import com.tomtom.james.common.api.configuration.JamesControllerConfiguration
import com.tomtom.james.common.api.controller.JamesController
import com.tomtom.james.common.api.informationpoint.InformationPointService
import com.tomtom.james.common.api.publisher.EventPublisher
import com.tomtom.james.common.api.script.ScriptEngine
import spock.lang.Specification

class ControllersManagerSpec extends Specification {

    def pluginManager = Mock(PluginManager)
    def conf1 = Mock(JamesControllerConfiguration)
    def conf2 = Mock(JamesControllerConfiguration)
    def configurations = [conf1, conf2]
    def informationPointService = Mock(InformationPointService)
    def scriptEngine = Mock(ScriptEngine)
    def eventPublisher = Mock(EventPublisher)
    def controller1 = Mock(JamesController)
    def controller2 = Mock(JamesController)

    void setup() {
        pluginManager.createControllerPluginInstance(conf1) >> Optional.of(controller1)
        pluginManager.createControllerPluginInstance(conf2) >> Optional.of(controller2)
    }

    def "Should create and initialize controllers using provided list of configurations"() {
        given:
        def controllersManager = new ControllersManager(pluginManager, configurations)

        when:
        controllersManager.initializeControllers(informationPointService, scriptEngine, eventPublisher)

        then:
        1 * controller1.initialize(conf1, informationPointService, scriptEngine, eventPublisher)
        1 * controller2.initialize(conf2, informationPointService, scriptEngine, eventPublisher)
    }

    def "Should close all initialized controllers"() {
        given:
        def controllersManager = new ControllersManager(pluginManager, configurations)
        controllersManager.initializeControllers(informationPointService, scriptEngine, eventPublisher)

        when:
        controllersManager.close()

        then:
        1 * controller1.close()
        1 * controller2.close()
    }
}
