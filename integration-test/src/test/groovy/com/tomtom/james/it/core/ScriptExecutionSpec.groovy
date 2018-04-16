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

package com.tomtom.james.it.core

import com.tomtom.james.controller.webservice.api.v1.InformationPointDTO
import com.tomtom.james.it.utils.AppClient
import com.tomtom.james.it.utils.JamesControllerProvider
import com.tomtom.james.it.utils.TestUtils
import com.tomtom.james.it.webservice.*

class ScriptExecutionSpec extends BaseJamesSpecification {

    def jamesController = JamesControllerProvider.get()


    def cleanup() {
        // cleaning existing ip
        def ips = jamesController.getInformationPoints()
        for (ip in ips) {
            jamesController.removeInformationPoint(ip.getClassName(), ip.getMethodName())
        }
        sleep(2000)
    }

    def "Method of subclass, information point on subclass"() {
        given:
        def ip = new InformationPointDTO(
                className: TestService.name,
                methodName: "methodOfSubclass",
                script: TestUtils.scriptLines(ScriptExecutionSpec, "simple")
        )
        def eventsBefore = TestUtils.readPublishedEvents()

        when:
        AppClient.methodOfSubclass()
        jamesController.createInformationPoint(ip)
        sleep(2000)
        def result1 = AppClient.methodOfSubclass()
        def eventsAfterFirstCall = readPublishedEventsWithWait(1)

        jamesController.removeInformationPoint(ip.getClassName(), ip.getMethodName())
        TestUtils.cleanUpEventsFile()

        jamesController.createInformationPoint(ip)
        def result2 = AppClient.methodOfSubclass()
        def eventsAfterSecondCall = readPublishedEventsWithWait(1)

        then:

        eventsBefore.isEmpty()
        eventsAfterFirstCall == [
                [
                        result     : "success",
                        className  : TestService.name,
                        methodName : "methodOfSubclass",
                        "arg(arg0)": "methodOfSubclass-arg0",
                        returnValue: "methodOfSubclass-value"
                ]
        ]
        eventsAfterSecondCall == [
                [
                        result     : "success",
                        className  : TestService.name,
                        methodName : "methodOfSubclass",
                        "arg(arg0)": "methodOfSubclass-arg0",
                        returnValue: "methodOfSubclass-value"
                ]
        ]
        result1 == "methodOfSubclass-value"
        result2 == "methodOfSubclass-value"
    }

    def "Method of superclass, information point on superclass"() {
        given:
        def ip = new InformationPointDTO(
                className: AbstractTestService.name,
                methodName: "methodOfSuperclass",
                script: TestUtils.scriptLines(ScriptExecutionSpec, "simple")
        )
        def eventsBefore = TestUtils.readPublishedEvents()

        when:
        //AppClient.methodOfSuperclass()
        jamesController.createInformationPoint(ip)
        sleep(2000)
        def result = AppClient.methodOfSuperclass()
        def eventsAfter = readPublishedEventsWithWait(1)

        then:
        eventsBefore.isEmpty()
        eventsAfter == [
                [
                        result     : "success",
                        className  : AbstractTestService.name,
                        methodName : "methodOfSuperclass",
                        "arg(arg0)": "methodOfSuperclass-arg0",
                        returnValue: "methodOfSuperclass-value"
                ]
        ]
        result == "methodOfSuperclass-value"
    }

    def "Abstract method of superclass, information point on subclass"() {
        given:
        def ip = new InformationPointDTO(
                className: TestService.name,
                methodName: "abstractMethodOfSuperclass",
                script: TestUtils.scriptLines(ScriptExecutionSpec, "simple")
        )
        def eventsBefore = TestUtils.readPublishedEvents()

        when:
        AppClient.abstractMethodOfSuperclass()
        jamesController.createInformationPoint(ip)
        sleep(2000)
        def result = AppClient.abstractMethodOfSuperclass()
        def eventsAfter = readPublishedEventsWithWait(1)

        then:
        eventsBefore.isEmpty()
        eventsAfter == [
                [
                        result     : "success",
                        className  : TestService.name,
                        methodName : "abstractMethodOfSuperclass",
                        "arg(arg0)": "abstractMethodOfSuperclass-arg0",
                        returnValue: "abstractMethodOfSuperclass-valueFromSubclass"
                ]
        ]
        result == "abstractMethodOfSuperclass-valueFromSubclass"
    }

    def "Abstract method of superclass, information point on superclass"() {
        given:
        def ip = new InformationPointDTO(
                className: AbstractTestService.name,
                methodName: "abstractMethodOfSuperclass",
                script: TestUtils.scriptLines(ScriptExecutionSpec, "simple")
        )
        def eventsBefore = TestUtils.readPublishedEvents()

        when:
        AppClient.abstractMethodOfSuperclass()
        jamesController.createInformationPoint(ip)
        sleep(2000)
        def result = AppClient.abstractMethodOfSuperclass()
        def eventsAfter = readPublishedEventsWithWait(1)

        then:
        eventsBefore.isEmpty()
        eventsAfter == [
                [
                        result     : "success",
                        className  : TestService.name,
                        methodName : "abstractMethodOfSuperclass",
                        "arg(arg0)": "abstractMethodOfSuperclass-arg0",
                        returnValue: "abstractMethodOfSuperclass-valueFromSubclass"
                ]
        ]
        result == "abstractMethodOfSuperclass-valueFromSubclass"
    }

//// FIXME - should it log anything when we set point on the superclass and we call overridden method in subclass ???
//    def "Method of superclass overridden in subclass, information point on superclass"() {
//        given:
//        def ip = new InformationPointDTO(
//                className: AbstractTestService.name,
//                methodName: "methodOfSuperclassOverriddenInSubclass",
//                script: TestUtils.scriptLines(ScriptExecutionSpec, "simple")
//        )
//        def eventsBefore = TestUtils.readPublishedEvents()
//
//        when:
//        AppClient.methodOfSuperclassOverriddenInSubclass()
//        jamesController.createInformationPoint(ip)
//        sleep(2000)
//        def result = AppClient.methodOfSuperclassOverriddenInSubclass()
//        def eventsAfter = readPublishedEventsWithWait(1)
//
//        then:
//
//        eventsBefore.isEmpty()
//        eventsAfter == [
//                [
//                        result     : "success",
//                        className  : TestService.name,
//                        methodName : "methodOfSuperclassOverriddenInSubclass",
//                        "arg(arg0)": "methodOfSuperclassOverriddenInSubclass-arg0",
//                        returnValue: "methodOfSuperclassOverriddenInSubclass-valueFromSubclass"
//                ]
//        ]
//        result == "methodOfSuperclassOverriddenInSubclass-valueFromSubclass"
//    }

    def "Method of superclass overridden in subclass, information point on subclass"() {
        given:
        def ip = new InformationPointDTO(
                className: TestService.name,
                methodName: "methodOfSuperclassOverriddenInSubclass",
                script: TestUtils.scriptLines(ScriptExecutionSpec, "simple")
        )
        def eventsBefore = TestUtils.readPublishedEvents()

        when:
        AppClient.methodOfSuperclassOverriddenInSubclass()
        jamesController.createInformationPoint(ip)
        sleep(2000)
        def result = AppClient.methodOfSuperclassOverriddenInSubclass()
        def eventsAfter = readPublishedEventsWithWait(1)

        then:
        eventsBefore.isEmpty()
        eventsAfter == [
                [
                        result     : "success",
                        className  : TestService.name,
                        methodName : "methodOfSuperclassOverriddenInSubclass",
                        "arg(arg0)": "methodOfSuperclassOverriddenInSubclass-arg0",
                        returnValue: "methodOfSuperclassOverriddenInSubclass-valueFromSubclass"
                ]
        ]
        result == "methodOfSuperclassOverriddenInSubclass-valueFromSubclass"
    }

    def "Method of superclass overridden in subclass and calling superclass, information point on superclass"() {
        given:
        def ip = new InformationPointDTO(
                className: AbstractTestService.name,
                methodName: "methodOfSuperclassOverriddenInSubclassCalledFromSubclass",
                script: TestUtils.scriptLines(ScriptExecutionSpec, "simple")
        )
        def informationPointsBefore = jamesController.informationPoints.size()
        def eventsBefore = TestUtils.readPublishedEvents()

        when:
        jamesController.createInformationPoint(ip)
        sleep(2000)
        def result = AppClient.methodOfSuperclassOverriddenInSubclassCalledFromSubclass()
        def eventsAfter = readPublishedEventsWithWait(1)


        then:
        informationPointsBefore == 0
        eventsBefore.isEmpty()
        eventsAfter.sort() == [
                [
                        result     : "success",
                        className  : AbstractTestService.name,
                        methodName : "methodOfSuperclassOverriddenInSubclassCalledFromSubclass",
                        "arg(arg0)": "arg-from-subclass",
                        returnValue: "methodOfSuperclassOverriddenInSubclassCalledFromSubclass-valueFromSuperclass"
                ]
        ]
        result == "methodOfSuperclassOverriddenInSubclassCalledFromSubclass-valueFromSubclass"
    }


    def "Method of superclass overridden in subclass and calling superclass, information point on subclass"() {
        given:
        def ip = new InformationPointDTO(
                className: TestService.name,
                methodName: "methodOfSuperclassOverriddenInSubclassCalledFromSubclass",
                script: TestUtils.scriptLines(ScriptExecutionSpec, "simple")
        )
        def eventsBefore = TestUtils.readPublishedEvents()
        def informationPointsBefore = jamesController.informationPoints.size()

        when:
        AppClient.methodOfSuperclassOverriddenInSubclassCalledFromSubclass()
        jamesController.createInformationPoint(ip)
        sleep(2000)
        def result = AppClient.methodOfSuperclassOverriddenInSubclassCalledFromSubclass()
        def eventsAfter = readPublishedEventsWithWait(1)

        then:
        informationPointsBefore == 0
        eventsBefore.isEmpty()
        eventsAfter == [
                [
                        result     : "success",
                        className  : TestService.name,
                        methodName : "methodOfSuperclassOverriddenInSubclassCalledFromSubclass",
                        "arg(arg0)": "methodOfSuperclassOverriddenInSubclassCalledFromSubclass-arg0",
                        returnValue: "methodOfSuperclassOverriddenInSubclassCalledFromSubclass-valueFromSubclass"
                ]
        ]
        result == "methodOfSuperclassOverriddenInSubclassCalledFromSubclass-valueFromSubclass"
    }

    def "Overloaded method, variant (arg0:String)"() {
        given:
        def ip = new InformationPointDTO(
                className: TestService.name,
                methodName: "overloadedMethodOfSubclass",
                script: TestUtils.scriptLines(ScriptExecutionSpec, "simple")
        )
        def eventsBefore = TestUtils.readPublishedEvents()

        when:
        AppClient.overloadedMethodOfSubclass_String()
        jamesController.createInformationPoint(ip)
        sleep(2000)
        def result = AppClient.overloadedMethodOfSubclass_String()
        def eventsAfter = readPublishedEventsWithWait(1)

        then:
        eventsBefore.isEmpty()
        eventsAfter == [
                [
                        result     : "success",
                        className  : TestService.name,
                        methodName : "overloadedMethodOfSubclass",
                        "arg(arg0)": "overloadedMethodOfSubclass-arg0",
                        returnValue: "overloadedMethodOfSubclass-valueFor(String)"
                ]
        ]
        result == "overloadedMethodOfSubclass-valueFor(String)"
    }

    def "Overloaded method, variant (arg0:int)"() {
        given:
        def ip = new InformationPointDTO(
                className: TestService.name,
                methodName: "overloadedMethodOfSubclass",
                script: TestUtils.scriptLines(ScriptExecutionSpec, "simple")
        )
        def eventsBefore = TestUtils.readPublishedEvents()
        def informationPointsBefore = jamesController.informationPoints.size()

        when:
        AppClient.overloadedMethodOfSubclass_Int()
        jamesController.createInformationPoint(ip)
        sleep(2000)
        def result = AppClient.overloadedMethodOfSubclass_Int()
        def eventsAfter = readPublishedEventsWithWait(1)

        then:
        informationPointsBefore == 0
        eventsBefore.isEmpty()
        eventsAfter == [
                [
                        result     : "success",
                        className  : TestService.name,
                        methodName : "overloadedMethodOfSubclass",
                        "arg(arg0)": 100,
                        returnValue: "overloadedMethodOfSubclass-valueFor(int)"
                ]
        ]

        result == "overloadedMethodOfSubclass-valueFor(int)"
    }

    def "Overloaded method, variant (arg0:String, arg1:int)"() {
        given:
        def ip = new InformationPointDTO(
                className: TestService.name,
                methodName: "overloadedMethodOfSubclass",
                script: TestUtils.scriptLines(ScriptExecutionSpec, "simple")
        )
        def eventsBefore = TestUtils.readPublishedEvents()

        when:
        AppClient.overloadedMethodOfSubclass_String_Int()
        jamesController.createInformationPoint(ip)
        sleep(2000)
        def result = AppClient.overloadedMethodOfSubclass_String_Int()
        def eventsAfter = readPublishedEventsWithWait(1)

        then:
        eventsBefore.isEmpty()
        eventsAfter == [
                [
                        result     : "success",
                        className  : TestService.name,
                        methodName : "overloadedMethodOfSubclass",
                        "arg(arg0)": "overloadedMethodOfSubclass-arg0",
                        "arg(arg1)": 101,
                        returnValue: "overloadedMethodOfSubclass-valueFor(String,int)"
                ]
        ]
        result == "overloadedMethodOfSubclass-valueFor(String,int)"
    }

// FIXME - should work fine - no idea why does not work
//    def "Method of internal class"() {
//        given:
//        def ip = new InformationPointDTO(
//                className: TestService.InternalClass.name,
//                methodName: "methodOfInternalClass",
//                script: TestUtils.scriptLines(ScriptExecutionSpec, "simple")
//        )
//        def eventsBefore = TestUtils.readPublishedEvents()
//
//        when:
//        AppClient.methodOfInternalClass()
//        jamesController.createInformationPoint(ip)
//        def result = AppClient.methodOfInternalClass()
//        sleep(2000)
//        def eventsAfter = readPublishedEventsWithWait(1)
//
//        then:
//        eventsBefore.isEmpty()
//        eventsAfter == [
//                [
//                        result     : "success",
//                        className  : TestService.InternalClass.name,
//                        methodName : "methodOfInternalClass",
//                        "arg(arg0)": "methodIfInternalClass-arg0",
//                        returnValue: "methodOfInternalClass-value"
//                ]
//        ]
//        result == "methodOfInternalClass-value"
//    }

    def "Method of interface implemented in subclass, information point on subclass"() {
        given:
        def ip = new InformationPointDTO(
                className: TestService.name,
                methodName: "methodOfInterface",
                script: TestUtils.scriptLines(ScriptExecutionSpec, "simple")
        )
        def eventsBefore = TestUtils.readPublishedEvents()

        when:
        AppClient.methodOfInterface()
        jamesController.createInformationPoint(ip)
        sleep(2000)
        def result = AppClient.methodOfInterface()
        def eventsAfter = readPublishedEventsWithWait(1)

        then:
        eventsBefore.isEmpty()
        eventsAfter == [
                [
                        result     : "success",
                        className  : TestService.name,
                        methodName : "methodOfInterface",
                        "arg(arg0)": "methodOfInterface-arg0",
                        returnValue: "methodOfInterface-value"
                ]
        ]
        result == "methodOfInterface-value"
    }

    def "Method of interface implemented in subclass, information point on interface"() {
        given:
        def ip = new InformationPointDTO(
                className: IService.name,
                methodName: "methodOfInterface",
                script: TestUtils.scriptLines(ScriptExecutionSpec, "simple")
        )
        def eventsBefore = TestUtils.readPublishedEvents()

        when:
        AppClient.methodOfInterface()
        jamesController.createInformationPoint(ip)
        sleep(2000)
        def result = AppClient.methodOfInterface()
        def eventsAfter = readPublishedEventsWithWait(1)

        then:
        eventsBefore.isEmpty()
        eventsAfter == [
                [
                        result     : "success",
                        className  : TestService.name,
                        methodName : "methodOfInterface",
                        "arg(arg0)": "methodOfInterface-arg0",
                        returnValue: "methodOfInterface-value"
                ]
        ]
        result == "methodOfInterface-value"
    }

    def "Method of interface implemented in two subclasses, information point on interface"() {
        given:
        def ip = new InformationPointDTO(
                className: IService.name,
                methodName: "methodOfInterface",
                script: TestUtils.scriptLines(ScriptExecutionSpec, "simple")
        )
        def eventsBefore = TestUtils.readPublishedEvents()

        when:
        AppClient.methodOfInterface_twoSubclassesCalled()
        jamesController.createInformationPoint(ip)
        sleep(2000)
        def result = AppClient.methodOfInterface_twoSubclassesCalled()
        def eventsAfter = readPublishedEventsWithWait(2)

        then:
        eventsBefore.isEmpty()
        eventsAfter.sort() == [
                [
                        result     : "success",
                        className  : TestService.name,
                        methodName : "methodOfInterface",
                        "arg(arg0)": "methodOfInterface-arg0",
                        returnValue: "methodOfInterface-value"
                ],
                [
                        result     : "success",
                        className  : TestService2.name,
                        methodName : "methodOfInterface",
                        "arg(arg0)": "methodOfInterface-arg0",
                        returnValue: "methodOfInterface-value2"
                ]
        ].sort()
        result == "methodOfInterface-value, methodOfInterface-value2"
    }

    def "Method with verbose information point, not throwing exception"() {
        given:
        def ip = new InformationPointDTO(
                className: TestServiceThrowingExceptions.name,
                methodName: "doNotThrow",
                script: TestUtils.scriptLines(ScriptExecutionSpec, "verbose")
        )
        def eventsBefore = TestUtils.readPublishedEvents()

        when:
        AppClient.methodNotThrowingAnException()
        jamesController.createInformationPoint(ip)
        sleep(2000)
        def result = AppClient.methodNotThrowingAnException()
        def eventsAfter = readPublishedEventsWithWait(1)

        then:
        eventsBefore.isEmpty()
        eventsAfter == [
                [
                        informationPointClassName : TestServiceThrowingExceptions.name,
                        informationPointMethodName: "doNotThrow",
                        originDeclaringClassName  : TestServiceThrowingExceptions.name,
                        originName                : "doNotThrow",
                        instanceFieldValue        : 7,
                        "arg(arg0)"               : "arg0-value",
                        "arg(arg1)"               : 101,
                        returnValue               : "doNotThrow result"
                ]
        ]
        result == "doNotThrow result"
    }

    def "Method with information point publishing volatile fields, not throwing exception"() {
        given:
        def ip = new InformationPointDTO(
                className: TestServiceThrowingExceptions.name,
                methodName: "doNotThrow",
                script: TestUtils.scriptLines(ScriptExecutionSpec, "volatile")
        )
        def eventsBefore = TestUtils.readPublishedEvents()

        when:
        AppClient.methodNotThrowingAnException()
        jamesController.createInformationPoint(ip)
        sleep(2000)
        def result = AppClient.methodNotThrowingAnException()
        def eventsAfter = readPublishedEventsWithWait(1)

        then:
        eventsBefore.isEmpty()
        eventsBefore.isEmpty()
        eventsAfter.size() == 1
        (eventsAfter[0]["executionTimeNanos"] as Long) > 0
        (eventsAfter[0]["callStack"] as List).size() > 0
        (eventsAfter[0]["currentThreadName"] as String).startsWith("http-nio-8008-exec-")
    }

    def "Method with verbose information point, throwing exception"() {
        given:
        def ip = new InformationPointDTO(
                className: TestServiceThrowingExceptions.name,
                methodName: "doThrow",
                script: TestUtils.scriptLines(ScriptExecutionSpec, "verbose")
        )
        def eventsBefore = TestUtils.readPublishedEvents()

        when:
        AppClient.methodThrowingAnException()
        jamesController.createInformationPoint(ip)
        sleep(2000)
        def result = AppClient.methodThrowingAnException()
        def eventsAfter = readPublishedEventsWithWait(1)

        then:
        eventsBefore.isEmpty()
        eventsAfter == [
                [
                        informationPointClassName : TestServiceThrowingExceptions.name,
                        informationPointMethodName: "doThrow",
                        originDeclaringClassName  : TestServiceThrowingExceptions.name,
                        originName                : "doThrow",
                        instanceFieldValue        : 7,
                        "arg(arg0)"               : "arg0-value",
                        "arg(arg1)"               : 101,
                        errorCauseMessage         : "from doThrow"
                ]
        ]
        result == "from doThrow"
    }

    def "Public static method of subclass, information point on subclass"() {
        given:
        def ip = new InformationPointDTO(
                className: TestService.name,
                methodName: "publicStaticMethod",
                script: TestUtils.scriptLines(ScriptExecutionSpec, "simple")
        )
        def eventsBefore = TestUtils.readPublishedEvents()

        when:
        AppClient.publicStaticMethod()
        jamesController.createInformationPoint(ip)
        sleep(2000)
        def result = AppClient.publicStaticMethod()
        def eventsAfter = readPublishedEventsWithWait(1)

        then:
        eventsBefore.isEmpty()
        eventsAfter == [
                [
                        result     : "success",
                        className  : TestService.name,
                        methodName : "publicStaticMethod",
                        "arg(arg0)": "publicStaticMethod-arg0",
                        returnValue: "publicStaticMethod-value"
                ]
        ]
        result == "publicStaticMethod-value"
    }

    def "Private static method of subclass, information point on subclass"() {
        given:
        def ip = new InformationPointDTO(
                className: TestService.name,
                methodName: "privateStaticMethod",
                script: TestUtils.scriptLines(ScriptExecutionSpec, "simple")
        )
        def eventsBefore = TestUtils.readPublishedEvents()

        when:
        AppClient.privateStaticMethod()
        jamesController.createInformationPoint(ip)
        sleep(2000)
        def result = AppClient.privateStaticMethod()
        def eventsAfter = readPublishedEventsWithWait(1)

        then:
        eventsBefore.isEmpty()
        eventsAfter == [
                [
                        result     : "success",
                        className  : TestService.name,
                        methodName : "privateStaticMethod",
                        "arg(arg0)": "privateStaticMethod-arg0",
                        returnValue: "privateStaticMethod-value"
                ]
        ]
        result == "privateStaticMethod-value"
    }
}
