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

package com.tomtom.james.it.webservice;

import org.springframework.stereotype.Service;

@Service
public class TestServiceThrowingExceptions {

    private final int field = 7;

    public String doNotThrow(String arg0, int arg1) {
        return "doNotThrow result";
    }

    public String doThrow(String arg0, int arg1) {
        throw new RuntimeException("from doThrow");
    }

    public String anotherDoNotThrow(String arg0, int arg1) { return "anotherDoNotThrow result"; }

    public String anotherDoNotThrow(String arg0, int arg1, String arg2) {
        return anotherDoNotThrow(arg0, arg1) + ":" + arg2;
    }

    public String anotherDoThrow(String arg0, int arg1) {
        throw new RuntimeException("from anotherDoThrow");
    }

    public String anotherDoThrow(String arg0, int arg1, String arg2) {
        try {
            return anotherDoThrow(arg0, arg1);
        } catch (Exception ex) {
            throw new RuntimeException(ex.getMessage() + ":" + arg2, ex);
        }
    }
}
