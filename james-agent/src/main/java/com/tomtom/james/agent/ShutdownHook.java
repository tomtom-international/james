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

import com.google.common.util.concurrent.Uninterruptibles;
import com.tomtom.james.common.log.Logger;
import com.tomtom.james.configuration.AgentConfiguration;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class ShutdownHook extends Thread {

    private static final Logger LOG = Logger.getLogger(ShutdownHook.class);

    private final AgentConfiguration agentConfiguration;
    private Runnable cleanupCallback;

    public ShutdownHook(AgentConfiguration agentConfiguration, Runnable cleanupCallback) {
        super("james-shutdown-thread");
        this.agentConfiguration = Objects.requireNonNull(agentConfiguration);
        this.cleanupCallback = cleanupCallback;
    }

    @Override
    public void run() {
        Uninterruptibles.sleepUninterruptibly(agentConfiguration.getShutdownDelay(), TimeUnit.MILLISECONDS);
        cleanupCallback.run();
        if (!agentConfiguration.isQuiet()) {
            LOG.info("Agent shutdown complete.");
        }
    }
}
