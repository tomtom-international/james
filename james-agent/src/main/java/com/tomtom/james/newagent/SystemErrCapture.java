package com.tomtom.james.newagent;

import com.google.common.io.CharStreams;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.StringReader;
import java.util.function.Predicate;

public class SystemErrCapture {

    private static PrintStream oldErrStream;
    private static ByteArrayOutputStream outputStream;
    private static PrintStream newErrStream;

    public static void register() {
        SystemErrCapture.oldErrStream = System.err;
        outputStream = new ByteArrayOutputStream();
        newErrStream = new PrintStream(outputStream);
        System.setErr(newErrStream);
    }

    public static void unregisterAndReplay(Predicate<String> predicate) {
        if (newErrStream == null) {
            return;
        }
        newErrStream.close();
        System.setErr(oldErrStream);

        replayErrorsMatchingPredicate(outputStream, predicate);
        newErrStream = null;
        outputStream = null;
    }


    private static void replayErrorsMatchingPredicate(final ByteArrayOutputStream outputStream,
                                                final Predicate<String> predicate) {
        try {
            CharStreams.readLines(new StringReader(outputStream.toString()))
                       .stream().filter(predicate::test)
                       .forEach(System.err::println);
        } catch (IOException e) {
            // ignore me
        }
    }
}
