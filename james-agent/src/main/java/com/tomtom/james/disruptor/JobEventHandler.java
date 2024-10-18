package com.tomtom.james.disruptor;

import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.WorkHandler;
import com.tomtom.james.common.log.Logger;

public class JobEventHandler implements EventHandler<JobEvent>, WorkHandler<JobEvent> {

    private static final Logger LOG = Logger.getLogger(JobEventHandler.class);

    public JobEventHandler() {

    }

    @Override
    public void onEvent(
        JobEvent event,
        long sequence,
        boolean endOfBatch) throws Exception {
        onEvent(event);
    }

    @Override
    public void onEvent(final JobEvent event) throws Exception {
        try{

            Runnable job = event.getJob();
            if (job != null) {
                job.run();
            }
            event.setJob(null);
        }
        catch (Exception e ){
            LOG.error("Unhandled exception in event handler.", e);
        }
    }
}
