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
    public long getInitialDelay() {
        return configuration.get("initialDelay")
                .map(StructuredConfiguration::asLong)
                .orElse(JAMESHQ_DEFAULT_INITIAL_DELAY);
    }

    @Override
    public long getScanPeriod() {
        return configuration.get("scanPeriod")
                .map(StructuredConfiguration::asLong)
                .orElse(JAMESHQ_DEFAULT_SCAN_PERIOD);
    }

    @Override
    public long getJamesInterval() {
        return configuration.get("jamesInterval")
                .map(StructuredConfiguration::asLong)
                .orElse(JAMES_DEFAULT_INTERVAL);
    }

}
