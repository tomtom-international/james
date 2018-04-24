package com.tomtom.james.newagent;

import com.tomtom.james.common.api.QueueBacked;

import java.util.concurrent.ArrayBlockingQueue;

public class JamesObjectivesQueue extends ArrayBlockingQueue<JamesObjective> implements QueueBacked {

    public JamesObjectivesQueue(int capacity) {
        super(capacity);
    }

    @Override
    public int getJobQueueSize() {
        return this.size();
    }

    @Override
    public int getJobQueueRemainingCapacity() {
        return this.remainingCapacity();
    }

    @Override
    public int getDroppedJobsCount() {
        return 0;
    }
}
