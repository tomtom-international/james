package com.tomtom.james.newagent.tools;

import java.util.concurrent.ArrayBlockingQueue;

public class BasicNewClassQueue extends ArrayBlockingQueue<Class> implements NewClassQueue {

    public BasicNewClassQueue(int capacity) {
        super(capacity);
    }
}
