package com.tomtom.james.informationpoint.advice;

import java.time.Duration;

public class ExecutionContext {
    private final long started;
    private Object initialContext;

    public ExecutionContext() {
        this.started = System.nanoTime();
    }

    public Duration getElapsedTime() {
        return Duration.ofNanos(System.nanoTime() - started);
    }

    public Object getInitialContext() {
        return initialContext;
    }

    public void setInitialContext(final Object initialContext) {
        this.initialContext = initialContext;
    }
}
