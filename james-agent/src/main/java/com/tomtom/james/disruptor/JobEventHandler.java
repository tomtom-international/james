package com.tomtom.james.disruptor;

import com.lmax.disruptor.EventHandler;
import com.tomtom.james.common.log.Logger;

public class JobEventHandler implements EventHandler<JobEvent> {

    private static final Logger LOG = Logger.getLogger(JobEventHandler.class);

    @Override
    public void onEvent(
        JobEvent event,
        long sequence,
        boolean endOfBatch) {
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
