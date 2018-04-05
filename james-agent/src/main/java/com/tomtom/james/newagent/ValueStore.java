package com.tomtom.james.newagent;

import java.util.concurrent.ConcurrentHashMap;

public class ValueStore<T> extends ConcurrentHashMap<String, T> {

    @Override
    public String toString() {
        return "ValueStore : size=" + size();
    }
}
