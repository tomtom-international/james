package com.tomtom.james.store.informationpoints.io;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import com.tomtom.james.common.api.informationpoint.InformationPoint;
import java.util.Objects;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class InformationPointYamlDTO extends InformationPointDTO {

    @JsonIdentityInfo(generator = ObjectIdGenerators.StringIdGenerator.class)
    private BaseScript baseScript;
    private String script;

    // For marshalling
    InformationPointYamlDTO() {
    }

    InformationPointYamlDTO(InformationPoint informationPoint) {
        className = informationPoint.getClassName();
        methodName = informationPoint.getMethodName();
        baseScript = new BaseScript(informationPoint.getBaseScript().orElse(null));
        script = informationPoint.getScript().orElse(null);
        sampleRate = informationPoint.getSampleRate();
        successSampleRate = informationPoint.getSuccessSampleRate();
        errorSampleRate = informationPoint.getErrorSampleRate();
        metadata = informationPoint.getMetadata();
        successExecutionThreshold = informationPoint.getSuccessExecutionThreshold();
    }

    @Override
    public InformationPointDTO processFiles(ScriptsStore fileScriptStore) {
        if (this.scriptPath != null) {
            this.script = fileScriptStore.loadScriptByName(this.scriptPath);
        }
        if (this.baseScriptPath != null) {
            this.baseScript = new BaseScript(fileScriptStore.loadScriptByName(this.baseScriptPath));
        }
        return this;
    }

    @Override
    public InformationPoint toInformationPoint() {
        InformationPoint.Builder builder = InformationPoint.builder()
                                                           .withClassName(className)
                                                           .withMethodName(methodName);
        builder.withMetadata(metadata);
        builder.withBaseScript(baseScript == null? null : safeTrim(baseScript.getScript()));
        builder.withScript(safeTrim(script));
        builder.withSampleRate(sampleRate);
        builder.withSuccessSampleRate(successSampleRate);
        builder.withErrorSampleRate(errorSampleRate);
        builder.withSuccessExecutionThreshold(successExecutionThreshold);
        return builder.build();
    }

    static class BaseScript {
        String script;

        BaseScript(){
        }

        BaseScript(String script){
            this.script = script;
        }

        public String getScript() {
            return script;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            final BaseScript that = (BaseScript)o;
            return Objects.equals(script, that.script);
        }

        @Override
        public int hashCode() {
            return Objects.hash(script);
        }
    }

    private String safeTrim(String script){
        if(script != null){
            return script.trim();
        }
        return null;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        final InformationPointYamlDTO that = (InformationPointYamlDTO)o;
        return Objects.equals(baseScript, that.baseScript) && Objects.equals(script, that.script);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), baseScript, script);
    }
}
