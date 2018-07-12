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

package com.tomtom.james.controller.webservice.handlers.v1;

import com.sun.net.httpserver.HttpExchange;
import com.tomtom.james.common.api.informationpoint.InformationPoint;
import com.tomtom.james.common.api.informationpoint.InformationPointService;
import com.tomtom.james.common.api.informationpoint.Metadata;
import com.tomtom.james.common.log.Logger;
import com.tomtom.james.controller.webservice.HTTPContentType;
import com.tomtom.james.controller.webservice.HTTPStatus;
import com.tomtom.james.controller.webservice.ResponseBuilder;
import com.tomtom.james.controller.webservice.api.v1.InformationPointDTO;
import com.tomtom.james.controller.webservice.handlers.AbstractHttpHandler;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class InformationPointHandler extends AbstractHttpHandler {

    private static final Logger LOG = Logger.getLogger(InformationPointHandler.class);

    private final InformationPointService informationPointService;

    public InformationPointHandler(InformationPointService informationPointService) {
        this.informationPointService = informationPointService;
    }

    @Override
    public void doHandle(HttpExchange httpExchange, List<String> pathParams) throws IOException {
        if (isGet.test(httpExchange) && pathParams.isEmpty()) {
            Collection<InformationPointDTO> informationPoints = getInformationPoints();
            ResponseBuilder.forExchange(httpExchange)
                    .withResponseBody(asJSONBytes(informationPoints), HTTPContentType.APPLICATION_JSON)
                    .sendResponse(HTTPStatus.OK);

        } else if (isGet.test(httpExchange) && pathParams.size() == 2) {
            Optional<InformationPointDTO> informationPoint = getInformationPoint(pathParams.get(0), pathParams.get(1));
            if (informationPoint.isPresent()) {
                ResponseBuilder.forExchange(httpExchange)
                        .withResponseBody(asJSONBytes(informationPoint.get()), HTTPContentType.APPLICATION_JSON)
                        .sendResponse(HTTPStatus.OK);
            } else {
                ResponseBuilder.forExchange(httpExchange)
                        .sendResponse(HTTPStatus.NOT_FOUND);
            }

        } else if (isPost.test(httpExchange) && pathParams.isEmpty()) {
            InformationPointDTO dto = readRequestBodyAsJSON(httpExchange, InformationPointDTO.class);
            addInformationPoint(dto);
            ResponseBuilder.forExchange(httpExchange)
                    .sendResponse(HTTPStatus.OK);

        } else if (isDelete.test(httpExchange) && pathParams.size() == 2) {
            removeInformationPoint(pathParams.get(0), pathParams.get(1));
            ResponseBuilder.forExchange(httpExchange)
                    .sendResponse(HTTPStatus.OK);

        } else {
            ResponseBuilder.forExchange(httpExchange)
                    .sendResponse(HTTPStatus.METHOD_NOT_ALLOWED);
        }
    }

    private Collection<InformationPointDTO> getInformationPoints() {
        LOG.trace("Retrieving information points");
        return informationPointService
                .getInformationPoints()
                .stream()
                .map(this::createDTO)
                .collect(Collectors.toList());
    }

    private Optional<InformationPointDTO> getInformationPoint(String className, String methodName) {
        LOG.trace(() -> "Retrieving information point " + className + "#" + methodName);
        return informationPointService
                .getInformationPoint(className, methodName)
                .map(this::createDTO);
    }

    private void addInformationPoint(InformationPointDTO dto) {
        LOG.trace(() -> "Adding information point " + dto.getClassName() + "#" + dto.getMethodName());
        informationPointService.addInformationPoint(createInformationPoint(dto));
    }

    private void removeInformationPoint(String className, String methodName) {
        LOG.trace(() -> "Removing information point " + className + "#" + methodName);
        InformationPoint pointRef = InformationPoint.builder()
                .withClassName(className)
                .withMethodName(methodName)
                .build();
        informationPointService.removeInformationPoint(pointRef);
    }

    private InformationPointDTO createDTO(InformationPoint informationPoint) {
        InformationPointDTO dto = new InformationPointDTO();
        dto.setClassName(informationPoint.getClassName());
        dto.setMethodName(informationPoint.getMethodName());
        dto.setScript(informationPoint.splittedScriptLines());
        dto.setSampleRate(informationPoint.getSampleRate());
        dto.setMetadata(informationPoint.getMetadata());
        return dto;
    }

    private InformationPoint createInformationPoint(InformationPointDTO dto) {
        return InformationPoint.builder()
                .withClassName(dto.getClassName())
                .withMethodName(dto.getMethodName())
                .withScript(joinedScriptLines(dto.getScript()))
                .withSampleRate(dto.getSampleRate())
                .withMetadata(dto.getMetadata())
                .build();
    }

    private String joinedScriptLines(List<String> scriptLines) {
        return scriptLines != null ? String.join("\n", scriptLines) : null;
    }
}
