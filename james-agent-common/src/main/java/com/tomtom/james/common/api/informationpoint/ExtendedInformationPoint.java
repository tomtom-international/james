package com.tomtom.james.common.api.informationpoint;

public class ExtendedInformationPoint extends InformationPoint {
    // name of the class that contains body of method
    protected String methodBodyClassName;

    private ExtendedInformationPoint() {
        super();
    }

    public ExtendedInformationPoint(InformationPoint informationPoint, String methodBodyClassName) {
        this();
        this.methodName = informationPoint.getMethodName();
        this.className = informationPoint.getClassName();
        this.sampleRate = informationPoint.getSampleRate();
        this.script = informationPoint.getScript().orElse("");
        this.methodBodyClassName = methodBodyClassName;
    }

    public String getMethodBodyClassName() {
        return methodBodyClassName;
    }

}
