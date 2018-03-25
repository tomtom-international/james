package com.tomtom.james.newagent;

import com.tomtom.james.common.api.informationpoint.InformationPoint;

public class JamesObjective {
    private InformationPoint informationPoint;
    private Class clazz;
    private ObjectiveType type;

    public enum ObjectiveType {
        ADD, REMOVE;
    }

    public JamesObjective(ObjectiveType type, Class clazz, InformationPoint informationPoint) {
        this.clazz = clazz;
        this.informationPoint = informationPoint;
        this.type = type;
    }

    public InformationPoint getInformationPoint() {
        return informationPoint;
    }

    public ObjectiveType getType() {
        return type;
    }

    public Class getClazz() {
        return clazz;
    }

    public String toString() {
        return "JamesObjective: " + type + ", " + clazz + " : " + informationPoint.getMethodName();
    }

}
