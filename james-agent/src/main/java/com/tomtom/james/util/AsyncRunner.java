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

package com.tomtom.james.util;

import com.tomtom.james.common.log.Logger;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

public class AsyncRunner<T extends Runnable> implements Runnable {

    private static final Logger LOG = Logger.getLogger(AsyncRunner.class);
    private static final int POLL_TIMEOUT = 100;

    private final BlockingQueue<T> jobQueue;

    public AsyncRunner(BlockingQueue<T> jobQueue) {
        this.jobQueue = jobQueue;
    }

    @Override
    public void run() {
        while (!Thread.interrupted()) {
            try {
                Runnable job = jobQueue.poll(POLL_TIMEOUT, TimeUnit.MILLISECONDS);
                if (job != null) job.run();
            } catch (InterruptedException e) {
                return;
            } catch (Exception e) {
                LOG.error("Unhandled exception in async runner", e);
                return;
            }
        }
    }
}
