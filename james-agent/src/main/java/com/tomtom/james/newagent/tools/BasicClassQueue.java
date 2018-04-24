package com.tomtom.james.newagent.tools;

import com.tomtom.james.common.api.QueueBacked;

import java.util.concurrent.ArrayBlockingQueue;

public class BasicClassQueue extends ArrayBlockingQueue<Class> implements ClassQueue {

    public BasicClassQueue(int capacity) {
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
