package com.tomtom.james.configuration;

import java.util.Collection;

public interface ClassScannerConfiguration {

    long getInitialDelayInMs();
    long getScanPeriodInMs();
    Collection<String> getIgnoredPackages();

}
