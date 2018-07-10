package com.tomtom.james.common.api.informationpoint;

import java.util.concurrent.ConcurrentHashMap;

public class Metadata extends ConcurrentHashMap<String, Object> {

    public static final String PREFIX = "@metadata";
    public static final String OWNER = "owner";
    public static final String ELASTIC_SEARCH_INDEX = "esIndex";

    @Override
    public String toString() {
        return "MetadataStore : size=" + size();
    }
}
