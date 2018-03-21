package com.tomtom.james.newagent;

import com.google.common.base.Stopwatch;
import com.tomtom.james.common.log.Logger;

import java.lang.instrument.Instrumentation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class JamesClassScanner implements Runnable {
    private static final Logger LOG = Logger.getLogger(JamesClassScanner.class);
    private static int INITIAL_DELAY = 10000;
    private static int SCAN_PERIOD = 5000;
    private List<Class> processedClasses = new ArrayList<>();
    private ClassDeltaBuffer classDeltaBuffer;

    public JamesClassScanner(ClassDeltaBuffer classDeltaBuffer) {
        this.classDeltaBuffer = classDeltaBuffer;
    }

    @Override
    public void run() {
        LOG.trace("JamesClassScanner ready and sleep " + INITIAL_DELAY + " ms of INITIAL_DELAY.");
        Instrumentation instrumentation = JamesAgent.getInstrumentation();
        while (true) {
            LOG.trace("JamesClassScanner - scan started.");
            Stopwatch stopwatch = new Stopwatch();
            // FIXME - optimize getting delta
            List<Class> newScan = Arrays.asList(instrumentation.getAllLoadedClasses());
            newScan.removeAll(processedClasses);
            LOG.trace("JamesClassScanner - delta size : " + newScan.size());
            classDeltaBuffer.addAll(newScan);
            stopwatch.stop();
            LOG.trace("JamesClassScanner - scan finished (" + stopwatch.elapsed());
            try {
                Thread.sleep(SCAN_PERIOD);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

}
