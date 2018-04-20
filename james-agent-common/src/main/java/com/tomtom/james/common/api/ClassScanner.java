package com.tomtom.james.common.api;

import java.util.Collection;

public interface ClassScanner extends StatisticsProvider{

    Collection<String> getIgnoredPackages();

    Collection<String> getClassStructureInfos();

    Collection<String> getProcessedClassesInfos();
}
