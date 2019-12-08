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
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class InformationPoint {

    protected String className;
    protected String methodName;
    protected String baseScript;
    protected String script;
    //unused. left for backward compatibility
    protected int sampleRate = 100;
    protected double successSampleRate = 100;
    protected double errorSampleRate = 100;
    protected long successExecutionThreshold = -1;
    protected Metadata metadata;
    protected Boolean requiresInitialContext = Boolean.FALSE;

    public InformationPoint() {
        metadata = new Metadata();
    }

    public InformationPoint(InformationPoint informationPoint) {
        this.className = informationPoint.className;
        this.methodName = informationPoint.methodName;
        this.baseScript = informationPoint.baseScript;
        this.script = informationPoint.script;
        this.sampleRate = informationPoint.sampleRate;
        this.successSampleRate = informationPoint.successSampleRate;
        this.errorSampleRate = informationPoint.errorSampleRate;
        this.successExecutionThreshold = informationPoint.successExecutionThreshold;
        this.metadata = informationPoint.metadata;
        this.requiresInitialContext = informationPoint.requiresInitialContext;
    }

    public String getClassName() {
        return className;
    }

    public String getMethodName() {
        return methodName;
    }

    public Optional<String> getBaseScript() {
        return Optional.ofNullable(baseScript);
    }

    public List<String> splittedBaseScriptLines() {
        return getBaseScript().map(s -> Arrays.asList(s.split("\n"))).orElse(null);
    }

    public Optional<String> getScript() {
        return Optional.ofNullable(script);
    }

    public List<String> splittedScriptLines() {
        return getScript().map(s -> Arrays.asList(s.split("\n"))).orElse(null);
    }

    public int getSampleRate() {
        return sampleRate;
    }

    public double getSuccessSampleRate() {
        return successSampleRate;
    }

    public double getErrorSampleRate() {
        return errorSampleRate;
    }

    public long getSuccessExecutionThreshold() {
        return successExecutionThreshold;
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
        private String baseScript;
        private Integer sampleRate;
        private Double successSampleRate;
        private Double errorSampleRate;
        private Long successExecutionThreshold;
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

        public Builder withBaseScript(List<String> baseScript) {
            return withBaseScript(joinedScriptLines(baseScript));
        }

        public Builder withBaseScript(String baseScript) {
            this.baseScript = baseScript;
            return doesRequireInitialContext(baseScript);
        }

        public Builder withScript(List<String> script) {
            return withScript(joinedScriptLines(script));
        }

        public Builder withScript(String script) {
            this.script = script;
            return doesRequireInitialContext(script);
        }

        private String joinedScriptLines(List<String> scriptLines) {
            return scriptLines != null ? String.join("\n", scriptLines) : null;
        }

        private Builder doesRequireInitialContext(String script) {
            if (script != null && script.contains(" onPrepareContext")) {
                this.requireInitialContext = true;
            }
            return this;
        }

        public Builder withSampleRate(Integer sampleRate) {
            this.sampleRate = sampleRate;
            return this;
        }

        public Builder withSuccessSampleRate(Double successSampleRate) {
            this.successSampleRate = successSampleRate;
            return this;
        }

        public Builder withErrorSampleRate(Double errorSampleRate) {
            this.errorSampleRate = errorSampleRate;
            return this;
        }

        public Builder withSuccessExecutionThreshold(Long successExecutionThreshold) {
            this.successExecutionThreshold = successExecutionThreshold;
            return this;
        }

        public Builder withMetadata(Metadata metadata) {
            if (metadata != null) {
                this.metadata.putAll(metadata);
            }
            return this;
        }

        public Builder copyOf(InformationPoint copyFrom) {
            this.className = copyFrom.className;
            this.methodName = copyFrom.methodName;
            this.baseScript = copyFrom.baseScript;
            this.script = copyFrom.script;
            this.sampleRate = copyFrom.sampleRate;
            this.successSampleRate = copyFrom.successSampleRate;
            this.errorSampleRate = copyFrom.errorSampleRate;
            this.successExecutionThreshold = copyFrom.successExecutionThreshold;
            this.metadata.putAll(copyFrom.metadata);
            this.requireInitialContext = copyFrom.requiresInitialContext;
            return this;
        }

        public InformationPoint build() {
            InformationPoint ip = new InformationPoint();
            ip.className = Objects.requireNonNull(className);
            ip.methodName = Objects.requireNonNull(methodName);
            ip.baseScript = baseScript;
            ip.script = script;
            ip.sampleRate = Optional.ofNullable(sampleRate).orElse(100);
            ip.successSampleRate = Optional.ofNullable(successSampleRate).orElse((double) ip.sampleRate);
            ip.errorSampleRate = Optional.ofNullable(errorSampleRate).orElse((double) ip.sampleRate);
            ip.successExecutionThreshold = Optional.ofNullable(successExecutionThreshold).orElse(-1L);
            ip.metadata.putAll(metadata);
            ip.requiresInitialContext = requireInitialContext;
            return ip;
        }
    }
}