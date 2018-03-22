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

import com.tomtom.james.it.utils.TestUtils
import spock.lang.Specification

import java.util.concurrent.TimeUnit

import static org.awaitility.Awaitility.await

abstract class BaseJamesSpecification extends Specification {

    def setup() {
        TestUtils.cleanUpInformationPoints()
        TestUtils.cleanUpEventsFile()
    }

    def readPublishedEventsWithWait(int expectedEvents) {
        await().atMost(5, TimeUnit.SECONDS).until { TestUtils.readPublishedEvents().size() >= expectedEvents }
        sleep(100) // give the producer a bit more time to ensure there are no more events
        return TestUtils.readPublishedEvents()
    }

}