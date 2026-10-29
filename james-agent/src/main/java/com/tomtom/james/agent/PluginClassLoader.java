/*
 * Copyright 2026 TomTom International B.V.
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

package com.tomtom.james.agent;

import java.net.URL;
import java.net.URLClassLoader;

/**
 * Loads a plugin's classes from its own jar before the agent's class loader, which on an executable-jar application
 * also holds the application's libraries: a library the plugin bundles must not resolve to the application's version.
 * What the jar lacks (e.g. log4j-api, compileOnly in the log4j2 publisher) still comes from the application. The JDK,
 * the James classes and slf4j (so plugin libraries log through the application's binding) are taken parent first.
 */
class PluginClassLoader extends URLClassLoader {

    private static final String[] PARENT_FIRST_PACKAGES = {"java.", "javax.", "com.tomtom.james.", "org.slf4j."};

    PluginClassLoader(URL pluginJar, ClassLoader agentClassLoader) {
        super(new URL[]{pluginJar}, agentClassLoader);
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        if (isParentFirst(name)) {
            return super.loadClass(name, resolve);
        }
        synchronized (getClassLoadingLock(name)) {
            Class<?> loaded = findLoadedClass(name);
            if (loaded == null) {
                try {
                    loaded = findClass(name);
                } catch (ClassNotFoundException e) {
                    return super.loadClass(name, resolve);
                }
            }
            if (resolve) {
                resolveClass(loaded);
            }
            return loaded;
        }
    }

    private static boolean isParentFirst(String className) {
        for (String prefix : PARENT_FIRST_PACKAGES) {
            if (className.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }
}
