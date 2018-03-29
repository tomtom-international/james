package com.tomtom.james.configuration;

import com.tomtom.james.common.api.configuration.StructuredConfiguration;

import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;

public class ClassScannerConfigurationFacade implements ClassScannerConfiguration {
    private static final Long CLASSSCANNER_DEFAULT_INITIAL_DELAY = 10000L; // FIXME where should be this value defined ? maybe in JVMAgent ????
    private static final Long CLASSSCANNER_DEFAULT_SCAN_PERIOD = 5000L; // FIXME where should be this value defined ? maybe in JVMAgent ????
    private StructuredConfiguration configuration;

    public ClassScannerConfigurationFacade(StructuredConfiguration structuredConfiguration) {
        this.configuration = structuredConfiguration;
    }

    @Override
    public long getInitialDelay() {
        return configuration.get("initialDelay")
                .map(StructuredConfiguration::asLong)
                .orElse(CLASSSCANNER_DEFAULT_INITIAL_DELAY);
    }

    @Override
    public long getScanPeriod() {
        return configuration.get("scanPeriod")
                .map(StructuredConfiguration::asLong)
                .orElse(CLASSSCANNER_DEFAULT_SCAN_PERIOD);
    }

    @Override
    public Collection<String> getIgnoredPackages() {
        return configuration.get("classScanner.ignoredPackages")
                .map(StructuredConfiguration::asList)
                .orElse(Collections.emptyList())
                .stream()
                .map(StructuredConfiguration::asString)
                .collect(Collectors.toList());
    }
}
