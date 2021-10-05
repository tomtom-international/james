package com.tomtom.james.store.io;

import java.util.ArrayList;
import java.util.Collection;
import java.util.ServiceLoader;

public class ConfigParserFactory {

    private final static ConfigParserFactory INSTANCE = new ConfigParserFactory();

    final Collection<ConfigParserWriter> availableParsers;

    public static ConfigParserFactory getInstance() {
        return INSTANCE;
    }

    ConfigParserFactory(){
        availableParsers = new ArrayList();
        ServiceLoader<ConfigParserWriter> serviceLoader = ServiceLoader.load(ConfigParserWriter.class);
        serviceLoader.forEach(this::registerConfigParser);
    }

    protected void registerConfigParser(ConfigParserWriter configParserWriter) {
        availableParsers.add(configParserWriter);
    }

    public ConfigParserWriter getParser(String filePath) {
        return availableParsers.stream().filter(parser -> parser.supportsConfigFile(filePath)).findFirst().orElseThrow(
            () -> new IllegalArgumentException(String.format("Config file: %s has unsuported extension", filePath)));
    }
}
