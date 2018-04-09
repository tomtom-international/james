package com.tomtom.james.newagent.tools;

import com.tomtom.james.common.api.informationpoint.InformationPoint;

import java.util.concurrent.ArrayBlockingQueue;

public class BasicInformationPointQueue extends ArrayBlockingQueue<InformationPoint> implements InformationPointQueue {

    public BasicInformationPointQueue(int capacity) {
        super(capacity);
    }

}
