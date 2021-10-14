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

package com.tomtom.james.store.informationpoints.io

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper
import spock.lang.Specification

class InformationPointYamlDTOSpec extends Specification {

    def minimalYAML = '''
class-name-value!method-name-value:
'''
    def onlyYAML = '''
    script: |
        line1
        line2
    sampleRate: 70
'''

    def completeYAML = '''
class-name-value!method-name-value:
    script: |
        line1
        line2
    sampleRate: 70
'''
    def updatedYAML = '''
class-name-value!method-name-value:
    script: |
        line1
        line2
    successSampleRate: 0.5
    errorSampleRate: 100
'''

    def baseScriptYAML = '''
class-name-value!method-name-value:
    baseScript: 
        script: |
            baseline1
            baseline2
    script: |
        line1
        line2
'''

    def baseScriptWithReferenceYAML = '''
class-name-value!method-name-value:
    baseScript: &id001
        script: |
            baseline1
            baseline2
    script: |
        line1
        line2
class-name-value2!method-name-value:
    baseScript: *id001
    script: &sId002 |
        line3
        line4
class-name-value3!method-name-value:
    baseScript: *id001
    script: *sId002
'''

    def objectMapper = new ObjectMapper(new YAMLMapper()).findAndRegisterModules();

    def type = objectMapper.getTypeFactory().constructMapType(Map, String, InformationPointYamlDTO)

    def "Should parse Only yaml to DTO"() {
        when:
        Collection<InformationPointYamlDTO> ipsDTOs = objectMapper.readValue(minimalYAML, type).entrySet()
                .stream()
                .map(entry ->
                        Optional.ofNullable(entry.getValue()).orElse(new InformationPointYamlDTO()).withMethodReference(entry.getKey()))
                .collect(java.util.stream.Collectors.toList())
        def ip = ipsDTOs[0].toInformationPoint()

        then:
        ip.className == "class-name-value"
        ip.methodName == "method-name-value"
        !ip.script.isPresent()
        ip.sampleRate == 100
    }

    def "Should parse minimal YAML to DTO"() {
        when:
        Collection<InformationPointYamlDTO> ipsDTOs = objectMapper.readValue(minimalYAML, type).entrySet()
                .stream()
                .map(entry ->
                        Optional.ofNullable(entry.getValue()).orElse(new InformationPointYamlDTO()).withMethodReference(entry.getKey()))
                .collect(java.util.stream.Collectors.toList())
        def ip = ipsDTOs[0].toInformationPoint()

        then:
        ip.className == "class-name-value"
        ip.methodName == "method-name-value"
        !ip.script.isPresent()
        ip.sampleRate == 100
    }

    def "Should parse complete YAML to DTO"() {
        when:
        Collection<InformationPointYamlDTO> ipsDTOs = objectMapper.readValue(completeYAML, type).entrySet()
                .stream()
                .map(entry ->
                        Optional.ofNullable(entry.value).orElse(new InformationPointYamlDTO()).withMethodReference(entry.getKey()))
                .collect(java.util.stream.Collectors.toList())
        def ip = ipsDTOs[0].toInformationPoint()

        then:
        ip.className == "class-name-value"
        ip.methodName == "method-name-value"
        ip.script.get() == "line1\nline2"
        ip.sampleRate == 70
    }

    def "Should parse update YAML to DTO (with new sample rates)"() {
        when:
        Collection<InformationPointYamlDTO> ipsDTOs = objectMapper.readValue(updatedYAML, type).entrySet()
                .stream()
                .map(entry ->
                        Optional.ofNullable(entry.value).orElse(new InformationPointYamlDTO()).withMethodReference(entry.getKey()))
                .collect(java.util.stream.Collectors.toList())
        def ip = ipsDTOs[0].toInformationPoint()

        then:
        ip.className == "class-name-value"
        ip.methodName == "method-name-value"
        ip.script.get() == "line1\nline2"
        ip.successSampleRate == 0.5
        ip.errorSampleRate == 100
        ip.sampleRate == 100
    }

    def "Should parse YAML with base script"() {
        when:
        Collection<InformationPointYamlDTO> ipsDTOs = objectMapper.readValue(baseScriptYAML, type).entrySet()
                .stream()
                .map(entry ->
                        Optional.ofNullable(entry.value).orElse(new InformationPointYamlDTO()).withMethodReference(entry.getKey()))
                .collect(java.util.stream.Collectors.toList())
        def ip = ipsDTOs[0].toInformationPoint()

        then:
        ip.className == "class-name-value"
        ip.methodName == "method-name-value"
        ip.baseScript.get() == "baseline1\nbaseline2"
        ip.script.get() == "line1\nline2"
    }


    def "Should parse YAML with successExecutionThreshold"() {
        when:
        Collection<InformationPointYamlDTO> ipsDTOs = objectMapper.readValue("""
class-name-value!method-name-value:
    successExecutionThreshold: 99
""", type).entrySet()
                .stream()
                .map(entry ->
                Optional.ofNullable(entry.value).orElse(new InformationPointYamlDTO()).withMethodReference(entry.getKey()))
                .collect(java.util.stream.Collectors.toList())
        def ip = ipsDTOs[0].toInformationPoint()

        then:
        ip.className == "class-name-value"
        ip.methodName == "method-name-value"
        ip.successExecutionThreshold == 99
    }

    def "Should parse yaml with reference to base script to DTO"() {
        when:
        Collection<InformationPointYamlDTO> ipsDTOs = objectMapper.readValue(baseScriptWithReferenceYAML, type).entrySet()
                .stream()
                .map(entry ->
                        Optional.ofNullable(entry.getValue()).orElse(new InformationPointYamlDTO()).withMethodReference(entry.getKey()))
                .collect(java.util.stream.Collectors.toList())
        def ip = ipsDTOs[0].toInformationPoint()
        def ip1 = ipsDTOs[1].toInformationPoint()
        def ip2 = ipsDTOs[2].toInformationPoint()

        then:
        ip.getBaseScript() == ip2.getBaseScript()
        ip1.getScript() != ip2.getScript() //not able to reference on simple types
        ip.getScript() != ip1.getScript()
    }
}
