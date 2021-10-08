package com.tomtom.james.store.informationpoints.io;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tomtom.james.common.api.informationpoint.InformationPoint;
import com.tomtom.james.common.log.Logger;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Collectors;


public class PropertiesConfigIO implements ConfigParser, ConfigWriter {

    private static final Logger LOG = Logger.getLogger(PropertiesConfigIO.class);

    private ObjectMapper objectMapper = new ObjectMapper();
    private JavaType informationPointType = objectMapper
        .getTypeFactory()
        .constructType(InformationPointJsonDTO.class);

    @Override
    public boolean supportsConfigFile(final String name) {
        return name.endsWith(".properties");
    }

    @Override
    public Collection<InformationPointDTO> parseConfiguration(final InputStream inStream, ScriptsStore fileScriptStore)
        throws IOException {
        //properties
        final Properties configProperties = new Properties();
        configProperties.load(inStream);
        final List<InformationPointDTO> dtos = configProperties.entrySet().stream().map(entry -> {
                                                                       try {
                                                                           final InformationPointJsonDTO dto =
                                                                               objectMapper.readValue(((String)entry.getValue())
                                                                                   , informationPointType);
                                                                           return Optional.ofNullable(dto).orElse(new InformationPointJsonDTO())
                                                                                          .withMethodReference((String)entry.getKey());
                                                                       } catch (JsonProcessingException e) {
                                                                           e.printStackTrace();
                                                                           return null;
                                                                       }
                                                                   })
                                                                   .map(dto -> dto.processFiles(fileScriptStore))
                                                                   .collect(Collectors.toList());
        return (Collection)dtos;
    }

    @Override
    public void storeConfiguration(final OutputStream outputStream, final Collection<InformationPoint> informationPoints)
        throws IOException {
        Properties properties = new Properties();
        informationPoints.stream().map(InformationPointJsonDTO::new).collect(
                             Collectors.toMap(informationPoint -> informationPoint.getMethodReference(),
                                              informationPointJsonDTO -> {
                                 informationPointJsonDTO.className = null;
                                 informationPointJsonDTO.methodName = null;
                                 return informationPointJsonDTO;
                             })).entrySet()
                         .stream().forEach(entry -> {
                                               try (final ByteArrayOutputStream byteArrayOutputStream =
                                                        new ByteArrayOutputStream()) {
                                                   objectMapper.writeValue(byteArrayOutputStream, entry.getValue());
                                                   final String json = byteArrayOutputStream.toString();
                                                   properties.put(entry.getKey(), json);
                                               } catch (IOException e) {
                                                   LOG.warn("Problem while parsing file!",e);
                                               }
                                           }
                                          );
        properties.store(outputStream, "");
    }
}
