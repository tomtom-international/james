package com.tomtom.james.newagent.tools;

import com.tomtom.james.common.api.informationpoint.InformationPoint;
import com.tomtom.james.newagent.tools.NewInformationPointQueue;

import java.util.concurrent.ConcurrentLinkedQueue;

public class BasicNewInformationPointQueue extends ConcurrentLinkedQueue<InformationPoint> implements NewInformationPointQueue {
}
