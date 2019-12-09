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

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.tomtom.james.common.api.informationpoint.InformationPoint;
import com.tomtom.james.common.api.informationpoint.InformationPointService;
import com.tomtom.james.common.log.Logger;
import com.tomtom.james.informationpoint.advice.InformationPointServiceSupplier;
import com.tomtom.james.newagent.tools.InformationPointQueue;
import com.tomtom.james.store.InformationPointStore;

public class InformationPointServiceImpl implements InformationPointService {

    private static final Logger LOG = Logger.getLogger(InformationPointServiceImpl.class);

    private final InformationPointStore store;
    private final Map<String, InformationPoint> informationPoints;
    private final InformationPointQueue informationPointQueue;
    private final InformationPointQueue removeInformationPointQueue;


    public InformationPointServiceImpl(InformationPointStore store, InformationPointQueue informationPointQueue, InformationPointQueue removeInformationPointQueue) {
        this.store = Objects.requireNonNull(store);
        this.informationPointQueue = informationPointQueue;
        this.removeInformationPointQueue = removeInformationPointQueue;
        informationPoints = store.restore().stream().collect(Collectors.toConcurrentMap(InformationPointServiceImpl::toKey, Function.identity()));
        informationPointQueue.addAll(informationPoints.values()); // put all restored information points to the queue
        InformationPointServiceSupplier.register(this);
    }

    private static String toKey(InformationPoint informationPoint) {
        return toKey(informationPoint.getClassName(), informationPoint.getMethodName());
    }

    private static String toKey(String className, String methodName) {
        return className + "#" + methodName;
    }

    @Override
    public Collection<InformationPoint> getInformationPoints() {
        return Collections.unmodifiableCollection(informationPoints.values());
    }

    @Override
    public Collection<InformationPoint> getInformationPoints(String className) {
        return informationPoints.values().stream().filter(ip -> ip.getClassName().equals(className)).collect(Collectors.toSet());
    }

    @Override
    public Optional<InformationPoint> getInformationPoint(String className, String methodName) {
        return Optional.ofNullable(informationPoints.get(toKey(className, methodName)));
    }

    @Override
    public void addInformationPoint(InformationPoint informationPoint) {
        informationPoints.put(toKey(informationPoint), informationPoint);
        store.store(informationPoints.values());
        informationPointQueue.add(informationPoint);
        LOG.trace("InformationPoint added : " + informationPoint + " | queue size: " + informationPointQueue.size());
        LOG.trace("Metadata: " + informationPoint.getMetadata().toString());
    }

    @Override
    public void removeInformationPoint(InformationPoint informationPoint) {
        informationPoints.remove(toKey(informationPoint));
        removeInformationPointQueue.add(informationPoint);
        store.store(informationPoints.values());
    }

}
