package com.tomtom.james.common.api.informationpoint;

import java.util.concurrent.ConcurrentHashMap;

public class Metadata extends ConcurrentHashMap<String, Object> {

    public static final String PREFIX = "@metadata";

    @Override
    public String toString() {
        return "Metadata: " + super.toString();
    }
}
