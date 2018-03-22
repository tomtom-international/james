package com.tomtom.james.newagent;

import com.tomtom.james.common.api.informationpoint.InformationPoint;

import java.util.concurrent.ConcurrentLinkedQueue;

public class BasicNewInformationPointQueue extends ConcurrentLinkedQueue<InformationPoint> implements NewInformationPointQueue {
}
