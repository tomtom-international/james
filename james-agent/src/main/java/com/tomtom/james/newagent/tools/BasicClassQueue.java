package com.tomtom.james.newagent.tools;

import java.util.concurrent.ArrayBlockingQueue;

public class BasicClassQueue extends ArrayBlockingQueue<Class> implements ClassQueue {

    public BasicClassQueue(int capacity) {
        super(capacity);
    }
}
