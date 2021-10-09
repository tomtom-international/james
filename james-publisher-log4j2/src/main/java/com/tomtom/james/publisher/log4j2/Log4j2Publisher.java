/*
 * Copyright 2021 TomTom International B.V.
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

package com.tomtom.james.publisher.log4j2;

import com.tomtom.james.common.api.configuration.EventPublisherConfiguration;
import com.tomtom.james.common.api.publisher.Event;
import com.tomtom.james.common.api.publisher.EventPublisher;
import com.tomtom.james.publisher.log4j2.configuration.Log4j2PublisherConfiguration;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.MapMessage;
import java.util.LinkedHashMap;

public class Log4j2Publisher implements EventPublisher {

    private Log4j2PublisherConfiguration configuration;
    private Logger logger;
    private Level level;

    @Override
    public String getId() {
        return "james.publisher.log4j2";
    }

    @Override
    public void initialize(EventPublisherConfiguration eventPublisherConfiguration) {
        configuration = new Log4j2PublisherConfiguration(eventPublisherConfiguration);
        logger = LogManager.getLogger(configuration.getLogger());
        level = Level.toLevel(configuration.getLevel());
    }

    @Override
    public void close() {
    }

    @Override
    public void publish(Event evt) {
        final LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        map.put("@created", evt.getTimestamp());
        configuration.getEventType().ifPresent(type -> map.put("type", type));
        map.putAll(configuration.getFields());
        map.putAll(evt.getContent());
        logger.log(level, new MapMessage(map));
    }

}
