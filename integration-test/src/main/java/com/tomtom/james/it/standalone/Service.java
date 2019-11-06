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

package com.tomtom.james.it.standalone;

import java.util.concurrent.ThreadLocalRandom;

class Service {

    private int fieldValue = 10;

    int doSomething(String arg) {
        System.out.println("doSomething called with arg = " + arg);
        sleep();
        throwAtRandom(0.5);
        return 7;
    }

    static int doSomethingStatic(String arg) {
        System.out.println("doSomething static called with arg = " + arg);
        sleep();
        throwAtRandom(0.5);
        return 7;
    }

    private static void throwAtRandom(double probability) {
        double v = ThreadLocalRandom.current()
                .nextDouble();
        if (v < probability) {
            throw new RuntimeException(v + " < " + probability);
        }
    }

    private static void sleep() {
        try {
            Thread.sleep(ThreadLocalRandom.current()
                    .nextInt(500));
        } catch (InterruptedException e) {
        }
    }
}
