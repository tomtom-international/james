package com.tomtom.james.configuration;

import java.util.Collection;

public interface ClassScannerConfiguration {

    long getInitialDelay();
    long getScanPeriod();
    Collection<String> getIgnoredPackages();

}
