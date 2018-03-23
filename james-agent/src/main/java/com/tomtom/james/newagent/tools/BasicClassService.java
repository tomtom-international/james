package com.tomtom.james.newagent.tools;

import com.tomtom.james.newagent.ClassService;
import com.tomtom.james.newagent.JamesClassScanner;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

public class BasicClassService implements ClassService {
    private Thread scanner;
    private ClassStructure allClassesMap;
    private ClassStructure childrenMap;


    public BasicClassService(NewClassQueue newClassQueue, int initDelay, int scanPeriod) {
        allClassesMap = new BasicClassStructure();
        childrenMap = new BasicClassStructure();
        scanner = new Thread(new JamesClassScanner(newClassQueue, allClassesMap, childrenMap, initDelay, scanPeriod));
        //scanner.setDaemon(true);
        scanner.run();
    }

    @Override
    public Set<Class> getAllClasses(String className) {
        return allClassesMap.getChildren(className);
    }

    @Override
    public Set<Class> getAllClasses() {
        return allClassesMap.values().stream().flatMap(Collection::stream).collect(Collectors.toSet());
    }

    @Override
    public Set<Class> getChildrenOf(String className) {
        return childrenMap.getChildren(className);
    }
}