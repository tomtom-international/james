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

package com.tomtom.james.controller.webservice.handlers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.tomtom.james.common.log.Logger;
import com.tomtom.james.controller.webservice.HTTPContentType;
import com.tomtom.james.controller.webservice.HTTPStatus;
import com.tomtom.james.controller.webservice.ResponseBuilder;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public abstract class AbstractHttpHandler implements HttpHandler {

    private static final Logger LOG = Logger.getLogger(AbstractHttpHandler.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    protected static final Predicate<HttpExchange> isGet = e -> e.getRequestMethod().equals("GET");
    protected static final Predicate<HttpExchange> isPost = e -> e.getRequestMethod().equals("POST");
    protected static final Predicate<HttpExchange> isPut = e -> e.getRequestMethod().equals("PUT");
    protected static final Predicate<HttpExchange> isDelete = e -> e.getRequestMethod().equals("DELETE");

    protected abstract void doHandle(HttpExchange httpExchange, List<String> pathParams) throws IOException;

    @Override
    public void handle(HttpExchange httpExchange) throws IOException {
        try {
            doHandle(httpExchange, pathParams(httpExchange));
        } catch (Throwable t) {
            try {
                StringWriter writer = new StringWriter();
                writer.write("<h1>500 Internal Service Error</h1>");
                t.printStackTrace(new PrintWriter(writer));
                ResponseBuilder.forExchange(httpExchange)
                        .withResponseBody(writer.toString().getBytes(), HTTPContentType.TEXT_PLAIN)
                        .sendResponse(HTTPStatus.INTERNAL_SERVICE_ERROR);
            } catch (Throwable t2) {
                LOG.error(() -> "Error sending handler exception", t2);
                LOG.error(() -> "Root cause", t);
            }
        } finally {
            httpExchange.close();
        }
    }

    protected static String readRequestBodyAsString(HttpExchange exchange) throws IOException {
        InputStreamReader reader = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8);
        return new BufferedReader(reader).lines().collect(Collectors.joining("\n"));
    }

    protected static <T> T readRequestBodyAsJSON(HttpExchange exchange, Class<T> klass) throws IOException {
        return OBJECT_MAPPER.readValue(exchange.getRequestBody(), klass);
    }

    protected static byte[] asJSONBytes(Object value) throws IOException {
        return OBJECT_MAPPER.writeValueAsBytes(value);
    }

    private List<String> pathParams(HttpExchange httpExchange) {
        int contextPathLength = httpExchange.getHttpContext().getPath().length();
        String params = httpExchange.getRequestURI().getPath().substring(contextPathLength);
        if (params.isEmpty()) {
            return Collections.emptyList();
        } else {
            params = params.startsWith("/") ? params.substring(1) : params;
            return Arrays.asList(params.split("/"));
        }
    }
}
