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

import java.util.function.Supplier;

public class Logger {

    private static Level currentLogLevel = Level.WARN;
    private static Format format = Format.PLAIN;

    private final LogWriter writer;
    private final String path;

    private Logger(String path) {
        this.path = path;
        this.writer = new StdErrLogWriter();
    }

    public static Logger getLogger(Class<?> klass) {
        return new Logger(klass.getName());
    }

    public static void setCurrentLogLevel(Level level) {
        currentLogLevel = level;
    }

    public static void setLogFormat(final Format logFormat) {
        format = logFormat;
    }

    @SuppressWarnings("unused")
    public void trace(Supplier<String> messageSupplier) {
        if (Level.TRACE.shouldBeLoggedFor(currentLogLevel))
            writer.write(Level.TRACE, format, path, messageSupplier.get());
    }

    @SuppressWarnings("unused")
    public void trace(String message) {
        if (Level.TRACE.shouldBeLoggedFor(currentLogLevel))
            writer.write(Level.TRACE, format, path, message);
    }

    @SuppressWarnings("unused")
    public void trace(Supplier<String> messageSupplier, Throwable throwable) {
        if (Level.TRACE.shouldBeLoggedFor(currentLogLevel))
            writer.write(Level.TRACE, format, path, messageSupplier.get(), throwable);
    }

    @SuppressWarnings("unused")
    public void trace(String message, Throwable throwable) {
        if (Level.TRACE.shouldBeLoggedFor(currentLogLevel))
            writer.write(Level.TRACE, format, path, message, throwable);
    }


    @SuppressWarnings("unused")
    public void debug(Supplier<String> messageSupplier) {
        if (Level.DEBUG.shouldBeLoggedFor(currentLogLevel))
            writer.write(Level.DEBUG, format, path, messageSupplier.get());
    }

    @SuppressWarnings("unused")
    public void debug(String message) {
        if (Level.DEBUG.shouldBeLoggedFor(currentLogLevel))
            writer.write(Level.DEBUG, format, path, message);
    }

    @SuppressWarnings("unused")
    public void debug(Supplier<String> messageSupplier, Throwable throwable) {
        if (Level.DEBUG.shouldBeLoggedFor(currentLogLevel))
            writer.write(Level.DEBUG, format, path, messageSupplier.get(), throwable);
    }

    @SuppressWarnings("unused")
    public void debug(String message, Throwable throwable) {
        if (Level.DEBUG.shouldBeLoggedFor(currentLogLevel))
            writer.write(Level.DEBUG, format, path, message, throwable);
    }


    @SuppressWarnings("unused")
    public void info(Supplier<String> messageSupplier) {
        if (Level.INFO.shouldBeLoggedFor(currentLogLevel))
            writer.write(Level.INFO, format, path, messageSupplier.get());
    }

    @SuppressWarnings("unused")
    public void info(String message) {
        if (Level.INFO.shouldBeLoggedFor(currentLogLevel))
            writer.write(Level.INFO, format, path, message);
    }

    @SuppressWarnings("unused")
    public void info(Supplier<String> messageSupplier, Throwable throwable) {
        if (Level.INFO.shouldBeLoggedFor(currentLogLevel))
            writer.write(Level.INFO, format, path, messageSupplier.get(), throwable);
    }

    @SuppressWarnings("unused")
    public void info(String message, Throwable throwable) {
        if (Level.INFO.shouldBeLoggedFor(currentLogLevel))
            writer.write(Level.INFO, format, path, message, throwable);
    }


    @SuppressWarnings("unused")
    public void warn(Supplier<String> messageSupplier) {
        if (Level.WARN.shouldBeLoggedFor(currentLogLevel))
            writer.write(Level.WARN, format, path, messageSupplier.get());
    }

    @SuppressWarnings("unused")
    public void warn(String message) {
        if (Level.WARN.shouldBeLoggedFor(currentLogLevel))
            writer.write(Level.WARN, format, path, message);
    }

    @SuppressWarnings("unused")
    public void warn(Supplier<String> messageSupplier, Throwable throwable) {
        if (Level.WARN.shouldBeLoggedFor(currentLogLevel))
            writer.write(Level.WARN, format, path, messageSupplier.get(), throwable);
    }

    @SuppressWarnings("unused")
    public void warn(String message, Throwable throwable) {
        if (Level.WARN.shouldBeLoggedFor(currentLogLevel))
            writer.write(Level.WARN, format, path, message, throwable);
    }


    @SuppressWarnings("unused")
    public void error(Supplier<String> messageSupplier) {
        if (Level.ERROR.shouldBeLoggedFor(currentLogLevel))
            writer.write(Level.ERROR, format, path, messageSupplier.get());
    }

    @SuppressWarnings("unused")
    public void error(String message) {
        if (Level.ERROR.shouldBeLoggedFor(currentLogLevel))
            writer.write(Level.ERROR, format, path, message);
    }

    @SuppressWarnings("unused")
    public void error(Supplier<String> messageSupplier, Throwable throwable) {
        if (Level.ERROR.shouldBeLoggedFor(currentLogLevel))
            writer.write(Level.ERROR, format, path, messageSupplier.get(), throwable);
    }

    @SuppressWarnings("unused")
    public void error(String message, Throwable throwable) {
        if (Level.ERROR.shouldBeLoggedFor(currentLogLevel))
            writer.write(Level.ERROR, format, path, message, throwable);
    }


    @SuppressWarnings("unused")
    public void fatal(Supplier<String> messageSupplier) {
        if (Level.FATAL.shouldBeLoggedFor(currentLogLevel))
            writer.write(Level.FATAL, format, path, messageSupplier.get());
    }

    @SuppressWarnings("unused")
    public void fatal(String message) {
        if (Level.FATAL.shouldBeLoggedFor(currentLogLevel))
            writer.write(Level.FATAL, format, path, message);
    }

    @SuppressWarnings("unused")
    public void fatal(Supplier<String> messageSupplier, Throwable throwable) {
        if (Level.FATAL.shouldBeLoggedFor(currentLogLevel))
            writer.write(Level.FATAL, format, path, messageSupplier.get(), throwable);
    }

    @SuppressWarnings("unused")
    public void fatal(String message, Throwable throwable) {
        if (Level.FATAL.shouldBeLoggedFor(currentLogLevel))
            writer.write(Level.FATAL, format, path, message, throwable);
    }

    @SuppressWarnings("unused")
    public boolean isTraceEnabled() {
        return Level.TRACE.shouldBeLoggedFor(currentLogLevel);
    }

    public enum Level {
        TRACE(0),
        DEBUG(1),
        INFO(2),
        WARN(3),
        ERROR(4),
        FATAL(5);

        private final int severity;

        Level(int severity) {
            this.severity = severity;
        }

        public boolean shouldBeLoggedFor(Level other) {
            return this.severity >= other.severity;
        }
    }

    public enum Format {
        PLAIN,
        JSON
    }
}
