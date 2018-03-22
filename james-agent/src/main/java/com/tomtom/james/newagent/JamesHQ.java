package com.tomtom.james.newagent;

import com.google.common.base.Stopwatch;
import com.tomtom.james.common.api.informationpoint.InformationPoint;
import com.tomtom.james.common.api.informationpoint.InformationPointService;
import com.tomtom.james.common.log.Logger;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

public class JamesHQ implements Runnable {
    private static final Logger LOG = Logger.getLogger(JamesHQ.class);
    private int scanPeriod = 1000;
    private InformationPointService informationPointService;
    private GlobalClassStructure globalClassStructure;
    private NewClassQueue newClassesQueue;
    private NewInformationPointQueue newInformationPointQueue;
    private Queue<JamesOrder> orderQueue = new ConcurrentLinkedQueue<>();

    private class JamesOrder {
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

    public JamesHQ(InformationPointService informationPointService, NewInformationPointQueue newInformationPointQueue, NewClassQueue newClassQueue, GlobalClassStructure globalClassStructure, int scanPeriod) {
        this.scanPeriod = scanPeriod;
        this.informationPointService = informationPointService;
        this.newClassesQueue = newClassQueue;
        this.globalClassStructure = globalClassStructure;
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
                    if (globalClassStructure.containsKey(informationPoint.getClass())) {

                        // is interface
                        if ()
                        // is abstract class
                        // has children
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
