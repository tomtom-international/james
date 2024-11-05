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

package com.tomtom.james.controller.kubernetes;

import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.common.util.concurrent.Uninterruptibles;
import com.google.gson.reflect.TypeToken;
import com.tomtom.james.common.api.ClassScanner;
import com.tomtom.james.common.api.QueueBacked;
import com.tomtom.james.common.api.configuration.JamesControllerConfiguration;
import com.tomtom.james.common.api.controller.JamesController;
import com.tomtom.james.common.api.informationpoint.InformationPoint;
import com.tomtom.james.common.api.informationpoint.InformationPointService;
import com.tomtom.james.common.api.publisher.EventPublisher;
import com.tomtom.james.common.api.script.ScriptEngine;
import com.tomtom.james.common.log.Logger;
import com.tomtom.james.store.informationpoints.io.ConfigIOFactory;
import com.tomtom.james.store.informationpoints.io.ConfigParser;
import com.tomtom.james.store.informationpoints.io.InMemoryScriptStore;
import com.tomtom.james.store.informationpoints.io.InformationPointDTO;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1ConfigMap;
import io.kubernetes.client.openapi.models.V1ConfigMapList;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.util.Config;
import io.kubernetes.client.util.Watch;
import okhttp3.OkHttpClient;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class KubernetesController implements JamesController {

    private static final Logger LOG = Logger.getLogger(KubernetesController.class);
    private final ExecutorService executor;
    private final Map<String, Map<String, InformationPointDTO>> informationPointsCache;
    private ApiClient apiClient;

    @Override
    public String getId() {
        return "james.controller.kubernetes";
    }

    public KubernetesController() {
        final ThreadFactory
            threadFactory = new ThreadFactoryBuilder()
            .setNameFormat(getId() + "-%d")
            .setDaemon(true)
            .build();
        this.executor = Executors.newSingleThreadExecutor(threadFactory);
        this.informationPointsCache = new LinkedHashMap<>();
    }

    @Override
    public void initialize(final JamesControllerConfiguration jamesControllerConfiguration,
                           final InformationPointService informationPointService,
                           final ClassScanner classScanner,
                           final ScriptEngine scriptEngine,
                           final EventPublisher eventPublisher,
                           final QueueBacked jamesObjectiveQueue,
                           final QueueBacked newClassesQueue,
                           final QueueBacked newInformationPointQueue,
                           final QueueBacked removeInformationPointQueue) {
        final KubernetesControllerConfiguration configuration =
            new KubernetesControllerConfiguration(jamesControllerConfiguration);
        apiClient = createApiClient(configuration.getUrl(), configuration.getToken());
        executor.execute(() -> {
            while (!Thread.interrupted()) {
                try (final Watch<V1ConfigMap> watch =
                         createConfigMapWatch(apiClient, configuration.getNamespace(), configuration.getLabels())) {
                    watchConfigMapChanges(watch, informationPointService);
                } catch (final Exception e) {
                    LOG.info("Unable to setup k8s watcher", e);
                    Uninterruptibles.sleepUninterruptibly(1, TimeUnit.SECONDS);
                }
            }
        });
        LOG.debug(() -> "Started watching ConfigMaps in namespace:" + configuration.getNamespace() +
                        " with labels: " + configuration.getLabels());
    }

    @Override
    public void close() {
        executor.shutdownNow();
    }

    private static ApiClient createApiClient(final String url, final String token) {
        try {
            if (url.trim().isEmpty()) {
                return Config.defaultClient();
            } else {
                return Config.fromToken(url, token, false); // it is only for local testing
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void watchConfigMapChanges(final Watch<V1ConfigMap> watch,
                                       final InformationPointService informationPointService) {
        for (final Watch.Response<V1ConfigMap> response : watch) {

            final V1ObjectMeta metadata = response.object.getMetadata();
            final String name =
                Optional.ofNullable(metadata).map(V1ObjectMeta::getName).orElse("noName");
            final Map<String, String> data = Maps.newHashMap();

            data.putAll(updatePairedMaps(metadata, name));
            final String type = response.type;
            LOG.debug(String.format("ConfigMap %s was %s ", name, type));
            if (!"DELETED".equals(type)) {
                final Map<String, String> currentConfigMapData =
                    Optional.ofNullable(response.object.getData()).orElse(Collections.emptyMap());
                data.putAll(currentConfigMapData);
            }
            processUpdate(name, readAllConfigurations(data), informationPointService);
        }
    }

    private Map<String, String> updatePairedMaps(final V1ObjectMeta metadata, final String name) {
        final String mainMapName = getSecondPartOfConfigurationName(name);

        try {
            final String fieldSelector = "metadata.name=" + mainMapName;
            final V1ConfigMapList v1ConfigMapList =
                new CoreV1Api(apiClient).listNamespacedConfigMap(metadata.getNamespace())
                                        .fieldSelector(fieldSelector)
                                        .watch(false).execute();
            return v1ConfigMapList.getItems().stream().findFirst().map(V1ConfigMap::getData)
                                  .orElse(Collections.emptyMap());
        } catch (ApiException e) {
            LOG.warn("Problem while looking for map other map");
            return Collections.emptyMap();
        }
    }

    private String getSecondPartOfConfigurationName(final String name) {
        final String suffix;
        final String toReplace;
        //configuration is stored in to config maps:
        // - configMap with configuration: CONFIG_NAME
        // - configMap with script files: CONFIG_NAME-files
        if (name.endsWith("-files")) {
            toReplace = "-files";
            suffix = "";
        } else {
            suffix = "-files";
            toReplace = "";
        }
        final String mainMapName = name.replace(toReplace, "") + suffix;
        return mainMapName;
    }

    private Watch<V1ConfigMap> createConfigMapWatch(
        final ApiClient apiClient, final String namespace, final Map<String, String> labels)
        throws ApiException {
        // infinite timeout
        final OkHttpClient httpClient =
            apiClient.getHttpClient().newBuilder().readTimeout(0, TimeUnit.SECONDS).build();
        apiClient.setHttpClient(httpClient);

        final CoreV1Api api = new CoreV1Api(apiClient);
        final String labelSelector = labels.entrySet().stream()
                                           .map(e -> String.format("%s=%s", e.getKey(), e.getValue()))
                                           .collect(Collectors.joining(","));
        return Watch.createWatch(
            apiClient,
            api.listNamespacedConfigMap(namespace).labelSelector(labelSelector).watch(true).buildCall(null),
            new TypeToken<Watch.Response<V1ConfigMap>>() {

            }.getType());
    }

    private Collection<InformationPointDTO> readAllConfigurations(Map<String, String> configMaps) {
        Map<ConfigParser, String> configurations = new HashMap<>();
        InMemoryScriptStore scriptStore = new InMemoryScriptStore();
        for (Map.Entry<String, String> configEntry : configMaps.entrySet()) {
            final ConfigParser parser = ConfigIOFactory.getInstance().getParser(configEntry.getKey()).orElse(null);
            if (parser != null) {
                configurations.put(parser, configEntry.getValue());
            } else if (configEntry.getKey().endsWith(".groovy")) {
                scriptStore.registerFile(configEntry.getKey(), configEntry.getValue());
            } else {
                LOG.warn("Unrecognized format:" + configEntry.getKey());
            }
        }
        return configurations.entrySet().stream().flatMap(entry -> {
            try (InputStream configStream = new ByteArrayInputStream(entry.getValue().getBytes());) {
                return entry.getKey().parseConfiguration(configStream, scriptStore).stream();
            } catch (IOException e) {
                LOG.error("Unable to parse configurations: " + configMaps.keySet(), e);
                return Stream.empty();
            }
        }).collect(Collectors.toSet());
    }

    private void processUpdate(final String configName, final Collection<InformationPointDTO> informationPoints,
                               final InformationPointService informationPointService) {
        final Map<String, InformationPointDTO> cache =
            informationPointsCache.computeIfAbsent(configName, name -> new LinkedHashMap<>());
        final Map<String, InformationPointDTO> informationPointsMap = informationPoints.stream().collect(
            Collectors.toMap(informationPoint -> informationPoint.getMethodReference(), Function.identity()));

        final MapDifference<String, InformationPointDTO> difference = Maps.difference(informationPointsMap, cache);

        difference.entriesOnlyOnLeft()
                  .forEach((name, value) -> onInformationPointAdded(value, informationPointService));
        difference.entriesDiffering()
                  .forEach((name, value) -> onInformationPointModified(value.leftValue(), informationPointService));
        difference.entriesOnlyOnRight().forEach((name, value) -> onInformationPointRemoved(value, informationPointService));

        cache.clear();
        cache.putAll(informationPointsMap);
    }

    private void onInformationPointAdded(final InformationPointDTO informationPointDto,
                                         final InformationPointService informationPointService) {
        final InformationPoint ip = informationPointDto.toInformationPoint();
        informationPointService.addInformationPoint(ip);
        LOG.debug(() -> "Information point " + ip + " added");
    }

    private void onInformationPointModified(final InformationPointDTO informationPointDto,
                                            final InformationPointService informationPointService) {
        final InformationPoint ip = informationPointDto.toInformationPoint();
        informationPointService.removeInformationPoint(ip);
        informationPointService.addInformationPoint(ip);
        LOG.debug(() -> "Information point " + ip + " modified");

    }

    private void onInformationPointRemoved(final InformationPointDTO informationPointDto,
                                           final InformationPointService informationPointService) {
        final InformationPoint ip = informationPointDto.toInformationPoint();
        informationPointService.removeInformationPoint(ip);
        LOG.debug(() -> "Information point " + ip + " removed");
    }

}
