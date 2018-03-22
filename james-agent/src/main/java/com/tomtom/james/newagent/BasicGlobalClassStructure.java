package com.tomtom.james.newagent;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class BasicGlobalClassStructure implements GlobalClassStructure {
    private Map<Class, ClassDescriptor> classList = new ConcurrentHashMap<>();
    private Map<String, Map<Class<?>,ClassDescriptor>> container = new ConcurrentHashMap<>();

    @Override
    public boolean contains(Class clazz) {
        return classList.containsKey(clazz);
    }

    @Override
    public Set<Class> getClasses() {
        return classList.keySet();
    }

    @Override
    public void add(ClassDescriptor descriptor) {
        // FIXME synchronization needed if multithreaded !!!
        container.computeIfAbsent(descriptor.getClazz().getName(), kay -> new ConcurrentHashMap<>());
        Map<Class<?>, ClassDescriptor> singleNameContainer = container.get(descriptor.getClazz().getName());
        singleNameContainer.put(descriptor.getClazz(), descriptor);
        container.put(descriptor.getClazz().getName(), singleNameContainer);
        classList.put(descriptor.getClazz(), descriptor);
    }

    @Override
    public ClassDescriptor get(Class clazz) {
        Map<Class<?>, ClassDescriptor> map = container.get(clazz.getName());
        if (map == null) return null;
        return map.get(clazz);
    }

    @Override
    public void addChild(Class parent, Class child) {
        // FIXME synchronization needed if multithreaded !!!
        container.computeIfAbsent(parent.getName(), kay -> new ConcurrentHashMap<>());
        Map<Class<?>, ClassDescriptor> singleNameContainer = container.get(parent.getName());
        ClassDescriptor parentClassDescriptor = singleNameContainer.get(parent);
        parentClassDescriptor.addChild(child);
        singleNameContainer.put(parent,parentClassDescriptor);
        container.put(parent.getName(), singleNameContainer);
    }

    @Override
    public void addEmpty(Class clazz) {
        add(new ClassDescriptor(clazz, new HashSet<>(), new HashSet<>()));
    }
}
