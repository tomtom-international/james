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

import com.fasterxml.jackson.databind.ObjectMapper

import java.nio.file.Files
import java.nio.file.NoSuchFileException
import java.nio.file.Paths

class TestUtils {

    static def tmpdir = Paths.get(System.getProperty("java.io.tmpdir"))
    static def eventsFileName = "James-TestPublisher-out.txt"
    static def objectMapper = new ObjectMapper()

    static def cleanUpEventsFile() {
        try {
            Files.delete(tmpdir.resolve(eventsFileName))
        } catch (NoSuchFileException e) {
            // do nothing
        }
    }

    static def cleanUpInformationPoints() {
        JamesControllerProvider.get().informationPoints.each {
            JamesControllerProvider.get().removeInformationPoint(it.className, it.methodName)
        }
    }

    static List<Map<String, Object>> readPublishedEvents() {
        try {
            return Files.readAllLines(tmpdir.resolve(eventsFileName)).collect {
                objectMapper.readValue(it, Map)
            }
        } catch (NoSuchFileException e) {
            return []
        } catch (IOException e) {
            throw new RuntimeException(e)
        }
    }

    static List<String> scriptLines(Class klass, String name) {
        return klass.getResourceAsStream("${klass.simpleName}_${name}.groovy").readLines()
    }

    static def splitToLines(String s) {
        return s.replace("\r", "").split("\n")
    }

}
