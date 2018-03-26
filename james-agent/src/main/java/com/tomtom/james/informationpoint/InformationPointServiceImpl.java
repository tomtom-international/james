/*
 * Copyright 2017 TomTom International B.V.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tomtom.james.informationpoint;

import com.tomtom.james.common.api.informationpoint.InformationPoint;
import com.tomtom.james.common.api.informationpoint.InformationPointService;
import com.tomtom.james.common.log.Logger;
import com.tomtom.james.newagent.tools.InformationPointQueue;
import com.tomtom.james.store.InformationPointStore;

import java.util.*;
import java.util.stream.Collectors;

public class InformationPointServiceImpl implements InformationPointService {

    private static final Logger LOG = Logger.getLogger(InformationPointServiceImpl.class);

    private final InformationPointStore store;
    private final List<InformationPoint> informationPoints;
    private final InformationPointQueue informationPointQueue;
    private final InformationPointQueue removeInformationPointQueue;


    public InformationPointServiceImpl(InformationPointStore store, InformationPointQueue informationPointQueue, InformationPointQueue removeInformationPointQueue) {
        this.store = Objects.requireNonNull(store);
        this.informationPointQueue = informationPointQueue;
        this.removeInformationPointQueue = removeInformationPointQueue;
        informationPoints = new ArrayList<>(store.restore());
        informationPointQueue.addAll(informationPoints); // put all restored information points to the queue
    }

    @Override
    public Collection<InformationPoint> getInformationPoints() {
        return Collections.unmodifiableCollection(informationPoints);
    }

    @Override
    public Collection<InformationPoint> getInformationPoints(String className) {
        return informationPoints.stream().filter(ip -> ip.getClassName().equals(className)).collect(Collectors.toSet());
    }

    @Override
    public Optional<InformationPoint> getInformationPoint(String className, String methodName) {
        return informationPoints.stream()
                .filter(point -> point.getClassName().equals(className) && point.getMethodName().equals(methodName))
                .findFirst();
    }

    @Override
    public void addInformationPoint(InformationPoint informationPoint) {
        informationPoints.add(informationPoint);
        store.store(informationPoints);
        informationPointQueue.add(informationPoint);
        LOG.trace("InformationPoint added : " + informationPoint + " | queue size: " + informationPointQueue.size());
    }

    @Override
    public void removeInformationPoint(InformationPoint informationPoint) {
        informationPoints.remove(informationPoint);
        removeInformationPointQueue.add(informationPoint);
        store.store(informationPoints);
    }

}
