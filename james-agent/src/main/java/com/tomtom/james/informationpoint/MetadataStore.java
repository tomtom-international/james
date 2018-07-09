package com.tomtom.james.informationpoint;

import com.tomtom.james.common.api.informationpoint.Metadata;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MetadataStore {

    private static final String JOIN_CHAR = "!";

    private static final Map<String, Metadata> informationPointMetadata = new ConcurrentHashMap<>();

    private static String getKey(String className, String methodName) {
        return className + JOIN_CHAR + methodName;
    }
    public static Metadata getMetadata(String className, String methodName) {
        return informationPointMetadata.getOrDefault(getKey(className, methodName), new Metadata());
    }

    public static void setMetadata(String className, String methodName, Metadata metadata) {
        informationPointMetadata.put(getKey(className, methodName), metadata);
    }

}
