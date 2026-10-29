/*
 * Copyright 2026 TomTom International B.V.
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

import com.google.common.collect.Iterables
import com.tomtom.james.common.api.publisher.EventPublisher
import spock.lang.Specification

class PluginClassLoaderSpec extends Specification {

    def agentClassLoader = PluginClassLoaderSpec.classLoader

    static URL locationOf(Class<?> type) {
        type.protectionDomain.codeSource.location
    }

    def "Should load a library from the plugin jar even when the agent's classpath has it"() {
        given:
        def loader = new PluginClassLoader(locationOf(Iterables), agentClassLoader)

        when:
        def loaded = loader.loadClass(Iterables.name)

        then:
        loaded.classLoader == loader
        loaded != Iterables
    }

    def "Should load a class the plugin jar lacks from the agent's classpath"() {
        given:
        def loader = new PluginClassLoader(locationOf(Iterables), agentClassLoader)

        expect:
        loader.loadClass(Specification.name) == Specification
    }

    def "Should share the James API with the agent"() {
        given:
        def loader = new PluginClassLoader(locationOf(EventPublisher), agentClassLoader)

        expect:
        loader.loadClass(EventPublisher.name) == EventPublisher
    }

    def "Should load a shared package from the plugin jar when the agent lacks it"() {
        given:
        def loader = new PluginClassLoader(locationOf(EventPublisher), ClassLoader.platformClassLoader)

        when:
        def loaded = loader.loadClass(EventPublisher.name)

        then:
        loaded.classLoader == loader
    }

    def "Should load JDK classes from the JDK"() {
        given:
        def loader = new PluginClassLoader(locationOf(Iterables), agentClassLoader)

        expect:
        loader.loadClass(String.name) == String
    }
}
