/*
 * Copyright 2017 TomTom International B.V.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tomtom.james.configuration;

import com.tomtom.james.common.api.configuration.ConfigurationStructureException;
import com.tomtom.james.common.api.configuration.StructuredConfiguration;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

class YAMLConfiguration implements StructuredConfiguration {

    private final Object root;
    private final List<String> path;

    YAMLConfiguration(InputStream is) {
        root = assertConfigurationNotEmpty(new Yaml().load(is));
        path = Collections.emptyList();
    }

    YAMLConfiguration(String s) {
        root = assertConfigurationNotEmpty(new Yaml().load(s));
        path = Collections.emptyList();
    }

    private YAMLConfiguration(Object root, List<String> path) {
        this.root = root;
        this.path = path;
    }

    @Override
    public Optional<StructuredConfiguration> get(String path) {
        return get(path.split("\\."));
    }

    @Override
    public Optional<StructuredConfiguration> get(String... pathSegments) {
        Object currentNode = root;
        for (String segment : pathSegments) {
            currentNode = asCheckedMap(currentNode).get(segment);
            if (currentNode == null) {
                return Optional.empty();
            }
        }
        List<String> newPath = new ArrayList<>(path);
        Collections.addAll(newPath, pathSegments);
        return Optional.of(new YAMLConfiguration(currentNode, newPath));
    }

    @Override
    public String asString() {
        try {
            return (String) root;
        } catch (ClassCastException e) {
            throw new ConfigurationException(e);
        }
    }

    @Override
    public boolean asBoolean() {
        try {
            return (Boolean) root;
        } catch (ClassCastException e) {
            throw new ConfigurationException(e);
        }
    }

    @Override
    public int asInteger() {
        try {
            return (Integer) root;
        } catch (ClassCastException e) {
            throw new ConfigurationException(e);
        }
    }

    @Override
    public long asLong() {
        try {
            return (root instanceof Long) ? (Long) root : (Integer) root;
        } catch (ClassCastException e) {
            throw new ConfigurationException(e);
        }
    }

    @Override
    public List<StructuredConfiguration> asList() {
        try {
            List<Object> rootAsList = asCheckedList(root);
            ArrayList<StructuredConfiguration> result = new ArrayList<>(rootAsList.size());
            for (int i = 0; i < rootAsList.size(); i++) {
                List<String> newPath = new ArrayList<>(path);
                newPath.add("[" + i + "]");
                result.add(new YAMLConfiguration(rootAsList.get(i), newPath));
            }
            return result;
        } catch (ClassCastException e) {
            throw new ConfigurationException(e);
        }
    }

    @Override
    public Map<String, StructuredConfiguration> asMap() {
        try {
            return asCheckedMap(root).entrySet().stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, entry -> {
                        List<String> newPath = new ArrayList<>(path);
                        newPath.add("[" + entry.getKey() + "]");
                        return new YAMLConfiguration(entry.getValue(), newPath);
                    }));
        } catch (ClassCastException e) {
            throw new ConfigurationException(e);
        }
    }

    @Override
    public String toString() {
        return String.join(".", path);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> asCheckedMap(Object node) {
        return (Map<String, Object>) node;
    }

    @SuppressWarnings("unchecked")
    private List<Object> asCheckedList(Object node) {
        return (List<Object>) node;
    }

    private class ConfigurationException extends RuntimeException {
        ConfigurationException(Exception e) {
            super(String.format("Error reading configuration property %s: %s",
                    YAMLConfiguration.this.toString(),
                    e.getMessage()));
        }
    }

    private Object assertConfigurationNotEmpty(Object o) {
        if (o == null) {
            throw new ConfigurationStructureException("Empty configuration file");
        }
        return o;
    }
}
