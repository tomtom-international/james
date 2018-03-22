package com.tomtom.james.newagent;

import com.google.common.base.Stopwatch;
import com.tomtom.james.common.api.informationpoint.InformationPoint;
import com.tomtom.james.common.api.informationpoint.InformationPointService;
import com.tomtom.james.common.log.Logger;

import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

public class JamesHQ implements Runnable {
    @Override
    public void run() {

    }
    private static final Logger LOG = Logger.getLogger(JamesHQ.class);
    private int scanPeriod = 1000;
    private InformationPointService informationPointService;
    private ClassStructure classStructure;
    private NewClassQueue newClassesQueue;
    private NewInformationPointQueue newInformationPointQueue;
    private Queue<JamesObjective> jamesObjectives = new ConcurrentLinkedQueue<>();

    private class JamesObjective {
        private InformationPoint informationPoint;
        private Class clazz;

        public JamesOrder(Class clazz, InformationPoint informationPoint) {
            this.clazz = clazz;
            this.informationPoint = informationPoint;
        }

        public InformationPoint getInformationPoint() {
            return informationPoint;
        }

        public Class getClazz() {
            return clazz;
        }
    }

    public JamesHQ(InformationPointService informationPointService, NewInformationPointQueue newInformationPointQueue, NewClassQueue newClassQueue, ClassStructure classStructure, int scanPeriod) {
        this.scanPeriod = scanPeriod;
        this.informationPointService = informationPointService;
        this.newClassesQueue = newClassQueue;
        this.classStructure = classStructure;
        this.newInformationPointQueue = newInformationPointQueue;
    }

    @Override
    public void run() {
        while (true) {
            Stopwatch stopwatch = Stopwatch.createStarted();

            // new ip
            // on already processed classes
            Stopwatch informationPointsProcessingStopwatch = Stopwatch.createStarted();
            while (!newInformationPointQueue.isEmpty()) { //FIXME - starvation issue - so many IP or new classes that everything stops ????
                InformationPoint informationPoint = newInformationPointQueue.poll();
                if (informationPoint != null) {
                    LOG.trace("JamesHQ - processing new InformationPoint : " + informationPoint);
                    if(classStructure.contains(informationPoint.getClassName())) {
                        LOG.trace("JamesHQ - preparing JamesObjectives based on ClassStructure");
                        Set<Class> children = classStructure.getChildren(informationPoint.getClassName());
                        children.forEach(child -> jamesObjectives.add(new JamesObjective(child, informationPoint)));
                    } else {
                        LOG.trace("JamesHQ - preparing simple JamesObjectives");
                        jamesObjectives.add(new JamesObjective())
                    }

                }
            }
            informationPointsProcessingStopwatch.stop();
            LOG.trace("JamesHQ - all new InformationPoints processing time = " + informationPointsProcessingStopwatch.elapsed());


            Stopwatch newClassesProcessingStopwatch = Stopwatch.createStarted();
            while (!newClassesQueue.isEmpty()) {
                ClassDescriptor classDescriptor = newClassesQueue.poll();
                if (classDescriptor != null) {
                    LOG.trace("JamesHQ - processing new class : " + classDescriptor.getClazz().getName());
                    // new class
                }
            }
            newClassesProcessingStopwatch.stop();
            LOG.trace("JamesHQ - all new Classes processing time = " + newClassesProcessingStopwatch.elapsed());


            stopwatch.stop();
            LOG.trace("JamesHQ scan time = " + stopwatch.elapsed());
            try {
                if (scanPeriod - stopwatch.elapsed(TimeUnit.MILLISECONDS) > 0) {
                    Thread.sleep(scanPeriod - stopwatch.elapsed(TimeUnit.MILLISECONDS));
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }


}
