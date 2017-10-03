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

package com.tomtom.james.script;

import com.tomtom.james.agent.ToolkitManager;
import com.tomtom.james.common.api.publisher.Event;
import com.tomtom.james.common.api.publisher.EventPublisher;
import com.tomtom.james.common.api.toolkit.Toolkit;
import groovy.lang.Script;

import java.util.concurrent.ConcurrentHashMap;

public abstract class InformationPointHandler extends Script {

    private final ConcurrentHashMap<Object, Object> localStore = new ConcurrentHashMap<>();

    private ConcurrentHashMap<Object, Object> globalStore;
    private EventPublisher publisher;
    private ToolkitManager toolkitManager;

    void setGlobalStore(ConcurrentHashMap<Object, Object> globalStore) {
        this.globalStore = globalStore;
    }

    void setEventPublisher(EventPublisher publisher) {
        this.publisher = publisher;
    }

    void setToolkitManager(ToolkitManager toolkitManager) {
        this.toolkitManager = toolkitManager;
    }

    @SuppressWarnings("unused")
    public void persistInLocalStore(Object key, Object val) {
        localStore.put(key, val);
    }

    @SuppressWarnings("unused")
    public Object retrieveFromLocalStore(Object key) {
        return localStore.get(key);
    }

    @SuppressWarnings("unused")
    public void persistInGlobalStore(Object key, Object val) {
        globalStore.put(key, val);
    }

    @SuppressWarnings("unused")
    public Object retrieveFromGlobalStore(Object key) {
        return globalStore.get(key);
    }

    @SuppressWarnings("unused")
    public void publishEvent(Event evt) {
        publisher.publish(evt);
    }

    @SuppressWarnings("unused")
    public Toolkit getToolkitById(String toolkitId) throws ToolkitNotFoundException {
        return toolkitManager.getToolkit(toolkitId)
                .orElseThrow(() -> new ToolkitNotFoundException(toolkitId));
    }

    public static class ToolkitNotFoundException extends Exception {
        ToolkitNotFoundException(String toolkitId) {
            super("Toolkit " + toolkitId + " not found");
        }
    }
}
