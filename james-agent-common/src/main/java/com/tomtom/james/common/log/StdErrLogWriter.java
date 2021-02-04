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

package com.tomtom.james.common.log;

import co.elastic.logging.EcsJsonSerializer;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;

class StdErrLogWriter implements LogWriter {

    private static final String SEPARATOR = " ";

    @Override
    public void write(Logger.Level level, Logger.Format format, String path, String message) {
        String formattedMessage;
        if (format == Logger.Format.JSON) {
            formattedMessage = formatJsonMessage(level, path, message, null);
        } else {
            formattedMessage = formatMessage(level, path, message);
        }
        System.err.println(formattedMessage);
    }

    @Override
    public void write(Logger.Level level, Logger.Format format, String path, String message, Throwable throwable) {
        String formattedMessage = null;
        if (format == Logger.Format.JSON) {
            formattedMessage = formatJsonMessage(level, path, message, null);
        } else {
            formattedMessage = formatMessage(level, path, message, throwable);
        }
        System.err.println(formattedMessage);
    }

    private String formatJsonMessage(final Logger.Level level, final String path, final String message, final Throwable throwable) {
        StringBuilder builder = new StringBuilder();
        EcsJsonSerializer.serializeObjectStart(builder, ZonedDateTime.now().toInstant().toEpochMilli());
        EcsJsonSerializer.serializeLogLevel(builder, level.toString());
        EcsJsonSerializer.serializeFormattedMessage(builder, message);
        EcsJsonSerializer.serializeEcsVersion(builder);
        EcsJsonSerializer.serializeThreadName(builder, Thread.currentThread().getName());
        EcsJsonSerializer.serializeLoggerName(builder, path);
        if (throwable != null) {
            EcsJsonSerializer.serializeException(builder,throwable, false);
        }
        EcsJsonSerializer.serializeObjectEnd(builder);
        return builder.toString();
    }

    private String formatMessage(Logger.Level level, String path, String message) {
        String result = LocalDateTime.now() + " [James] " +
                        '[' + Thread.currentThread().getName() + ']' +
                        SEPARATOR + level +
                        SEPARATOR + path +
                        SEPARATOR + '-' +
                        SEPARATOR + message;
        return result;
    }

    private String formatMessage(Logger.Level level, String path, String message, Throwable throwable) {
        StringWriter writer = new StringWriter();
        writer.write(formatMessage(level, path, message));
        writer.write(System.lineSeparator());
        throwable.printStackTrace(new PrintWriter(writer));
        return writer.toString();
    }
}
