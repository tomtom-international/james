package com.tomtom.james.newagent.tools;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * contains map of interfaces and abstract classes : class.name -> all children without interfaces
 */
public interface ClassStructure {

    boolean contains(String className);

    Set<Class> getChildren(String className);

    void addChild(String className, Class<?> clazz);

    Map<String, Set<Class>> getMap();

    Collection<Set<Class>> values();

    // FIXME remove after tests
    void printClassStructure();
}
