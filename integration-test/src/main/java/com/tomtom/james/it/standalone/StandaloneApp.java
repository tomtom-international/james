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

import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class StandaloneApp {

    public static void main(String[] args) throws Exception {
        System.out.println();
        outs("Dummy app started.");
        System.out.println();

        outs("Thread context class loader:", Thread.currentThread().getContextClassLoader());
        outs("System class loader:        ", ClassLoader.getSystemClassLoader());

        printClassLoaderURLs(((URLClassLoader) ClassLoader.getSystemClassLoader()));

        boolean keepRunning = Boolean.parseBoolean(System.getProperty("app.keepRunning", "false"));
        if (keepRunning) {
            Service service = new Service();
            while (true) {
                try {
                    service.doSomething(UUID.randomUUID().toString());
                } catch (Throwable t) {
                    // do nothing
                } finally {
                    Thread.sleep(2000);
                }

                try {
                    Service.doSomethingStatic(UUID.randomUUID().toString());
                } catch (Throwable t) {
                    // do nothing
                } finally {
                    Thread.sleep(2000);
                }
            }
        }
    }

    private static void printClassLoaderURLs(URLClassLoader urlClassLoader) {
        System.out.println();
        outs(urlClassLoader);
        Stream.of(urlClassLoader.getURLs()).forEach(url -> outs("..", url));

        if (urlClassLoader.getParent() != null) {
            printClassLoaderURLs((URLClassLoader) urlClassLoader.getParent());
        }
    }

    private static void outs(Object... parts) {
        String joinedParts = Arrays.stream(parts)
                .map(Object::toString)
                .collect(Collectors.joining(" "));
        System.out.println("[app] " + joinedParts);
    }
}
