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

import com.google.common.base.Splitter;
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
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.Pair;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1ConfigMap;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.util.Config;
import io.kubernetes.client.util.Watch;
import okhttp3.OkHttpClient;
import org.yaml.snakeyaml.Yaml;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class KubernetesController implements JamesController {

    private static final String SCRIPT = "script";
    private static final String BASE_SCRIPT = "baseScript";
    private static final Logger LOG = Logger.getLogger(KubernetesController.class);
    private final ExecutorService executor;
    private final Map<String, Map<String, String>> informationPointsCache;
    private final Yaml yaml = new Yaml();

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
        final ApiClient apiClient = createApiClient(configuration.getUrl(), configuration.getToken());
        executor.execute(() -> {
            while (!Thread.interrupted()) {
                try {
                    final Watch<V1ConfigMap> watch =
                        createConfigMapWatch(apiClient, configuration.getNamespace(), configuration.getLabels());
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

    private ApiClient createApiClient(final String url, final String token) {
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
            final String name =
                Optional.ofNullable(response.object.getMetadata()).map(V1ObjectMeta::getName).orElse("noName");
            final Map<String, String> data = Optional.ofNullable(response.object.getData()).orElse(Collections.emptyMap());
            final String type = response.type;
            LOG.debug(String.format("ConfigMap %s was %s ", name, type));
            final Map<String, String> informationPoints = new HashMap<>();
            for (final Map.Entry<String, String> entry : data.entrySet()) {
                if (!"DELETED".equals(type)) {
                    informationPoints.putAll(readConfigFile(entry.getKey(), entry.getValue()));
                }
            }
            processUpdate(name, informationPoints, informationPointService);
        }
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
            api.listNamespacedConfigMapCall(namespace,
                                            null,
                                            null,
                                            null,
                                            null,
                                            labelSelector,
                                            null,
                                            null,
                                            null,
                                            true,
                                            null),
            new TypeToken<Watch.Response<V1ConfigMap>>() {

            }.getType());
    }

    private Map<String, String> readConfigFile(final String name, final String content) {
        if (name.endsWith(".properties")) {
            return new BufferedReader(new StringReader(content))
                .lines()
                .map(line -> {
                    final int index = line.indexOf('=');
                    return new Pair(line.substring(0, index).replace('!', '#'), line.substring(index + 1));
                })
                .collect(Collectors.toMap(Pair::getName, Pair::getValue));
        } else if (name.endsWith(".yaml")) {
            final Map<String, Map> entries = yaml.load(content);
            return entries.entrySet().stream()
                          .collect(Collectors.toMap(e -> e.getKey().replace('!', '#'),
                                                    e -> adaptToCommonFormat(e.getValue())));
        } else {
            LOG.warn("Unrecognized format:" + name);
        }
        return Collections.emptyMap();
    }

    private String adaptToCommonFormat(final Map yamlDefinition) {
        if (yamlDefinition.containsKey(SCRIPT)) {
            yamlDefinition.put(SCRIPT, splitToLines(yamlDefinition, SCRIPT));
        }
        if (yamlDefinition.containsKey(BASE_SCRIPT) && ((Map)yamlDefinition.get(BASE_SCRIPT)).containsKey(SCRIPT)) {
            yamlDefinition.put(BASE_SCRIPT, splitToLines(((Map)yamlDefinition.get(BASE_SCRIPT)), SCRIPT));
        }
        return InformationPointDTOParser.serialize(yamlDefinition);
    }

    private List<String> splitToLines(final Map definition, final String key) {
        return Splitter.on("\n").omitEmptyStrings().splitToList(definition.get(key).toString());
    }

    private void processUpdate(final String configName, final Map<String, String> informationPoints,
                               final InformationPointService informationPointService) {
        final Map<String, String> cache =
            informationPointsCache.computeIfAbsent(configName, name -> new HashMap<>());
        final MapDifference<String, String> difference = Maps.difference(informationPoints, cache);
        difference.entriesOnlyOnLeft()
                  .forEach((name, value) -> onInformationPointAdded(name, value, informationPointService));
        difference.entriesDiffering()
                  .forEach((name, value) -> onInformationPointModified(name, value.leftValue(), informationPointService));
        difference.entriesOnlyOnRight().forEach((name, value) -> onInformationPointRemoved(name, informationPointService));

        cache.clear();
        cache.putAll(informationPoints);
    }

    private void onInformationPointAdded(final String methodReference, final String informationPointDtoAsJsonString,
                                         final InformationPointService informationPointService) {

        final Optional<InformationPoint> informationPoint =
            InformationPointDTOParser.parse(informationPointDtoAsJsonString, methodReference);
        informationPoint.ifPresent(ip -> {
            informationPointService.addInformationPoint(ip);
            LOG.debug(() -> "Information point " + ip + " added");
        });
    }

    private void onInformationPointModified(final String methodReference, final String informationPointDtoAsJsonString,
                                            final InformationPointService informationPointService) {
        final Optional<InformationPoint> informationPoint =
            InformationPointDTOParser.parse(informationPointDtoAsJsonString, methodReference);
        informationPoint.ifPresent(ip -> {
            informationPointService.removeInformationPoint(ip);
            informationPointService.addInformationPoint(ip);
            LOG.debug(() -> "Information point " + ip + " modified");
        });
    }

    private void onInformationPointRemoved(final String methodReference,
                                           final InformationPointService informationPointService) {
        final InformationPoint informationPoint = InformationPoint.builder().withMethodReference(methodReference).build();
        informationPointService.removeInformationPoint(informationPoint);
        LOG.debug(() -> "Information point " + informationPoint + " removed");
    }

}
