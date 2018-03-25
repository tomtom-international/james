package com.tomtom.james.newagent.tools;

import com.tomtom.james.common.api.informationpoint.InformationPoint;

import java.util.concurrent.ConcurrentLinkedQueue;

public class BasicInformationPointQueue extends ConcurrentLinkedQueue<InformationPoint> implements InformationPointQueue {
}
