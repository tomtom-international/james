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

import com.tomtom.james.common.api.configuration.StructuredConfiguration;

import java.util.Optional;

public class ProducerConfiguration {

    private final StructuredConfiguration configuration;

    ProducerConfiguration(StructuredConfiguration configuration) {
        this.configuration = configuration;
    }

    public Optional<Boolean> isAggregationEnabled() {
        return configuration.get("aggregationEnabled")
                .map(StructuredConfiguration::asBoolean);
    }

    public Optional<Long> getAggregationMaxCount() {
        return configuration.get("aggregationMaxCount")
                .map(StructuredConfiguration::asLong);
    }

    public Optional<Integer> getAggregationMaxSize() {
        return configuration.get("aggregationMaxSize")
                .map(StructuredConfiguration::asInteger);
    }

    public Optional<String> getCloudwatchEndpoint() {
        return configuration.get("cloudwatchEndpoint")
                .map(StructuredConfiguration::asString);
    }

    public Optional<Integer> getCloudwatchPort() {
        return configuration.get("cloudwatchPort")
                .map(StructuredConfiguration::asInteger);
    }

    public Optional<Integer> getCollectionMaxCount() {
        return configuration.get("collectionMaxCount")
                .map(StructuredConfiguration::asInteger);
    }

    public Optional<Long> getCollectionMaxSize() {
        return configuration.get("collectionMaxSize")
                .map(StructuredConfiguration::asLong);
    }

    public Optional<Integer> getConnectTimeout() {
        return configuration.get("connectTimeout")
                .map(StructuredConfiguration::asInteger);
    }

    public Optional<Boolean> isEnableCoreDumps() {
        return configuration.get("enableCoreDumps")
                .map(StructuredConfiguration::asBoolean);
    }

    public Optional<Boolean> isFailIfThrottled() {
        return configuration.get("failIfThrottled")
                .map(StructuredConfiguration::asBoolean);
    }

    public Optional<String> getKinesisEndpoint() {
        return configuration.get("kinesisEndpoint")
                .map(StructuredConfiguration::asString);
    }

    public Optional<Integer> getKinesisPort() {
        return configuration.get("kinesisPort")
                .map(StructuredConfiguration::asInteger);
    }

    public Optional<String> getLogLevel() {
        return configuration.get("logLevel")
                .map(StructuredConfiguration::asString);
    }

    public Optional<Integer> getMaxConnections() {
        return configuration.get("maxConnections")
                .map(StructuredConfiguration::asInteger);
    }

    public Optional<Integer> getMinConnections() {
        return configuration.get("minConnections")
                .map(StructuredConfiguration::asInteger);
    }

    public Optional<String> getNativeExecutable() {
        return configuration.get("nativeExecutable")
                .map(StructuredConfiguration::asString);
    }

    public Optional<Integer> getRateLimit() {
        return configuration.get("rateLimit")
                .map(StructuredConfiguration::asInteger);
    }

    public Optional<Integer> getRecordMaxBufferedTime() {
        return configuration.get("recordMaxBufferedTime")
                .map(StructuredConfiguration::asInteger);
    }

    public Optional<Integer> getRecordTtl() {
        return configuration.get("recordTtl")
                .map(StructuredConfiguration::asInteger);
    }

    public Optional<String> getRegion() {
        return configuration.get("region")
                .map(StructuredConfiguration::asString);
    }

    public Optional<Integer> getRequestTimeout() {
        return configuration.get("requestTimeout")
                .map(StructuredConfiguration::asInteger);
    }

    public Optional<String> getTempDirectory() {
        return configuration.get("tempDirectory")
                .map(StructuredConfiguration::asString);
    }

    public Optional<Boolean> isVerifyCertificate() {
        return configuration.get("verifyCertificate")
                .map(StructuredConfiguration::asBoolean);
    }
}
