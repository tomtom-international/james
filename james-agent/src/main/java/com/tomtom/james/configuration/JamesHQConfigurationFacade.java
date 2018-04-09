package com.tomtom.james.configuration;

import com.tomtom.james.common.api.configuration.StructuredConfiguration;

import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;

public class JamesHQConfigurationFacade implements JamesHQConfiguration {
    private static final Long JAMESHQ_DEFAULT_INITIAL_DELAY = 10000L;
    private static final Long JAMES_DEFAULT_INTERVAL = 1000L;
    private static final Long JAMESHQ_DEFAULT_SCAN_PERIOD = 5000L;
    private StructuredConfiguration configuration;

    public JamesHQConfigurationFacade(StructuredConfiguration structuredConfiguration) {
        this.configuration = structuredConfiguration;
    }

    @Override
    public long getInitialDelayInMs() {
        return configuration.get("initialDelayInMs")
                .map(StructuredConfiguration::asLong)
                .orElse(JAMESHQ_DEFAULT_INITIAL_DELAY);
    }

    @Override
    public long getScanPeriodInMs() {
        return configuration.get("scanPeriodInMs")
                .map(StructuredConfiguration::asLong)
                .orElse(JAMESHQ_DEFAULT_SCAN_PERIOD);
    }

    @Override
    public long getJamesIntervalInMs() {
        return configuration.get("jamesIntervalInMs")
                .map(StructuredConfiguration::asLong)
                .orElse(JAMES_DEFAULT_INTERVAL);
    }

}
