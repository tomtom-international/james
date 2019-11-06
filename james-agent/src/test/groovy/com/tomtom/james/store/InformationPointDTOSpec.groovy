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

package com.tomtom.james.store

import com.fasterxml.jackson.databind.ObjectMapper
import spock.lang.Specification

class InformationPointDTOSpec extends Specification {

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
    "successSampleRate": 50,
    "errorSampleRate": 100
}]
'''
    def updatedJsonIdenticalSampleRates = '''
[{
    "className": "class-name-value",
    "methodName": "method-name-value",
    "script": ["line1", "line2"],
    "sampleRate": 70,
    "successSampleRate": 70,
    "errorSampleRate": 70
}]
'''
    def invalidJson = '''
[{
    "className": "class-name-value",
    "methodName": "method-name-value",
    "script": ["line1", "line2"],
    "sampleRate": 100,
    "successSampleRate": 50,
    "errorSampleRate": 40
}]
'''

    def objectMapper = new ObjectMapper();
    def type = objectMapper.getTypeFactory().constructCollectionType(Collection, InformationPointDTO)

    def "Should parse minimal JSON to DTO"() {
        when:
        Collection<InformationPointDTO> ipsDTOs = objectMapper.readValue(minimalJSON, type)
        def ip = ipsDTOs[0].toInformationPoint()

        then:
        ip.className == "class-name-value"
        ip.methodName == "method-name-value"
        !ip.script.isPresent()
        ip.sampleRate == 100
    }

    def "Should parse complete JSON to DTO"() {
        when:
        Collection<InformationPointDTO> ipsDTOs = objectMapper.readValue(completeJSON, type)
        def ip = ipsDTOs[0].toInformationPoint()

        then:
        ip.className == "class-name-value"
        ip.methodName == "method-name-value"
        ip.script.get() == "line1\nline2"
        ip.sampleRate == 70
    }

    def "Should parse update JSON to DTO (with new sample rates)"() {
        when:
        Collection<InformationPointDTO> ipsDTOs = objectMapper.readValue(updatedJson, type)
        def ip = ipsDTOs[0].toInformationPoint()

        then:
        ip.className == "class-name-value"
        ip.methodName == "method-name-value"
        ip.script.get() == "line1\nline2"
        ip.successSampleRate == 50
        ip.errorSampleRate == 100
        ip.sampleRate == 100
    }

    def "Should parse updated JSON to DTO with all sample rates identical for backward compatibility"() {
        when:
        Collection<InformationPointDTO> ipsDTOs = objectMapper.readValue(updatedJsonIdenticalSampleRates, type)
        def ip = ipsDTOs[0].toInformationPoint()

        then:
        ip.className == "class-name-value"
        ip.methodName == "method-name-value"
        ip.script.get() == "line1\nline2"
        ip.successSampleRate == 70
        ip.errorSampleRate == 70
        ip.sampleRate == 70
    }

    def "Should error out on parsing invalid JSON to DTO"() {
        when:
        Collection<InformationPointDTO> ipsDTOs = objectMapper.readValue(invalidJson, type)
        def ip = ipsDTOs[0].toInformationPoint()

        then:
        thrown(IllegalStateException)
    }
}
