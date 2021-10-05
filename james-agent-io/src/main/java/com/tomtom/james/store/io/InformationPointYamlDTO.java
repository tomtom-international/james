package com.tomtom.james.store.io;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.tomtom.james.common.api.informationpoint.InformationPoint;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class InformationPointYamlDTO extends InformationPointDTO {

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
        builder.withBaseScript(baseScript == null? null :baseScript.getScript());
        builder.withScript(script);
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
}
