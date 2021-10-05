package com.tomtom.james.store.io;

import com.tomtom.james.common.api.informationpoint.InformationPoint;
import com.tomtom.james.common.api.informationpoint.Metadata;

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

    public String getMethodReference(){
        return className+"!"+methodName;
    }
}
