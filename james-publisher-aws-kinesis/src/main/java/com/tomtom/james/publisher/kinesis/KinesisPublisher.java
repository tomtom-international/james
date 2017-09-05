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

package com.tomtom.james.publisher.kinesis;

import com.amazonaws.services.kinesis.producer.KinesisProducer;
import com.amazonaws.services.kinesis.producer.KinesisProducerConfiguration;
import com.tomtom.james.common.api.configuration.EventPublisherConfiguration;
import com.tomtom.james.common.api.publisher.Event;
import com.tomtom.james.common.api.publisher.EventPublisher;
import com.tomtom.james.common.log.Logger;
import com.tomtom.james.publisher.kinesis.configuration.KinesisPublisherConfiguration;
import com.tomtom.james.publisher.kinesis.configuration.ProducerConfiguration;

import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;

@SuppressWarnings("unused")
public class KinesisPublisher implements EventPublisher {

    private static final Logger LOG = Logger.getLogger(KinesisPublisher.class);

    private JSONEventFormatter formatter;
    private String stream;
    private Supplier<String> partitionKeySupplier;
    private KinesisProducer producer;

    @Override
    public String getId() {
        return "james.publisher.kinesis";
    }

    @Override
    public void initialize(EventPublisherConfiguration eventPublisherConfiguration) {
        KinesisPublisherConfiguration configuration = new KinesisPublisherConfiguration(eventPublisherConfiguration);
        formatter = new JSONEventFormatter(configuration.getElasticSearch());
        stream = configuration.getStream();
        partitionKeySupplier = () -> configuration.getPartitionKey().orElse(UUID.randomUUID().toString());
        producer = createKinesisProducer(configuration);
    }

    @Override
    public void close() {
        if (producer != null) {
            producer.flushSync();
            producer = null;
        }
    }

    @Override
    public void publish(Event evt) {
        if (producer != null) {
            try {
                ByteBuffer buffer = ByteBuffer.wrap(formatter.format(Objects.requireNonNull(evt)).getBytes());
                producer.addUserRecord(stream, partitionKeySupplier.get(), buffer);
            } catch (Exception e) {
                LOG.warn("Failed to publish event to Kinesis", e);
            }
        }

    }

    private KinesisProducer createKinesisProducer(KinesisPublisherConfiguration configuration) {
        ProducerConfiguration pc = configuration.getProducer();
        KinesisProducerConfiguration kc = new KinesisProducerConfiguration();

        pc.isAggregationEnabled().ifPresent(kc::setAggregationEnabled);
        pc.getAggregationMaxCount().ifPresent(kc::setAggregationMaxCount);
        pc.getAggregationMaxSize().ifPresent(kc::setAggregationMaxSize);
        pc.getCloudwatchEndpoint().ifPresent(kc::setCloudwatchEndpoint);
        pc.getCloudwatchPort().ifPresent(kc::setCloudwatchPort);
        pc.getCollectionMaxCount().ifPresent(kc::setCollectionMaxCount);
        pc.getCollectionMaxSize().ifPresent(kc::setCollectionMaxSize);
        pc.getConnectTimeout().ifPresent(kc::setConnectTimeout);
        pc.isEnableCoreDumps().ifPresent(kc::setEnableCoreDumps);
        pc.isFailIfThrottled().ifPresent(kc::setFailIfThrottled);
        pc.getKinesisEndpoint().ifPresent(kc::setKinesisEndpoint);
        pc.getKinesisPort().ifPresent(kc::setKinesisPort);
        pc.getLogLevel().ifPresent(kc::setLogLevel);
        pc.getMaxConnections().ifPresent(kc::setMaxConnections);
        pc.getMinConnections().ifPresent(kc::setMinConnections);
        pc.getNativeExecutable().ifPresent(kc::setNativeExecutable);
        pc.getRateLimit().ifPresent(kc::setRateLimit);
        pc.getRecordMaxBufferedTime().ifPresent(kc::setRecordMaxBufferedTime);
        pc.getRecordTtl().ifPresent(kc::setRecordTtl);
        pc.getRegion().ifPresent(kc::setRegion);
        pc.getRequestTimeout().ifPresent(kc::setRequestTimeout);
        pc.getTempDirectory().ifPresent(kc::setTempDirectory);
        pc.isVerifyCertificate().ifPresent(kc::setVerifyCertificate);

        try {
            return new KinesisProducer(kc);
        } catch (Exception e) {
            LOG.error("Failed to initialize " + KinesisPublisher.class.getSimpleName(), e);
            return null;
        }
    }

}
