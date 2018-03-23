package com.tomtom.james.newagent.tools;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class BasicClassStructure implements ClassStructure {
    private Map<String, Set<Class>> container = new ConcurrentHashMap<>();

    public boolean contains(String className) {
        return container.containsKey(className);
    }

    @Override
    public Set<Class> getChildren(String className) {
        return container.get(className);
    }

    // FIXME - possible synchronization FUCKUP - think twice ! - current classScanner is single threaded and there is no risk ... but ...
    @Override
    public void addChild(String className, Class<?> clazz) {
        container.computeIfAbsent(className, key -> new HashSet<>());
        Set<Class> classSet = container.get(className);
        classSet.add(clazz);
        container.put(className, classSet);
    }

    @Override
    public Map<String, Set<Class>> getMap() {
        return container;
    }

    public Collection<Set<Class>> values() {
        return container.values();
    }

}
