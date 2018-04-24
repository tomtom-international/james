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
import com.tomtom.james.common.api.QueueBacked;
import com.tomtom.james.common.api.publisher.EventPublisher;
import com.tomtom.james.common.api.script.ScriptEngine;
import com.tomtom.james.common.log.Logger;
import com.tomtom.james.controller.webservice.HTTPContentType;
import com.tomtom.james.controller.webservice.HTTPStatus;
import com.tomtom.james.controller.webservice.ResponseBuilder;
import com.tomtom.james.controller.webservice.api.v1.QueueDTO;
import com.tomtom.james.controller.webservice.handlers.AbstractHttpHandler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class QueueHandler extends AbstractHttpHandler {

    private static final Logger LOG = Logger.getLogger(QueueHandler.class);

    private static final String NEW_CLASSES_KEY = "new-classes";
    private static final String NEW_INFORMATION_POINTS_KEY = "new-informationpoints";
    private static final String REMOVE_INFORMATION_POINTS_KEY = "remove-informationpoints";
    private static final String JAMES_OBJECTIVES_KEY = "james-objectives";
    private static final String SCRIPT_ENGINE_KEY = "script-engine";
    private static final String EVENT_PUBLISHER_KEY = "event-publisher";
    private static final String LIST_KEY = "list";

    private final ScriptEngine scriptEngine;
    private final EventPublisher eventPublisher;
    private final QueueBacked newClassesQueue;
    private final QueueBacked newInformationPointsQueue;
    private final QueueBacked removeInformationPointsQueue;
    private final QueueBacked jamesObjectivesQueue;

    public QueueHandler(ScriptEngine scriptEngine, EventPublisher eventPublisher, QueueBacked newClassesQueue, QueueBacked newInformationPointsQueue, QueueBacked removeInformationPointsQueue, QueueBacked jamesObjectivesQueue) {
        this.scriptEngine = scriptEngine;
        this.eventPublisher = eventPublisher;
        this.newClassesQueue = newClassesQueue;
        this.newInformationPointsQueue = newInformationPointsQueue;
        this.removeInformationPointsQueue = removeInformationPointsQueue;
        this.jamesObjectivesQueue = jamesObjectivesQueue;
    }

    @Override
    protected void doHandle(HttpExchange httpExchange, List<String> pathParams) throws IOException {
        if (isGet.test(httpExchange) && pathParams.isEmpty()) {
            Collection<QueueDTO> allQueues = getAllQueues();
            ResponseBuilder.forExchange(httpExchange)
                    .withResponseBody(asJSONBytes(allQueues), HTTPContentType.APPLICATION_JSON)
                    .sendResponse(HTTPStatus.OK);

        } else if (isGet.test(httpExchange) && pathParams.size() == 1 && pathParams.get(0).equals(SCRIPT_ENGINE_KEY)) {
            QueueDTO queue = getScriptEngineQueue();
            ResponseBuilder.forExchange(httpExchange)
                    .withResponseBody(asJSONBytes(queue), HTTPContentType.APPLICATION_JSON)
                    .sendResponse(HTTPStatus.OK);

        } else if (isGet.test(httpExchange) && pathParams.size() == 1 && pathParams.get(0).equals(EVENT_PUBLISHER_KEY)) {
            QueueDTO queue = getEventPublisherQueue();
            ResponseBuilder.forExchange(httpExchange)
                    .withResponseBody(asJSONBytes(queue), HTTPContentType.APPLICATION_JSON)
                    .sendResponse(HTTPStatus.OK);
        } else if (isGet.test(httpExchange) && pathParams.size() == 1 && pathParams.get(0).equals(JAMES_OBJECTIVES_KEY)) {
            QueueDTO queue = getJamesObjectivesQueue();
            ResponseBuilder.forExchange(httpExchange)
                    .withResponseBody(asJSONBytes(queue), HTTPContentType.APPLICATION_JSON)
                    .sendResponse(HTTPStatus.OK);
        } else if (isGet.test(httpExchange) && pathParams.size() == 1 && pathParams.get(0).equals(NEW_CLASSES_KEY)) {
            QueueDTO queue = getNewClassesQueue();
            ResponseBuilder.forExchange(httpExchange)
                    .withResponseBody(asJSONBytes(queue), HTTPContentType.APPLICATION_JSON)
                    .sendResponse(HTTPStatus.OK);
        } else if (isGet.test(httpExchange) && pathParams.size() == 1 && pathParams.get(0).equals(NEW_INFORMATION_POINTS_KEY)) {
            QueueDTO queue = getNewInformationPointsQueue();
            ResponseBuilder.forExchange(httpExchange)
                    .withResponseBody(asJSONBytes(queue), HTTPContentType.APPLICATION_JSON)
                    .sendResponse(HTTPStatus.OK);
        } else if (isGet.test(httpExchange) && pathParams.size() == 1 && pathParams.get(0).equals(REMOVE_INFORMATION_POINTS_KEY)) {
            QueueDTO queue = getRemoveInformationPointsQueue();
            ResponseBuilder.forExchange(httpExchange)
                    .withResponseBody(asJSONBytes(queue), HTTPContentType.APPLICATION_JSON)
                    .sendResponse(HTTPStatus.OK);
        } else if (isGet.test(httpExchange) && pathParams.size() == 1 && pathParams.get(0).equals(LIST_KEY)) {
            String[] data = {SCRIPT_ENGINE_KEY, EVENT_PUBLISHER_KEY, JAMES_OBJECTIVES_KEY, NEW_CLASSES_KEY, NEW_INFORMATION_POINTS_KEY, REMOVE_INFORMATION_POINTS_KEY};
            QueueDTO queue = getRemoveInformationPointsQueue();
            ResponseBuilder.forExchange(httpExchange)
                    .withResponseBody(asJSONBytes(data), HTTPContentType.APPLICATION_JSON)
                    .sendResponse(HTTPStatus.OK);
        } else {
            ResponseBuilder.forExchange(httpExchange)
                    .sendResponse(HTTPStatus.METHOD_NOT_ALLOWED);
        }
    }

    private Collection<QueueDTO> getAllQueues() {
        LOG.trace("Retrieving status of all work queues");
        return Arrays.asList(createQueueDTOForObject(scriptEngine, SCRIPT_ENGINE_KEY),
                createQueueDTOForObject(eventPublisher, EVENT_PUBLISHER_KEY),
                createQueueDTOForObject(newClassesQueue, NEW_CLASSES_KEY),
                createQueueDTOForObject(newInformationPointsQueue, NEW_INFORMATION_POINTS_KEY),
                createQueueDTOForObject(removeInformationPointsQueue, REMOVE_INFORMATION_POINTS_KEY),
                createQueueDTOForObject(jamesObjectivesQueue, JAMES_OBJECTIVES_KEY)

        );
    }

    private QueueDTO getScriptEngineQueue() {
        LOG.trace("Retrieving status of script engine work queue");
        return createQueueDTOForObject(scriptEngine, SCRIPT_ENGINE_KEY);
    }

    private QueueDTO getEventPublisherQueue() {
        LOG.trace("Retrieving status of publisher work queue");
        return createQueueDTOForObject(eventPublisher, EVENT_PUBLISHER_KEY);
    }

    private QueueDTO getNewClassesQueue() {
        LOG.trace("Retrieving newClasses queue");
        return createQueueDTOForObject(newClassesQueue, NEW_CLASSES_KEY);
    }

    private QueueDTO getNewInformationPointsQueue() {
        LOG.trace("Retrieving newClasses queue");
        return createQueueDTOForObject(newInformationPointsQueue, NEW_INFORMATION_POINTS_KEY);
    }

    private QueueDTO getRemoveInformationPointsQueue() {
        LOG.trace("Retrieving newClasses queue");
        return createQueueDTOForObject(removeInformationPointsQueue, REMOVE_INFORMATION_POINTS_KEY);
    }

    private QueueDTO getJamesObjectivesQueue() {
        LOG.trace("Retrieving newClasses queue");
        return createQueueDTOForObject(jamesObjectivesQueue, JAMES_OBJECTIVES_KEY);
    }

    private QueueDTO createQueueDTOForObject(Object object, String queueType) {
        QueueDTO dto = new QueueDTO();
        dto.setQueueType(queueType);
        dto.setQueueSize(object instanceof QueueBacked ? ((QueueBacked) object).getJobQueueSize() : -1);
        dto.setRemainingCapacity(object instanceof QueueBacked ? ((QueueBacked) object).getJobQueueRemainingCapacity() : -1);
        dto.setDroppedItemsCount(object instanceof QueueBacked ? ((QueueBacked) object).getDroppedJobsCount() : -1);
        return dto;
    }
}
