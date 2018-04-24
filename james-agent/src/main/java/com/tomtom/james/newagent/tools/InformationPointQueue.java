package com.tomtom.james.newagent.tools;

import com.tomtom.james.common.api.QueueBacked;
import com.tomtom.james.common.api.informationpoint.InformationPoint;

import java.util.Queue;


public interface InformationPointQueue extends Queue<InformationPoint>, QueueBacked{
}
