package com.tomtom.james.newagent;

import com.tomtom.james.common.api.informationpoint.InformationPoint;

public class JamesObjective {
    private InformationPoint informationPoint;
    private Class clazz;

    public JamesObjective(Class clazz, InformationPoint informationPoint) {
        this.clazz = clazz;
        this.informationPoint = informationPoint;
    }

    public InformationPoint getInformationPoint() {
        return informationPoint;
    }

    public Class getClazz() {
        return clazz;
    }
}
