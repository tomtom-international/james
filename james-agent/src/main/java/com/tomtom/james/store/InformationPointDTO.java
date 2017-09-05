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

package com.tomtom.james.store;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.tomtom.james.common.api.informationpoint.InformationPoint;

import java.util.List;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
class InformationPointDTO {

    private String className;
    private String methodName;
    private List<String> script;
    private Integer sampleRate;

    // For marshalling
    InformationPointDTO() {
    }

    InformationPointDTO(InformationPoint informationPoint) {
        className = informationPoint.getClassName();
        methodName = informationPoint.getMethodName();
        script = informationPoint.splittedScriptLines();
        sampleRate = informationPoint.getSampleRate();
    }

    public InformationPoint toInformationPoint() {
        InformationPoint.Builder builder = InformationPoint.builder()
                .withClassName(className)
                .withMethodName(methodName);
        if (script != null) {
            builder.withScript(String.join("\n", script));
        }
        builder.withSampleRate(sampleRate);
        return builder.build();
    }
}
