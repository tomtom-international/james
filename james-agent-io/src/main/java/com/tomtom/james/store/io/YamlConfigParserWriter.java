package com.tomtom.james.store.io;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.MapType;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.tomtom.james.common.api.informationpoint.InformationPoint;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;

public class YamlConfigParserWriter implements ConfigParserWriter {

    ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
    MapType informationPointType = objectMapper
        .getTypeFactory()
        .constructMapType(Map.class, String.class, InformationPointYamlDTO.class);

    @Override
    public boolean supportsConfigFile(final String name) {
        return name.endsWith(".yaml");
    }

    @Override
    public Collection<InformationPointDTO> parseConfiguration(final InputStream inStream, ScriptsStore fileScriptStore) throws IOException {
        final Map<String,InformationPointYamlDTO> dtosMap = objectMapper.readValue(inStream, informationPointType);
        Collection<InformationPointDTO> dtos =
            dtosMap.entrySet()
                   .stream()
                   .map(entry ->
                            Optional.ofNullable(entry.getValue())
                                    .orElse(new InformationPointYamlDTO())
                                    .withMethodReference(entry.getKey()))
                   .map(dto -> dto.processFiles(fileScriptStore))
                   .collect(
                       java.util.stream.Collectors.toList());
        return dtos;
    }

    @Override
    public void storeConfiguration(final OutputStream outputStream, final Collection<InformationPoint> informationPoints)
        throws IOException {
        //TODO
    }

}
