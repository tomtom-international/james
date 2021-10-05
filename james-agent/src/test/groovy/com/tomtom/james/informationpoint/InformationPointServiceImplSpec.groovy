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

package com.tomtom.james.informationpoint

import com.tomtom.james.common.api.informationpoint.InformationPoint
import com.tomtom.james.newagent.tools.InformationPointQueue
import com.tomtom.james.store.io.InformationPointStore
import spock.lang.Specification

class InformationPointServiceImplSpec extends Specification {

    def store = Mock(InformationPointStore)
    def newInformationPointQueue = Mock(InformationPointQueue)
    def removeInformationPointQueue = Mock(InformationPointQueue)

    def "Should add information point"() {
        given:
        def informationPoint = createInformationPoint()
        store.restore() >> []
        def service = new InformationPointServiceImpl(store, newInformationPointQueue, removeInformationPointQueue)

        when:
        service.addInformationPoint(informationPoint)

        then:
        true
        1 * newInformationPointQueue.add(informationPoint)
        1 * store.store({ it.contains(informationPoint) })
        service.getInformationPoint("class-name", "method-name").get() == informationPoint
    }


    def "Should remove information point"() {
        given:
        def informationPoint = createInformationPoint()
        store.restore() >> [informationPoint]
        def service = new InformationPointServiceImpl(store, newInformationPointQueue, removeInformationPointQueue)

        when:
        service.removeInformationPoint(informationPoint)

        then:
        true
        1 * store.store({ it.isEmpty() })
        !service.getInformationPoint("class-name", "method-name").isPresent()
    }

    def "Should get registered information points"() {
        given:
        def informationPoint1 = createInformationPoint("-1")
        def informationPoint2 = createInformationPoint("-2")
        store.restore() >> [informationPoint1, informationPoint2]
        def service = new InformationPointServiceImpl(store, newInformationPointQueue, removeInformationPointQueue)

        when:
        def informationPoints = service.getInformationPoints()

        then:
        informationPoints.containsAll([informationPoint1, informationPoint2])
    }

    def createInformationPoint(suffix = "") {
        return InformationPoint.builder()
                .withClassName("class-name")
                .withMethodName("method-name${suffix}")
                .build()
    }
}
