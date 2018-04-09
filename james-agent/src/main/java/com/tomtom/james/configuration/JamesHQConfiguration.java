package com.tomtom.james.configuration;

public interface JamesHQConfiguration {
    // time that HQ sleeps after start - before it starts to work
    long getInitialDelayInMs();
    // how often should make changes in IP
    long getScanPeriodInMs();
    // how often james is checking if there any new objectives
    long getJamesIntervalInMs();
}
