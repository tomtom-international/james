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
import spock.lang.Specification

class InformationPointJsonDTOSpec extends Specification {

    def minimalJSON = '''
[{
    "className": "class-name-value",
    "methodName": "method-name-value"
}]
'''

    def completeJSON = '''
[{
    "className": "class-name-value",
    "methodName": "method-name-value",
    "script": ["line1", "line2"],
    "sampleRate": 70
}]

'''
    def updatedJson = '''
[{
    "className": "class-name-value",
    "methodName": "method-name-value",
    "script": ["line1", "line2"],
    "successSampleRate": 0.5,
    "errorSampleRate": 100
}]
'''

    def baseScriptJson = '''
[{
    "className": "class-name-value",
    "methodName": "method-name-value",
    "baseScript": ["baseline1", "baseline2"],
    "script": ["line1", "line2"]
}]
'''

    def objectMapper = new ObjectMapper();
    def type = objectMapper.getTypeFactory().constructCollectionType(Collection, InformationPointJsonDTO)

    def "Should parse minimal JSON to DTO"() {
        when:
        Collection<InformationPointJsonDTO> ipsDTOs = objectMapper.readValue(minimalJSON, type)
        def ip = ipsDTOs[0].toInformationPoint()

        then:
        ip.className == "class-name-value"
        ip.methodName == "method-name-value"
        !ip.script.isPresent()
        ip.sampleRate == 100
    }

    def "Should parse complete JSON to DTO"() {
        when:
        Collection<InformationPointJsonDTO> ipsDTOs = objectMapper.readValue(completeJSON, type)
        def ip = ipsDTOs[0].toInformationPoint()

        then:
        ip.className == "class-name-value"
        ip.methodName == "method-name-value"
        ip.script.get() == "line1\nline2"
        ip.sampleRate == 70
    }

    def "Should parse update JSON to DTO (with new sample rates)"() {
        when:
        Collection<InformationPointJsonDTO> ipsDTOs = objectMapper.readValue(updatedJson, type)
        def ip = ipsDTOs[0].toInformationPoint()

        then:
        ip.className == "class-name-value"
        ip.methodName == "method-name-value"
        ip.script.get() == "line1\nline2"
        ip.successSampleRate == 0.5
        ip.errorSampleRate == 100
        ip.sampleRate == 100
    }

    def "Should parse JSON with base script"() {
        when:
        Collection<InformationPointJsonDTO> ipsDTOs = objectMapper.readValue(baseScriptJson, type)
        def ip = ipsDTOs[0].toInformationPoint()

        then:
        ip.className == "class-name-value"
        ip.methodName == "method-name-value"
        ip.baseScript.get() == "baseline1\nbaseline2"
        ip.script.get() == "line1\nline2"
    }


    def "Should parse JSON with successExecutionThreshold"() {
        when:
        Collection<InformationPointJsonDTO> ipsDTOs = objectMapper.readValue("""
[{
    "className": "class-name-value",
    "methodName": "method-name-value",
    "successExecutionThreshold" : 99
}]
""", type)
        def ip = ipsDTOs[0].toInformationPoint()

        then:
        ip.className == "class-name-value"
        ip.methodName == "method-name-value"
        ip.successExecutionThreshold == 99
    }
}
