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

package com.tomtom.james.publisher.kinesis.configuration;

import com.tomtom.james.common.api.configuration.EventPublisherConfiguration;
import com.tomtom.james.common.api.configuration.StructuredConfiguration;

import java.util.Optional;

public class KinesisPublisherConfiguration {

    private final StructuredConfiguration configurationProperties;

    public KinesisPublisherConfiguration(EventPublisherConfiguration eventPublisherConfiguration) {
        configurationProperties = eventPublisherConfiguration.getProperties()
                .orElseGet(StructuredConfiguration.Empty::new);
    }

    public String getStream() {
        return configurationProperties.get("stream")
                .map(StructuredConfiguration::asString)
                .orElse("james-kinesis");
    }

    public Integer getMaxMessageSize() {
        return configurationProperties.get("maxMessageSize")
                                      .map(StructuredConfiguration::asInteger)
                                      .orElse(-1);
    }

    public Optional<String> getPartitionKey() {
        return configurationProperties.get("partitionKey")
                .map(StructuredConfiguration::asString);
    }

    public ElasticSearchConfiguration getElasticSearch() {
        return configurationProperties.get("elasticSearch")
                .map(ElasticSearchConfiguration::new)
                .orElseGet(() -> new ElasticSearchConfiguration(new StructuredConfiguration.Empty()));
    }

    public ProducerConfiguration getProducer() {
        return configurationProperties.get("producer")
                .map(ProducerConfiguration::new)
                .orElseGet(() -> new ProducerConfiguration(new StructuredConfiguration.Empty()));
    }


}
