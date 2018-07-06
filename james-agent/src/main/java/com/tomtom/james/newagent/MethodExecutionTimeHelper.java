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

package com.tomtom.james.newagent;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.Supplier;

public final class MethodExecutionTimeHelper {

    private static ThreadLocal<ArrayDeque<Long>> startTimeStack = ThreadLocal.withInitial(new Supplier<ArrayDeque<Long>>() {
        @Override
        public ArrayDeque<Long> get() {
            return new ArrayDeque<Long>(8);
        }
    });

    public static long executionStarted() {
        final long startTime = System.nanoTime();
        startTimeStack.get().push(startTime);
        return startTime;
    }

    public static long getStartTime() {
        long startTime = startTimeStack.get().peek();
        return startTime;
    }

    public static long executionFinished() {
        long startTime = startTimeStack.get().pop();
        return startTime;
    }
}
