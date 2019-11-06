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

package com.tomtom.james.common.api.informationpoint;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class InformationPoint {

    protected String className;
    protected String methodName;
    protected String script;
    //unused left for backward compatibility
    protected Integer sampleRate;
    protected Integer successSampleRate;
    protected Integer errorSampleRate;
    protected Metadata metadata;
    protected Boolean requiresInitialContext = Boolean.FALSE;

    public InformationPoint() {
        metadata = new Metadata();
    }

    public String getClassName() {
        return className;
    }

    public String getMethodName() {
        return methodName;
    }

    public Optional<String> getScript() {
        return Optional.ofNullable(script);
    }

    public int getSampleRate() {
        return Optional.ofNullable(sampleRate).orElse(100);
    }

    public int getSuccessSampleRate() {
        return Optional.ofNullable(successSampleRate).orElseGet(this::getSampleRate);
    }

    public int getErrorSampleRate() {
        return Optional.ofNullable(errorSampleRate).orElseGet(this::getSampleRate);
    }

    public List<String> splittedScriptLines() {
        if (script != null) {
            return Arrays.asList(script.split("\n"));
        } else {
            return Collections.emptyList();
        }
    }

    public Boolean getRequiresInitialContext() {
        return requiresInitialContext;
    }

    public Metadata getMetadata() {
        return metadata;
    }

    @Override
    public String toString() {
        return className + '#' + methodName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        InformationPoint that = (InformationPoint) o;
        return Objects.equals(className, that.className) &&
                Objects.equals(methodName, that.methodName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(className, methodName);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private static final int CLASS_NAME_INDEX = 0;
        private static final int METHOD_NAME_INDEX = 1;

        private String className;
        private String methodName;
        private String script;
        private Integer sampleRate;
        private Integer successSampleRate;
        private Integer errorSampleRate;
        private Metadata metadata;
        private Boolean requireInitialContext = Boolean.FALSE;

        private Builder() {
            metadata = new Metadata();
        }

        public Builder withClassName(String className) {
            this.className = className;
            return this;
        }

        public Builder withMethodName(String methodName) {
            this.methodName = methodName;
            return this;
        }

        public Builder withMethodReference(String encodedReference) {
            List<String> parts = Pattern.compile("#")
                    .splitAsStream(Objects.requireNonNull(encodedReference))
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());
            className = parts.get(CLASS_NAME_INDEX);
            methodName = parts.get(METHOD_NAME_INDEX);
            return this;
        }

        public Builder withScript(String script) {
            this.script = script;
            if (script.contains(" onPrepareContext")) {
                this.requireInitialContext = true;
            }
            return this;
        }

        public Builder withSampleRate(Integer sampleRate) {
            this.sampleRate = sampleRate;
            return this;
        }

        public Builder withSuccessSampleRate(Integer successSampleRate) {
            if(sampleRate != null && successSampleRate != null &&  !sampleRate.equals(successSampleRate)){
                throw new IllegalStateException("Cannot set successSampleRate when sampleRate is set.");
            }
            this.successSampleRate = successSampleRate;
            return this;
        }

        public Builder withErrorSampleRate(Integer errorSampleRate) {
            if(sampleRate != null && errorSampleRate != null && !sampleRate.equals(errorSampleRate)){
                throw new IllegalStateException("Cannot set errorSampleRate when sampleRate is set.");
            }
            this.errorSampleRate = errorSampleRate;
            return this;
        }

        public Builder withMetadata(Metadata metadata) {
            if(metadata != null) {
                this.metadata.putAll(metadata);
            }
            return this;
        }

        public Builder copyOf(InformationPoint copyFrom) {
            this.className = copyFrom.className;
            this.methodName = copyFrom.methodName;
            this.script = copyFrom.script;
            this.sampleRate = copyFrom.sampleRate;
            this.successSampleRate = copyFrom.successSampleRate;
            this.errorSampleRate = copyFrom.errorSampleRate;
            this.metadata.putAll(copyFrom.metadata);
            this.requireInitialContext = copyFrom.requiresInitialContext;
            return this;
        }

        public InformationPoint build() {
            InformationPoint ip = new InformationPoint();
            ip.className = Objects.requireNonNull(className);
            ip.methodName = Objects.requireNonNull(methodName);
            ip.script = script;
            ip.sampleRate = sampleRate;
            ip.successSampleRate = successSampleRate;
            ip.errorSampleRate = errorSampleRate;
            ip.metadata.putAll(metadata);
            ip.requiresInitialContext = requireInitialContext;
           return ip;
        }
    }
}