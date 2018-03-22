package com.tomtom.james.newagent;

import java.util.HashSet;
import java.util.Set;

public class ClassDescriptor {
    private Class<?> clazz;
    private Set<Class<?>> parents = new HashSet<>();
    private Set<Class<?>> children = new HashSet<>();

    public ClassDescriptor(Class<?> clazz, Set<Class<?>> parents, Set<Class<?>> children) {
        this.clazz = clazz;
        this.parents = parents;
        this.children = children;
    }

    public Class<?> getClazz() {
        return clazz;
    }

    public Set<Class<?>> getParents() {
        return parents;
    }

    public Set<Class<?>> getChildren() {
        return children;
    }

    public void setChildren(Set<Class<?>> children) {
        this.children = children;
    }

    public void addChild(Class<?> child) {
        this.children.add(child);
    }
}
