package com.tomtom.james.configuration;

public interface JamesHQConfiguration {
    // time that HQ sleeps after start - before it starts to work
    long getInitialDelay();
    // how often should make changes in IP
    long getScanPeriod();
    // how often james is checking if there any new objectives
    long getJamesInterval();
}
