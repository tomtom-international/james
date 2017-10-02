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

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestServiceController {

    private final TestService testService;
    private final TestService2 testService2;
    private final TestServiceThrowingExceptions testServiceThrowingExceptions;

    public TestServiceController(TestService testService,
                                 TestService2 testService2,
                                 TestServiceThrowingExceptions testServiceThrowingExceptions) {
        this.testService = testService;
        this.testService2 = testService2;
        this.testServiceThrowingExceptions = testServiceThrowingExceptions;
    }

    @RequestMapping("/methodOfSubclass")
    public String methodOfSubclass() {
        return testService.methodOfSubclass("methodOfSubclass-arg0");
    }

    @RequestMapping("/otherMethodOfSubclass")
    public String otherMethodOfSubclass() {
        return testService.otherMethodOfSubclass("otherMethodOfSubclass-arg0");
    }

    @RequestMapping("/methodOfSuperclass")
    public String methodOfSuperclass() {
        return testService.methodOfSuperclass("methodOfSuperclass-arg0");
    }

    @RequestMapping("/abstractMethodOfSuperclass")
    public String abstractMethodOfSuperclass() {
        return testService.abstractMethodOfSuperclass("abstractMethodOfSuperclass-arg0");
    }

    @RequestMapping("/methodOfSuperclassOverriddenInSubclass")
    public String methodOfSuperclassOverriddenInSubclass() {
        return testService.methodOfSuperclassOverriddenInSubclass("methodOfSuperclassOverriddenInSubclass-arg0");
    }

    @RequestMapping("/methodOfSuperclassOverriddenInSubclassCalledFromSubclass")
    public String methodOfSuperclassOverriddenInSubclassCalledFromSubclass() {
        return testService.methodOfSuperclassOverriddenInSubclassCalledFromSubclass(
                "methodOfSuperclassOverriddenInSubclassCalledFromSubclass-arg0");
    }

    @RequestMapping("/overloadedMethodOfSubclass_String")
    public String overloadedMethodOfSubclass1() {
        return testService.overloadedMethodOfSubclass("overloadedMethodOfSubclass-arg0");
    }

    @RequestMapping("/overloadedMethodOfSubclass_Int")
    public String overloadedMethodOfSubclass2() {
        return testService.overloadedMethodOfSubclass(100);
    }

    @RequestMapping("/overloadedMethodOfSubclass_String_Int")
    public String overloadedMethodOfSubclass3() {
        return testService.overloadedMethodOfSubclass("overloadedMethodOfSubclass-arg0", 101);
    }

    @RequestMapping("/methodOfInternalClass")
    public String methodOfInternalClass() {
        return testService.getInternalClass().methodOfInternalClass("methodIfInternalClass-arg0");
    }

    @RequestMapping("/methodOfInterface")
    public String methodOfInterface() {
        return testService.methodOfInterface("methodOfInterface-arg0");
    }

    @RequestMapping("/methodOfInterface_twoSubclassesCalled")
    public String methodOfInterface2() {
        String result1 = testService.methodOfInterface("methodOfInterface-arg0");
        String result2 = testService2.methodOfInterface("methodOfInterface-arg0");
        return result1 + ", " + result2;
    }

    @RequestMapping("/methodNotThrowingAnException")
    public String methodNotThrowingAnException() {
        return testServiceThrowingExceptions.doNotThrow("arg0-value", 101);
    }

    @RequestMapping("/methodThrowingAnException")
    public String methodThrowingAnException() {
        try {
            return testServiceThrowingExceptions.doThrow("arg0-value", 101);
        } catch (Throwable t) {
            return t.getMessage();
        }
    }

}
