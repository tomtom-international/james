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

package com.tomtom.james.agent;

import com.google.common.collect.Iterables;
import com.google.common.collect.Streams;
import com.tomtom.james.common.api.Identifiable;
import com.tomtom.james.common.api.configuration.EventPublisherConfiguration;
import com.tomtom.james.common.api.configuration.JamesControllerConfiguration;
import com.tomtom.james.common.api.configuration.ToolkitConfiguration;
import com.tomtom.james.common.api.controller.JamesController;
import com.tomtom.james.common.api.publisher.EventPublisher;
import com.tomtom.james.common.api.toolkit.Toolkit;
import com.tomtom.james.common.log.Logger;
import com.tomtom.james.publisher.ConsolePublisher;
import com.tomtom.james.publisher.FilePublisher;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class PluginManager {

    private static final Logger LOG = Logger.getLogger(PluginManager.class);

    private final Collection<URLClassLoader> pluginClassLoaders;

    public PluginManager(Collection<String> pluginDirectoriesPaths, Collection<String> pluginFilesPaths) {
        Collection<URL> pluginURLs = findPlugins(
                Objects.requireNonNull(pluginDirectoriesPaths),
                Objects.requireNonNull(pluginFilesPaths));
        pluginClassLoaders = createClassLoadersForPluginJars(pluginURLs);
    }

    public Optional<EventPublisher> createEventPublisherPluginInstance(EventPublisherConfiguration configuration) {
        ArrayList<EventPublisher> publishers = new ArrayList<>();
        publishers.addAll(createBuiltinPublisherPlugins());
        publishers.addAll(loadPlugins(EventPublisher.class));

        Collection<EventPublisher> publishersMatchingId = publishers.stream()
                .filter(publisher -> publisher.getId().equals(configuration.getId()))
                .collect(Collectors.toList());

        return publishersMatchingId.size() > 0
                ? Optional.of(Iterables.getOnlyElement(publishersMatchingId))
                : Optional.empty();
    }

    Optional<JamesController> createControllerPluginInstance(JamesControllerConfiguration configuration) {
        Collection<JamesController> jamesControllers = loadPlugins(JamesController.class);
        Collection<JamesController> controllersMatchingId = jamesControllers.stream()
                .filter(controller -> controller.getId().equals(configuration.getId()))
                .collect(Collectors.toList());
        return controllersMatchingId.size() > 0
                ? Optional.of(Iterables.getOnlyElement(controllersMatchingId))
                : Optional.empty();
    }

    Optional<Toolkit> createToolkitPluginInstance(ToolkitConfiguration configuration) {
        Collection<Toolkit> toolkits = loadPlugins(Toolkit.class);
        Collection<Toolkit> toolkitsMatchingId = toolkits.stream()
                .filter(toolkit -> toolkit.getId().equals(configuration.getId()))
                .collect(Collectors.toList());
        return toolkitsMatchingId.size() > 0
                ? Optional.of(Iterables.getOnlyElement(toolkitsMatchingId))
                : Optional.empty();
    }

    private static Collection<URL> findPlugins(Collection<String> directoriesPaths, Collection<String> filePaths) {
        ArrayList<URL> pluginURLs = new ArrayList<>();

        for (String directoryPath : directoriesPaths) {
            Path path = FileSystems.getDefault().getPath(directoryPath);
            try {
                try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(path)) {
                    for (Path pluginJarPath : dirStream) {
                        pluginURLs.add(pluginJarPath.normalize().toUri().toURL());
                    }
                }
            } catch (IOException e) {
                LOG.warn("Error loading plugins: " + e);
            }
        }
        for (String filePath : filePaths) {
            Path path = FileSystems.getDefault().getPath(filePath);
            try {
                pluginURLs.add(path.normalize().toUri().toURL());
            } catch (IOException e) {
                LOG.warn("Error loading plugin: " + e);
            }
        }
        return pluginURLs;
    }

    private Collection<URLClassLoader> createClassLoadersForPluginJars(Collection<URL> pluginURLs) {
        return pluginURLs.stream()
                .map(url -> new URLClassLoader(new URL[]{url}))
                .collect(Collectors.toList());
    }

    private <T extends Identifiable> Collection<T> loadPlugins(Class<T> pluginClass) {
        ArrayList<T> loadedPlugins = new ArrayList<>();
        pluginClassLoaders.forEach(classLoader -> {
            ServiceLoader<T> serviceLoader = ServiceLoader.load(pluginClass, classLoader);
            loadedPlugins.addAll(Streams.stream(serviceLoader).collect(Collectors.toList()));
        });
        return loadedPlugins.stream().distinct().collect(Collectors.toList());
    }

    private Collection<EventPublisher> createBuiltinPublisherPlugins() {
        EventPublisher consolePublisher = new ConsolePublisher();
        EventPublisher filePublisher = new FilePublisher();
        return Arrays.asList(consolePublisher, filePublisher);
    }

}
