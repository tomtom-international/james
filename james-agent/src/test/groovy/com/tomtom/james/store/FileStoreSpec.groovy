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

import com.tomtom.james.common.api.informationpoint.InformationPoint
import com.tomtom.james.common.api.informationpoint.Metadata
import spock.lang.Specification
import java.nio.file.Files

class FileStoreSpec extends Specification {

    def "Should store and restore instrumentation point"() {
        setup:
        def fileName = Files.createTempFile("spock", "FileStoreSpec.json")
                .toAbsolutePath().toString()
        when:
        def meta = new Metadata()
        meta['key'] = 'value'
        def simpleIP = InformationPoint.builder()
                .withClassName("clazz")
                .withMethodName("simple")
                .withBaseScript("base")
                .withScript("script")
                .withSuccessSampleRate(12)
                .withErrorSampleRate(10)
                .withMetadata(meta)
                .build();
        def fileStorage = new FileStore(fileName);

        fileStorage.store(Collections.singletonList(simpleIP))
        InformationPoint restored = new ArrayList(fileStorage.restore())[0]

        then:
        restored.className == simpleIP.className
        restored.methodName == simpleIP.methodName
        restored.metadata == simpleIP.metadata
        restored.baseScript == simpleIP.baseScript
        restored.script == simpleIP.script
        restored.sampleRate == simpleIP.sampleRate
        restored.successSampleRate == simpleIP.successSampleRate
        restored.errorSampleRate == simpleIP.errorSampleRate
    }

}
