package com.tomtom.james.newagent;

import com.tomtom.james.common.api.informationpoint.InformationPoint;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class JamesObjective {
    private Set<InformationPoint> informationPoints = new HashSet<>();
    private Class clazz;

    public JamesObjective(Class clazz) {
        this.clazz = clazz;
    }

    public void addInformationPoint(InformationPoint informationPoint) {
        this.informationPoints.add(informationPoint);
    }

    public Set<InformationPoint> getInformationPoints() {
        return informationPoints;
    }

    public Class getClazz() {
        return clazz;
    }

    public String toString() {
        return "JamesObjective: " + clazz + " : [" + informationPoints.size() + "]" + informationPoints.stream().map(InformationPoint::getMethodName).collect(Collectors.joining(", "));
    }

}
