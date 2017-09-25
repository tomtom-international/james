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

public enum HTTPStatus {

    // 2xx Success
    OK(200),

    // 4xx Client errors
    NOT_FOUND(404),
    METHOD_NOT_ALLOWED(405),

    // 5xx Server errors
    INTERNAL_SERVICE_ERROR(500);

    private final int code;

    HTTPStatus(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
