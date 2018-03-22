package com.tomtom.james.newagent;

import java.util.List;
import java.util.Set;

public interface GlobalClassStructure {

    boolean contains(Class clazz);

    Set<Class> getClasses();

    void add(ClassDescriptor descriptor);

    ClassDescriptor get(Class clazz);

    void addChild(Class parent, Class child);

    void addEmpty(Class clazz);
}

