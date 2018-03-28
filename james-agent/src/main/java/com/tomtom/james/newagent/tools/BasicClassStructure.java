package com.tomtom.james.newagent.tools;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class BasicClassStructure implements ClassStructure {
    // map className -> set of Class in any classloader
    private Map<String, Set<Class>> container = new ConcurrentHashMap<>();

    public boolean contains(String className) {
        return container.containsKey(className);
    }

    @Override
    public Set<Class> getChildren(String className) {
        return (container.get(className) != null) ? container.get(className) : new HashSet<>();
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

    //FIXME remove after tests
    public void printClassStructure() {
        System.out.println("-------------------------------------------------------------");
        List<String> keys = new ArrayList<>(container.keySet());
        Collections.sort(keys);
        for (String className : keys) {
            System.out.println(" " + className);
            container.get(className).forEach(item -> System.out.println("      - " + item));
        }
        System.out.println("-------------------------------------------------------------");
    }
}
