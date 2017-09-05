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

package com.tomtom.james.controller.webservice;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;

public class ResponseBuilder {

    private static final String HTTP_HEADER_CONTENT_TYPE = "Content-Type";
    private static final String HTTP_HEADER_SERVER = "Server";

    private final HttpExchange exchange;
    private HTTPContentType contentType;
    private byte[] responseBody;

    private ResponseBuilder(HttpExchange exchange) {
        this.exchange = exchange;
    }

    public static ResponseBuilder forExchange(HttpExchange exchange) {
        return new ResponseBuilder(exchange);
    }

    public ResponseBuilder withResponseBody(byte[] responseBody, HTTPContentType contentType) throws IOException {
        this.responseBody = responseBody;
        this.contentType = contentType;
        return this;
    }

    public void sendResponse(HTTPStatus responseStatus) throws IOException {
        Headers headers = exchange.getResponseHeaders();
        headers.add(HTTP_HEADER_SERVER, "James HTTP Server");
        if (contentType != null) {
            headers.add(HTTP_HEADER_CONTENT_TYPE, contentType.toString());
        }
        exchange.sendResponseHeaders(responseStatus.getCode(), responseBody != null ? responseBody.length : -1L);

        if (responseBody != null) {
            try (OutputStream outputStream = exchange.getResponseBody()) {
                outputStream.write(responseBody);
            }
        }
    }
}
