package com.tomtom.james.store.informationpoints.io;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.tomtom.james.common.api.informationpoint.InformationPoint;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class JsonConfigIO implements ConfigParser , ConfigWriter{

    ObjectMapper objectMapper = new ObjectMapper();
    CollectionType informationPointType = objectMapper
        .getTypeFactory()
        .constructCollectionType(Collection.class, InformationPointJsonDTO.class);

    @Override
    public boolean supportsConfigFile(final String name) {
        return name.endsWith(".json");
    }

    @Override
    public Collection<InformationPointDTO> parseConfiguration(final InputStream inStream, ScriptsStore fileScriptStore) throws IOException {
        Collection<InformationPointJsonDTO> dtos = objectMapper.readValue(inStream, informationPointType);
        return dtos.stream()
                   .map(dto -> dto.processFiles(fileScriptStore))
                   .collect(java.util.stream.Collectors.toList());
    }

    @Override
    public void storeConfiguration(final OutputStream outputStream, final Collection<InformationPoint> informationPoints)
        throws IOException {
        final List<InformationPointJsonDTO> dtos =
            informationPoints.stream().map(InformationPointJsonDTO::new).collect(Collectors.toList());
        objectMapper.writeValue(outputStream,dtos);
    }
}
