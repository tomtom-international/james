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

package com.tomtom.james.common.api.publisher;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class Event {

    private final Instant timestamp;
    private final Map<String, Object> content;

    public Event(Map<String, Object> content, Instant timestamp) {
        this.timestamp = timestamp;
        this.content = Objects.requireNonNull(content);
    }

    public Event(Map<String, Object> content) {
        this(content, Instant.now());
    }

    public Event(String message) {
        this(createMessageMap(Objects.requireNonNull(message)));
    }

    public Map<String, Object> getContent() {
        return content;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    private static Map<String, Object> createMessageMap(String message) {
        HashMap<String, Object> map = new HashMap<>();
        map.put("message", message);
        return Collections.unmodifiableMap(map);
    }

    public Event withEntry(String key, Object value) {
        this.content.put(key, value);
        return this;
    }

    public Event withEntries(Map<String, Object> content) {
        this.content.putAll(content);
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Event event = (Event) o;
        return Objects.equals(timestamp, event.timestamp) &&
                Objects.equals(content, event.content);
    }

    @Override
    public int hashCode() {
        return Objects.hash(timestamp, content);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Event{");
        sb.append("timestamp=").append(timestamp);
        sb.append(", content=").append(content);
        sb.append('}');
        return sb.toString();
    }
}
