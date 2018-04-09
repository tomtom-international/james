package com.tomtom.james.newagent;

public class InstrumentationNotFoundException extends Exception {
    public InstrumentationNotFoundException() {
    }

    public InstrumentationNotFoundException(String message) {
        super(message);
    }

    public InstrumentationNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public InstrumentationNotFoundException(Throwable cause) {
        super(cause);
    }

    public InstrumentationNotFoundException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
