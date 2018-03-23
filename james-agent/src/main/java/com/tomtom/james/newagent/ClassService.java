package com.tomtom.james.newagent;

import java.util.Set;

public interface ClassService {
    Set<Class> getAllClasses(String className);
    Set<Class> getAllClasses();
    Set<Class> getChildrenOf(String className);

    // FIXME remove after tests
    void printChildrenStructure();
    // FIXME remove after tests
    void printAllClassesStructure();
}
