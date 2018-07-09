package com.tomtom.james.common.api.informationpoint;

import java.util.concurrent.ConcurrentHashMap;

public class Metadata extends ConcurrentHashMap<String, Object> {

    public static final String OWNER = "informationPointOwner";
    public static final String INDEX = "informationPointIndex";

    @Override
    public String toString() {
        return "MetadataStore : size=" + size();
    }
}
