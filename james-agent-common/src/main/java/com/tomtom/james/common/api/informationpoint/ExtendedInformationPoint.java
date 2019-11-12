package com.tomtom.james.common.api.informationpoint;

import java.util.Objects;

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
        this.successSampleRate = informationPoint.getSuccessSampleRate();
        this.errorSampleRate = informationPoint.getErrorSampleRate();
        this.script = informationPoint.getScript().orElse("");
        this.methodBodyClassName = methodBodyClassName;
    }

    public String getMethodBodyClassName() {
        return methodBodyClassName;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        final ExtendedInformationPoint that = (ExtendedInformationPoint) o;
        return Objects.equals(methodBodyClassName, that.methodBodyClassName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), methodBodyClassName);
    }
}
