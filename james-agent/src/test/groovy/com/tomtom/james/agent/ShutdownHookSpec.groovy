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

import com.tomtom.james.common.api.Closeable
import com.tomtom.james.common.api.publisher.EventPublisher
import com.tomtom.james.common.api.script.ScriptEngine
import com.tomtom.james.configuration.AgentConfiguration
import spock.lang.Specification

class ShutdownHookSpec extends Specification {

    def controllersManager = Mock(ControllersManager)
    def scriptEngine = Mock(ScriptEngine)
    def eventPublisher = Mock(EventPublisher)
    def agentConfiguration = Mock(AgentConfiguration)
    def methodExecutionHelper = Mock(Closeable)

    def shutdownHook = new ShutdownHook(controllersManager, scriptEngine, eventPublisher, agentConfiguration, methodExecutionHelper)

    def "Should close resources on execution"() {
        when:
        agentConfiguration.getShutdownDelay() >> 1
        shutdownHook.run()

        then:
        1 * controllersManager.close()
        1 * scriptEngine.close()
        1 * eventPublisher.close()
        1 * methodExecutionHelper.close()
    }
}
