package com.tomtom.james.disruptor;

import com.lmax.disruptor.EventHandler;
import com.tomtom.james.common.log.Logger;

/**
 * Runs the job of every {@code workerCount}-th event, starting at {@code workerIndex}, so {@code workerCount} handlers
 * on one ring buffer share the work the way Disruptor 3's worker pool did.
 */
public class JobEventHandler implements EventHandler<JobEvent> {

    private static final Logger LOG = Logger.getLogger(JobEventHandler.class);

    private final int workerIndex;
    private final int workerCount;

    public JobEventHandler() {
        this(0, 1);
    }

    public JobEventHandler(int workerIndex, int workerCount) {
        this.workerIndex = workerIndex;
        this.workerCount = workerCount;
    }

    public static JobEventHandler[] workers(int workerCount) {
        JobEventHandler[] handlers = new JobEventHandler[workerCount];
        for (int i = 0; i < workerCount; i++) {
            handlers[i] = new JobEventHandler(i, workerCount);
        }
        return handlers;
    }

    @Override
    public void onEvent(
        JobEvent event,
        long sequence,
        boolean endOfBatch) {
        if (sequence % workerCount != workerIndex) {
            return;
        }
        try {
            Runnable job = event.getJob();
            if (job != null) {
                job.run();
            }
            event.setJob(null);
        } catch (Exception e) {
            LOG.error("Unhandled exception in event handler.", e);
        }
    }
}
