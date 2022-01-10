package com.tomtom.james.store.informationpoints.io;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.tomtom.james.common.api.informationpoint.InformationPoint;
import com.tomtom.james.common.api.informationpoint.Metadata;
import java.util.Objects;

public abstract class InformationPointDTO {

    protected String className;
    protected String methodName;
    protected Integer sampleRate;
    protected Double successSampleRate;
    protected Double errorSampleRate;
    protected Long successExecutionThreshold;
    protected Metadata metadata = new Metadata();
    protected String baseScriptPath;
    protected String scriptPath;
    protected Integer version;

    public abstract InformationPointDTO processFiles(ScriptsStore fileScriptStore);

    public InformationPointDTO withMethodReference(String classAndMethodName){
        if(classAndMethodName == null || !classAndMethodName.contains("!")){
            throw new IllegalArgumentException("Name passed shouldn't be null and should contain '!' mark");
        }
        final String[] classAndMethod = classAndMethodName.split("!");
        className = classAndMethod[0];
        methodName = classAndMethod[1];
        return this;
    }

    public abstract InformationPoint toInformationPoint();

    @JsonIgnore
    public String getMethodReference(){
        return className+"!"+methodName;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final InformationPointDTO that = (InformationPointDTO)o;
        return Objects.equals(className, that.className) && Objects.equals(methodName, that.methodName)
               && Objects.equals(sampleRate, that.sampleRate) && Objects.equals(successSampleRate,
                                                                                that.successSampleRate)
               && Objects.equals(errorSampleRate, that.errorSampleRate) && Objects.equals(
            successExecutionThreshold, that.successExecutionThreshold) && Objects.equals(metadata, that.metadata)
               && Objects.equals(baseScriptPath, that.baseScriptPath) && Objects.equals(scriptPath,
                                                                                        that.scriptPath)
               && Objects.equals(version, that.version);
    }

    @Override
    public int hashCode() {
        return Objects.hash(className, methodName, sampleRate, successSampleRate, errorSampleRate, successExecutionThreshold,
                            metadata, baseScriptPath, scriptPath, version);
    }
}
