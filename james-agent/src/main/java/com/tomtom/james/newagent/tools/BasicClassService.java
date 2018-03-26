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


    public BasicClassService(NewClassQueue newClassQueue, Collection<String> ignoredPackages, int initDelay, int scanPeriod) {
        this.allClassesMap = new BasicClassStructure();
        this.childrenMap = new BasicClassStructure();
        scanner = new Thread(new JamesClassScanner(newClassQueue, allClassesMap, childrenMap, ignoredPackages, initDelay, scanPeriod));
        scanner.setDaemon(true);
        scanner.start();
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

    @Override
    public void printChildrenStructure() {
        childrenMap.printClassStructure();
    }

    @Override
    public void printAllClassesStructure() {
        allClassesMap.printClassStructure();
    }

}
