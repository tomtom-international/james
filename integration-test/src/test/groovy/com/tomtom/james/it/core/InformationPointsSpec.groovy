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
import com.tomtom.james.it.utils.JamesControllerProvider

class InformationPointsSpec extends BaseJamesSpecification {

    def "Information point list should be empty after James is started"() {
        given:
        def controller = JamesControllerProvider.get()

        when:
        def ips = controller.informationPoints

        then:
        ips.isEmpty()
    }

    def "Should add information point"() {
        given:
        def controller = JamesControllerProvider.get()
        def informationPoint = new InformationPointDTO(
                className: "foo.bar.className",
                methodName: "methodName",
                script: []
        )

        when:
        controller.createInformationPoint(informationPoint)
        def ips = controller.informationPoints

        then:
        ips.size() == 1
        ips[0].className == "foo.bar.className"
        ips[0].methodName == "methodName"
        ips[0].sampleRate == 100

    }

    def "Should remove information point"() {
        given:
        def controller = JamesControllerProvider.get()
        def informationPoint = new InformationPointDTO(
                className: "foo.bar.className2",
                methodName: "methodName2",
                script: []
        )

        when:
        controller.createInformationPoint(informationPoint)
        def ipsBeforeRemove = controller.informationPoints
        controller.removeInformationPoint(informationPoint.className, informationPoint.methodName)
        def ipsAfterRemove = controller.informationPoints


        then:
        ipsBeforeRemove.size() == 1
        ipsBeforeRemove[0].className == "foo.bar.className2"
        ipsBeforeRemove[0].methodName == "methodName2"
        ipsBeforeRemove[0].sampleRate == 100

        ipsAfterRemove.isEmpty()
    }

}
