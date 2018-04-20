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

package com.tomtom.james.controller.consul;

import com.boundary.config.ConsulWatchedConfigurationSource;
import com.ecwid.consul.v1.ConsulClient;
import com.netflix.config.ConcurrentCompositeConfiguration;
import com.netflix.config.ConfigurationManager;
import com.netflix.config.DynamicWatchedConfiguration;
import com.tomtom.james.common.api.ClassScanner;
import com.tomtom.james.common.api.QueueBacked;
import com.tomtom.james.common.api.configuration.JamesControllerConfiguration;
import com.tomtom.james.common.api.controller.JamesController;
import com.tomtom.james.common.api.informationpoint.InformationPoint;
import com.tomtom.james.common.api.informationpoint.InformationPointService;
import com.tomtom.james.common.api.publisher.EventPublisher;
import com.tomtom.james.common.api.script.ScriptEngine;
import com.tomtom.james.common.log.Logger;
import org.apache.commons.configuration.AbstractConfiguration;
import org.apache.commons.configuration.event.ConfigurationEvent;

import java.util.Optional;

@SuppressWarnings("unused")
public class ConsulController implements JamesController {

    private static final Logger LOG = Logger.getLogger(ConsulController.class);

    @Override
    public String getId() {
        return "james.controller.consul";
    }

    @Override
    public void initialize(JamesControllerConfiguration jamesControllerConfiguration,
                           InformationPointService informationPointService,
                           ClassScanner classScanner,
                           ScriptEngine scriptEngine,
                           EventPublisher eventPublisher,
                           QueueBacked jamesObjectiveQueue,
                           QueueBacked newClassesQueue,
                           QueueBacked newInformationPointQueue,
                           QueueBacked removeInformationPointQueue) {
        ConsulControllerConfiguration configuration = new ConsulControllerConfiguration(jamesControllerConfiguration);
        setupConsulWatcher(configuration, informationPointService);
        LOG.trace(() -> "Consul controller started, watching " + configuration.getFolderPath() + " at "
                + configuration.getHost() + ":" + configuration.getPort());
    }

    @Override
    public void close() {
        // do nothing
    }

    private void setupConsulWatcher(ConsulControllerConfiguration configuration,
                                    InformationPointService informationPointService) {
        ConsulClient client = new ConsulClient(configuration.getHost(), configuration.getPort());
        ConsulWatchedConfigurationSource configurationSource =
                new ConsulWatchedConfigurationSource(configuration.getFolderPath(), client);
        DynamicWatchedConfiguration dynamicConfig = new DynamicWatchedConfiguration(configurationSource);

        dynamicConfig.addConfigurationListener(event -> {
            if (!event.isBeforeUpdate()) {
                switch (event.getType()) {
                    case AbstractConfiguration.EVENT_ADD_PROPERTY:
                        onInformationPointAdded(event, informationPointService);
                        break;
                    case AbstractConfiguration.EVENT_SET_PROPERTY:
                        onInformationPointModified(event, informationPointService);
                        break;
                    case AbstractConfiguration.EVENT_CLEAR_PROPERTY:
                        onInformationPointRemoved(event, informationPointService);
                        break;
                    case AbstractConfiguration.EVENT_CLEAR:
                        onInformationPointsCleared(informationPointService);
                        break;
                    default:
                        LOG.debug(() -> "Unsupported event type: " + event.getType());
                }
            }
        });
        configurationSource.startAsync();

        ConcurrentCompositeConfiguration compositeConfig = new ConcurrentCompositeConfiguration();
        compositeConfig.addConfiguration(dynamicConfig, "consul-dynamic");
        ConfigurationManager.install(compositeConfig);
    }

    private void onInformationPointAdded(ConfigurationEvent event, InformationPointService informationPointService) {
        String methodReference = readMethodReference(event);
        String informationPointDtoAsJsonString = (String) event.getPropertyValue();

        Optional<InformationPoint> informationPoint =
                InformationPointDTOParser.parse(informationPointDtoAsJsonString, methodReference);
        informationPoint.ifPresent(ip -> {
            informationPointService.addInformationPoint(ip);
            LOG.trace(() -> "Information point " + ip + " added");
        });
    }

    private void onInformationPointModified(ConfigurationEvent event, InformationPointService informationPointService) {
        String methodReference = readMethodReference(event);
        String informationPointDtoAsJsonString = (String) event.getPropertyValue();

        Optional<InformationPoint> informationPoint =
                InformationPointDTOParser.parse(informationPointDtoAsJsonString, methodReference);
        informationPoint.ifPresent(ip -> {
            informationPointService.removeInformationPoint(ip);
            informationPointService.addInformationPoint(ip);
            LOG.trace(() -> "Information point " + ip + " modified");
        });
    }

    private void onInformationPointRemoved(ConfigurationEvent event, InformationPointService informationPointService) {
        String methodReference = readMethodReference(event);

        InformationPoint informationPoint = InformationPoint.builder().withMethodReference(methodReference).build();
        informationPointService.removeInformationPoint(informationPoint);
        LOG.trace(() -> "Information point " + informationPoint + " removed");
    }

    private void onInformationPointsCleared(InformationPointService informationPointService) {
        informationPointService.getInformationPoints()
                .forEach(informationPointService::removeInformationPoint);

        LOG.trace("All information points removed");
    }

    private String readMethodReference(ConfigurationEvent event) {
        return event.getPropertyName().replace('!', '#');
    }
}
