package com.tomtom.james.store.informationpoints.io;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;
import java.util.ServiceLoader;

public class ConfigIOFactory {

    private final static ConfigIOFactory INSTANCE = new ConfigIOFactory();

    final Collection<ConfigParser> availableParsers;
    final Collection<ConfigWriter> availableWriters;

    public static ConfigIOFactory getInstance() {
        return INSTANCE;
    }

    ConfigIOFactory(){
        availableParsers = new ArrayList();
        ServiceLoader<ConfigParser> parserServiceLoader = ServiceLoader.load(ConfigParser.class);
        parserServiceLoader.forEach(this::registerConfigParser);
        availableWriters = new ArrayList();
        ServiceLoader<ConfigWriter> writerServiceLoader = ServiceLoader.load(ConfigWriter.class);
        writerServiceLoader.forEach(this::registerConfigWriter);
    }

    protected void registerConfigParser(ConfigParser configParser) {
        availableParsers.add(configParser);
    }

    protected void registerConfigWriter(ConfigWriter configParser) {
        availableWriters.add(configParser);
    }

    public Optional<ConfigParser> getParser(String filePath) {
        return availableParsers.stream().filter(parser -> parser.supportsConfigFile(filePath)).findFirst();
    }

    public Optional<ConfigWriter> getWriter(String filePath) {
        return availableWriters.stream().filter(parser -> parser.supportsConfigFile(filePath)).findFirst();
    }
}
