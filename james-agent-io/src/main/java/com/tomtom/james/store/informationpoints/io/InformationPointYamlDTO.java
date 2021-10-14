package com.tomtom.james.store.informationpoints.io;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import com.tomtom.james.common.api.informationpoint.InformationPoint;

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
    }

    private String safeTrim(String script){
        if(script != null){
            return script.trim();
        }
        return null;
    }
}
