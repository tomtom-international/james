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
import com.tomtom.james.common.api.informationpoint.Metadata;
import com.tomtom.james.common.api.publisher.Event;
import com.tomtom.james.common.api.publisher.EventPublisher;
import com.tomtom.james.common.api.toolkit.Toolkit;
import groovy.lang.Script;

public abstract class InformationPointHandler extends Script {

    private EventPublisher publisher;
    private ToolkitManager toolkitManager;
    private Metadata metadata;

    void setEventPublisher(EventPublisher publisher) {
        this.publisher = publisher;
    }

    void setToolkitManager(ToolkitManager toolkitManager) {
        this.toolkitManager = toolkitManager;
    }

    void setMetadata(Metadata metadata) {
        this.metadata = metadata;
    }

    @SuppressWarnings("unused")
    public void publishEvent(Event evt) {
        evt.getContent().putAll(metadata);
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
