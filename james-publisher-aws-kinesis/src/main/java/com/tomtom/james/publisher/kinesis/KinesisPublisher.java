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

import software.amazon.kinesis.producer.Attempt;
import software.amazon.kinesis.producer.KinesisProducer;
import software.amazon.kinesis.producer.KinesisProducerConfiguration;
import software.amazon.kinesis.producer.UnexpectedMessageException;
import software.amazon.kinesis.producer.UserRecordFailedException;
import software.amazon.kinesis.producer.UserRecordResult;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.tomtom.james.common.api.configuration.EventPublisherConfiguration;
import com.tomtom.james.common.api.publisher.Event;
import com.tomtom.james.common.api.publisher.EventPublisher;
import com.tomtom.james.common.log.Logger;
import com.tomtom.james.publisher.kinesis.configuration.KinesisPublisherConfiguration;
import com.tomtom.james.publisher.kinesis.configuration.ProducerConfiguration;

import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.function.Supplier;

@SuppressWarnings("unused")
public class KinesisPublisher implements EventPublisher {

    private static final Logger LOG = Logger.getLogger(KinesisPublisher.class);

    private JSONEventFormatter formatter;
    private String stream;
    private Supplier<String> partitionKeySupplier;
    private KinesisProducer producer;
    private int maxMessageSize;
    private final ExecutorService executor;

    public KinesisPublisher() {
        final ThreadFactory
            threadFactory = new ThreadFactoryBuilder()
            .setNameFormat(getId() + "-%d")
            .setDaemon(true)
            .build();
        this.executor = Executors.newSingleThreadExecutor(threadFactory);
    }

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
        maxMessageSize = configuration.getMaxMessageSize();
        producer = createKinesisProducer(configuration);
    }

    @Override
    public void close() {
        if (producer != null) {
            producer.flushSync();
            producer = null;
        }
        executor.shutdownNow();
    }

    @Override
    public void publish(Event evt) {
        if (producer != null) {
            try {
                final String formattedEvent = formatter.format(Objects.requireNonNull(evt));
                ByteBuffer buffer = ByteBuffer.wrap(formattedEvent.getBytes());
                if (maxMessageSize > 0 && buffer.remaining() > maxMessageSize) {
                    LOG.warn("Event is too large to be published to Kinesis. Skipping ...\n " + formattedEvent);
                    return;
                }

                final ListenableFuture<UserRecordResult> future = producer.addUserRecord(stream, partitionKeySupplier.get(), buffer);
                Futures.addCallback(future, new UserRecordCallback(), executor);
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
        pc.getMetricsLevel().ifPresent(kc::setMetricsLevel);
        pc.getMetricsGranularity().ifPresent(kc::setMetricsGranularity);
        pc.getMetricsNamespace().ifPresent(kc::setMetricsNamespace);
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

    private static class UserRecordCallback implements FutureCallback<UserRecordResult> {

        @Override
        public void onFailure(Throwable t) {
            // If we see any failures, we will log them.
            if (t instanceof UserRecordFailedException) {
                int attempts = ((UserRecordFailedException)t).getResult().getAttempts().size() - 1;
                Attempt last = ((UserRecordFailedException)t).getResult().getAttempts().get(attempts);
                if (attempts > 1) {
                    Attempt previous = ((UserRecordFailedException)t).getResult().getAttempts().get(attempts - 1);
                    LOG.error(String.format("Record failed to put - %s : %s. Previous failure - %s : %s (no. attempts: %d)",
                                            last.getErrorCode(), last.getErrorMessage(), previous.getErrorCode(),
                                            previous.getErrorMessage(), attempts));
                } else {
                    LOG.error(String.format("Record failed to put - %s : %s.", last.getErrorCode(), last.getErrorMessage()));
                }
            } else if (t instanceof UnexpectedMessageException) {
                LOG.error("Record failed to put due to unexpected message received from native layer", t);
            }
            LOG.error("Failed to publish event to Kinesis", t);
        }

        @Override
        public void onSuccess(UserRecordResult result) {
        }
    }

}
