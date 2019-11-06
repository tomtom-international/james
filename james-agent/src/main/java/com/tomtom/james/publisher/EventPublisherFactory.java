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

package com.tomtom.james.publisher;

import com.google.common.collect.Iterables;
import com.tomtom.james.agent.PluginManager;
import com.tomtom.james.common.api.configuration.ConfigurationStructureException;
import com.tomtom.james.common.api.configuration.EventPublisherConfiguration;
import com.tomtom.james.common.api.publisher.EventPublisher;
import com.tomtom.james.common.log.Logger;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class EventPublisherFactory {

    private static final Logger LOG = Logger.getLogger(EventPublisherFactory.class);

    private EventPublisherFactory() {
    }

    public static EventPublisher create(PluginManager pluginManager,
                                        Collection<EventPublisherConfiguration> eventPublisherConfigurations) {
        List<EventPublisher> publishers = eventPublisherConfigurations.stream()
                .map(configuration -> createAndInitializeEventPublisher(pluginManager, configuration))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
        switch (publishers.size()) {
            case 0:
                throw new ConfigurationStructureException("No event publishers defined, check configuration");
            case 1:
                return Iterables.getOnlyElement(publishers);
            default:
                return new CompositePublisher(publishers);
        }
    }

    private static Optional<EventPublisher> createAndInitializeEventPublisher(PluginManager pluginManager,
                                                                              EventPublisherConfiguration configuration) {
        Optional<EventPublisher> eventPublisher = pluginManager.createEventPublisherPluginInstance(configuration);
        if (eventPublisher.isPresent()) {
            LOG.trace(() -> "Loaded event publisher plugin " + configuration.getId());
            eventPublisher.get().initialize(configuration);
            String threadPoolNameFormat = configuration.getId() + "-thread-pool-%d";
            if(configuration.useDisruptor()){

                EventPublisher asyncWrapper = new DisruptorAsyncPublisher(eventPublisher.get(),
                        threadPoolNameFormat,
                        configuration.getAsyncWorkers(),
                        configuration.getMaxAsyncJobQueueCapacity());
                return Optional.of(asyncWrapper);
            }
            else{
                EventPublisher asyncWrapper = new AsyncPublisher(eventPublisher.get(),
                        threadPoolNameFormat,
                        configuration.getAsyncWorkers(),
                        configuration.getMaxAsyncJobQueueCapacity());
                return Optional.of(asyncWrapper);
            }
        } else {
            LOG.warn(() -> "Error loading event publisher " + configuration.getId() + ", plugin not found");
            return Optional.empty();
        }
    }
}
