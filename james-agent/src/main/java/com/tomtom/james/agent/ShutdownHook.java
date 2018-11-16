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

package com.tomtom.james.agent;

import com.google.common.collect.ImmutableList;
import com.tomtom.james.common.api.Closeable;
import com.tomtom.james.common.api.publisher.EventPublisher;
import com.tomtom.james.common.api.script.ScriptEngine;
import com.tomtom.james.common.log.Logger;
import com.tomtom.james.configuration.AgentConfiguration;
import com.tomtom.james.newagent.MethodExecutionContextHelper;

import java.util.Collection;
import java.util.Objects;

public class ShutdownHook extends Thread {

    private static final Logger LOG = Logger.getLogger(ShutdownHook.class);

    private final AgentConfiguration agentConfiguration;
    private final Collection<Closeable> closeables;

    public ShutdownHook(ControllersManager controllersManager,
                 ScriptEngine scriptEngine,
                 EventPublisher eventPublisher,
                 AgentConfiguration agentConfiguration,
                 Closeable methodExecutionContextHelper) {
        super("james-shutdown-thread");
        this.agentConfiguration = Objects.requireNonNull(agentConfiguration);
        closeables = ImmutableList.of(
                Objects.requireNonNull(controllersManager),
                Objects.requireNonNull(scriptEngine),
                Objects.requireNonNull(eventPublisher),
                Objects.requireNonNull(methodExecutionContextHelper));
    }

    @Override
    public void run() {
        closeables.forEach(Closeable::close);
        if (!agentConfiguration.isQuiet()) {
            LOG.info("Agent shutdown complete.");
        }
    }
}
