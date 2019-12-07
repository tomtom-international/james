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

import java.util.List;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.tomtom.james.common.api.informationpoint.InformationPoint;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
class InformationPointDTO {

    private String className;
    private String methodName;
    private List<String> baseScript;
    private List<String> script;
    private Integer sampleRate;
    private Double successSampleRate;
    private Double errorSampleRate;
    private Long successExecutionThreshold;

    // For marshalling
    InformationPointDTO() {
    }

    InformationPointDTO(InformationPoint informationPoint) {
        className = informationPoint.getClassName();
        methodName = informationPoint.getMethodName();
        baseScript = informationPoint.splittedBaseScriptLines();
        script = informationPoint.splittedScriptLines();
        sampleRate = informationPoint.getSampleRate();
        successSampleRate = informationPoint.getErrorSampleRate();
        errorSampleRate = informationPoint.getSuccessSampleRate();
        successExecutionThreshold = informationPoint.getSuccessExecutionThreshold();
    }

    public InformationPoint toInformationPoint() {
        InformationPoint.Builder builder = InformationPoint.builder()
                .withClassName(className)
                .withMethodName(methodName);
        builder.withBaseScript(baseScript);
        builder.withScript(script);
        builder.withSampleRate(sampleRate);
        builder.withSuccessSampleRate(successSampleRate);
        builder.withErrorSampleRate(errorSampleRate);
        builder.withSuccessExecutionThreshold(successExecutionThreshold);
        return builder.build();
    }
}
