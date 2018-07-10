/*
 * Copyright 2017 TomTom International B.V.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tomtom.james.controller.consul;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tomtom.james.common.api.informationpoint.InformationPoint;
import com.tomtom.james.common.api.informationpoint.Metadata;
import com.tomtom.james.common.log.Logger;
import com.tomtom.james.controller.consul.dto.InformationPointDTOv1;
import com.tomtom.james.controller.consul.dto.Versioned;

import java.io.IOException;
import java.util.Optional;

class InformationPointDTOParser {

    private static final Logger LOG = Logger.getLogger(InformationPointDTOParser.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    static Optional<InformationPoint> parse(String json, String methodReference) {
        try {
            int dtoVersion = MAPPER.readValue(json, Versioned.class).getVersion();
            switch (dtoVersion) {
                case 1:
                    return Optional.of(parseV1(json, methodReference));
                default:
                    LOG.error("Unsupported information point DTO version: " + dtoVersion);
                    return Optional.empty();
            }
        } catch (IOException e) {
            LOG.error("Error parsing information point JSON", e);
            return Optional.empty();
        }
    }

    private static InformationPoint parseV1(String json, String methodReference) throws IOException {
        InformationPointDTOv1 dto = MAPPER.readValue(json, InformationPointDTOv1.class);
        return InformationPoint.builder()
                .withMethodReference(methodReference)
                .withScript(dto.getScript() != null ? String.join("\n", dto.getScript()) : null)
                .withSampleRate(dto.getSampleRate())
                .withMetadata(Metadata.OWNER, dto.getOwner())
                .withMetadata(Metadata.ELASTIC_SEARCH_INDEX, dto.getIndex())
                .build();
    }

}
