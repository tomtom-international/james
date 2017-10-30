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

package com.tomtom.james.it.utils

import org.springframework.web.client.RestTemplate

class AppClient {

    private static def restTemplate = new RestTemplate()
    private static def baseURL = "http://localhost:8008"

    static def methodOfSubclass() {
        return restTemplate.getForObject(baseURL + "/methodOfSubclass", String)
    }

    static def methodOfSuperclass() {
        return restTemplate.getForObject(baseURL + "/methodOfSuperclass", String)
    }

    static def abstractMethodOfSuperclass() {
        return restTemplate.getForObject(baseURL + "/abstractMethodOfSuperclass", String)
    }

    static def methodOfSuperclassOverriddenInSubclass() {
        return restTemplate.getForObject(baseURL + "/methodOfSuperclassOverriddenInSubclass", String)
    }

    static def methodOfSuperclassOverriddenInSubclassCalledFromSubclass() {
        return restTemplate.getForObject(baseURL + "/methodOfSuperclassOverriddenInSubclassCalledFromSubclass", String)
    }

    static def overloadedMethodOfSubclass_String() {
        return restTemplate.getForObject(baseURL + "/overloadedMethodOfSubclass_String", String)
    }

    static def overloadedMethodOfSubclass_Int() {
        return restTemplate.getForObject(baseURL + "/overloadedMethodOfSubclass_Int", String)
    }

    static def overloadedMethodOfSubclass_String_Int() {
        return restTemplate.getForObject(baseURL + "/overloadedMethodOfSubclass_String_Int", String)
    }

    static def methodOfInternalClass() {
        return restTemplate.getForObject(baseURL + "/methodOfInternalClass", String)
    }

    static def methodOfInterface() {
        return restTemplate.getForObject(baseURL + "/methodOfInterface", String)
    }

    static def methodOfInterface_twoSubclassesCalled() {
        return restTemplate.getForObject(baseURL + "/methodOfInterface_twoSubclassesCalled", String)
    }

    static def methodNotThrowingAnException() {
        return restTemplate.getForObject(baseURL + "/methodNotThrowingAnException", String)
    }

    static def methodThrowingAnException() {
        return restTemplate.getForObject(baseURL + "/methodThrowingAnException", String)
    }

    static def publicStaticMethod() {
        return restTemplate.getForObject(baseURL + "/publicStaticMethod", String)
    }

    static def privateStaticMethod() {
        return restTemplate.getForObject(baseURL + "/privateStaticMethod", String)
    }

}
