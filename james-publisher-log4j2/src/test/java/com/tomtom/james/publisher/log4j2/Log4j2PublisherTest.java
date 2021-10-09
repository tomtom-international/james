package com.tomtom.james.publisher.log4j2;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tomtom.james.common.api.configuration.EventPublisherConfiguration;
import com.tomtom.james.common.api.configuration.StructuredConfiguration;
import com.tomtom.james.common.api.publisher.Event;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.simple.SimpleLogger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.time.Instant;
import java.util.Collections;
import java.util.Optional;

class Log4j2PublisherTest {

    public static final String LOGGER_NAME = "james.logger";

    public Log4j2Publisher createPublisher() {
        final EventPublisherConfiguration eventPublisherConfiguration = mock(EventPublisherConfiguration.class);
        final StructuredConfiguration configuration = mock(StructuredConfiguration.class);
        final StructuredConfiguration loggerConfiguration = mock(StructuredConfiguration.class);
        final StructuredConfiguration levelConfiguration = mock(StructuredConfiguration.class);
        final StructuredConfiguration eventTypeConfiguration = mock(StructuredConfiguration.class);
        when(eventPublisherConfiguration.getProperties())
            .thenReturn(Optional.of(configuration));
        when(configuration.get("logger")).thenReturn(Optional.of(loggerConfiguration));
        when(loggerConfiguration.asString()).thenReturn(LOGGER_NAME);
        when(configuration.get("level")).thenReturn(Optional.of(levelConfiguration));
        when(levelConfiguration.asString()).thenReturn("INFO");
        when(configuration.get("eventType")).thenReturn(Optional.of(eventTypeConfiguration));
        when(eventTypeConfiguration.asString()).thenReturn("james");

        final Log4j2Publisher log4j2Publisher = new Log4j2Publisher();
        log4j2Publisher.initialize(eventPublisherConfiguration);
        return log4j2Publisher;
    }

    @BeforeEach
    void setUp() {
        System.setProperty("org.apache.logging.log4j.simplelog." + LOGGER_NAME + ".level", "INFO");
    }

    @Test
    public void shouldLogEvent() {
        SimpleLogger logger = (SimpleLogger)LogManager.getLogger(LOGGER_NAME);
        final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        logger.setStream(new PrintStream(byteArrayOutputStream));

        createPublisher().publish(new Event(Collections.singletonMap("key", "value"), Instant.EPOCH));

        assertThat(byteArrayOutputStream.toString().trim()).isEqualTo(
            "INFO logger @created=\"1970-01-01T00:00:00Z\" key=\"value\" type=\"james\"");
    }

}
