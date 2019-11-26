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

package com.tomtom.james.controller.webservice.api.v1;
import com.tomtom.james.common.api.informationpoint.Metadata;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class InformationPointDTO {

    private String className;
    private String methodName;
    private List<String> baseScript;
    private List<String> script;
    private Integer sampleRate;
    private Double successSampleRate;
    private Double errorSampleRate;
    private Metadata metadata;

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public List<String> getBaseScript() {
        return baseScript;
    }

    public void setBaseScript(List<String> baseScript) {
        this.baseScript = baseScript;
    }

    public List<String> getScript() {
        return script;
    }

    public void setScript(List<String> script) {
        this.script = script;
    }

    public Integer getSampleRate() {
        return sampleRate;
    }

    public void setSampleRate(Integer sampleRate) {
        this.sampleRate = sampleRate;
    }

    public Metadata getMetadata() {
        return metadata;
    }

    public void setMetadata(Metadata metadata) {
        this.metadata = metadata;
    }

    public Double getSuccessSampleRate() {
        return successSampleRate;
    }

    public void setSuccessSampleRate(final Double successSampleRate) {
        this.successSampleRate = successSampleRate;
    }

    public Double getErrorSampleRate() {
        return errorSampleRate;
    }

    public void setErrorSampleRate(final Double errorSampleRate) {
        this.errorSampleRate = errorSampleRate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        InformationPointDTO that = (InformationPointDTO) o;
        return Objects.equals(className, that.className) &&
                Objects.equals(methodName, that.methodName) &&
                Objects.equals(baseScript, that.baseScript) &&
                Objects.equals(script, that.script) &&
                Objects.equals(sampleRate, that.sampleRate) &&
                Objects.equals(successSampleRate, that.successSampleRate) &&
                Objects.equals(errorSampleRate, that.errorSampleRate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(className, methodName, baseScript, script, sampleRate, successSampleRate, errorSampleRate, metadata);
    }

}