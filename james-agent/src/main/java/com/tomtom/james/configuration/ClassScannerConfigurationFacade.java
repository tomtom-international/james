package com.tomtom.james.configuration;

import com.tomtom.james.common.api.configuration.StructuredConfiguration;

import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;

public class ClassScannerConfigurationFacade implements ClassScannerConfiguration {
    private static final Long CLASSSCANNER_DEFAULT_INITIAL_DELAY = 10000L;
    private static final Long CLASSSCANNER_DEFAULT_SCAN_PERIOD = 5000L;
    private StructuredConfiguration configuration;

    public ClassScannerConfigurationFacade(StructuredConfiguration structuredConfiguration) {
        this.configuration = structuredConfiguration;
    }

    @Override
    public long getInitialDelayInMs() {
        return configuration.get("initialDelayInMs")
                .map(StructuredConfiguration::asLong)
                .orElse(CLASSSCANNER_DEFAULT_INITIAL_DELAY);
    }

    @Override
    public long getScanPeriodInMs() {
        return configuration.get("scanPeriodInMs")
                .map(StructuredConfiguration::asLong)
                .orElse(CLASSSCANNER_DEFAULT_SCAN_PERIOD);
    }

    @Override
    public Collection<String> getIgnoredPackages() {
        return configuration.get("ignoredPackages")
                .map(StructuredConfiguration::asList)
                .orElseGet(Collections::emptyList)
                .stream()
                .map(StructuredConfiguration::asString)
                .collect(Collectors.toList());
    }
}
