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

package com.tomtom.james.controller.webservice.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tomtom.james.controller.webservice.api.v1.InformationPointDTO;
import okhttp3.*;

import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

public class JamesControllerClient {

    public static final MediaType MEDIA_TYPE_JSON = MediaType.parse("application/json; charset=utf-8");

    private final ObjectMapper mapper = new ObjectMapper();
    private final OkHttpClient client;
    private final String baseURL;

    public JamesControllerClient(String baseURL) {
        this.baseURL = baseURL;
        this.client = new OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(5, TimeUnit.SECONDS)
                .build();
    }

    public Collection<InformationPointDTO> getInformationPoints() throws IOException {
        Request request = new Request.Builder().url(baseURL + "/v1/information-point").build();
        Response response = client.newCall(request).execute();
        assertResponseSuccessful(response);
        return readResponseBody(response, new TypeReference<Collection<InformationPointDTO>>() {
        });
    }

    public void createInformationPoint(String payload) throws IOException {
        Request request = new Request.Builder()
                .post(RequestBody.create(MEDIA_TYPE_JSON, payload))
                .url(baseURL + "/v1/information-point")
                .build();
        Response response = client.newCall(request).execute();
        assertResponseSuccessful(response);
    }

    public void createInformationPoint(InformationPointDTO ip) throws IOException {
        createInformationPoint(mapper.writeValueAsString(ip));
    }

    public void removeInformationPoint(String className, String methodName) throws IOException {
        Request request = new Request.Builder()
                .delete()
                .url(baseURL + "/v1/information-point/" + className + "/" + methodName)
                .build();
        Response response = client.newCall(request).execute();
        assertResponseSuccessful(response);
    }

    private <T> T readResponseBody(Response response, TypeReference<T> valueTypeRef) throws IOException {
        try (ResponseBody body = response.body()) {
            return mapper.readValue(body.source().inputStream(), valueTypeRef);
        }
    }

    private <T> T readResponseBody(Response response, Class<T> klass) throws IOException {
        try (ResponseBody body = response.body()) {
            return mapper.readValue(body.source().inputStream(), klass);
        }
    }

    private void assertResponseSuccessful(Response response) throws IOException {
        if (!response.isSuccessful()) {
            throw new IOException("Controller returned HTTP " + response.code());
        }
    }
}
