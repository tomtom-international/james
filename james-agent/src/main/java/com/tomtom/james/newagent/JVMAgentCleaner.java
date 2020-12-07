package com.tomtom.james.newagent;

import com.tomtom.james.common.api.Closeable;
import java.util.Arrays;

public class JVMAgentCleaner {

    private static Closeable[] closeables;

    public static synchronized void init(Closeable... closeables) {
        JVMAgentCleaner.closeables = closeables;
    }

    public static synchronized boolean close() {
        if (closeables != null) {
            Arrays.stream(closeables).forEach(Closeable::close);
            closeables = null;
            return true;
        } else {
            return false;
        }
    }
}
