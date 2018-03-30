package com.tomtom.james.newagent;

import com.google.common.base.Stopwatch;
import com.tomtom.james.common.api.informationpoint.InformationPoint;
import com.tomtom.james.common.api.informationpoint.InformationPointService;
import com.tomtom.james.common.log.Logger;
import com.tomtom.james.newagent.james.GroovyJames;
import com.tomtom.james.newagent.tools.InformationPointQueue;
import com.tomtom.james.newagent.tools.NewClassQueue;
import org.apache.commons.lang3.ClassUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

public class JamesHQ implements Runnable {
    private static final Logger LOG = Logger.getLogger(JamesHQ.class);
    private long initialDelay = 10000;
    private long scanPeriod = 1000;
    private long jamesInterval = 1000;
    private InformationPointService informationPointService;
    private ClassService classService;
    private NewClassQueue newClassesQueue;
    private InformationPointQueue addInformationPointQueue;
    private InformationPointQueue removeInformationPointQueue;
    private Queue<JamesObjective> jamesObjectives = new ConcurrentLinkedQueue<>();
    private Thread james;

    public JamesHQ(InformationPointService informationPointService, ClassService classService, InformationPointQueue addInformationPointQueue, InformationPointQueue removeInformationPointQueue, NewClassQueue newClassQueue, long initialDelay, long scanPeriod, long jamesInterval) {
        this.scanPeriod = scanPeriod;
        this.initialDelay = initialDelay;
        this.jamesInterval = jamesInterval;
        this.informationPointService = informationPointService;
        this.classService = classService;
        this.newClassesQueue = newClassQueue;
        this.addInformationPointQueue = addInformationPointQueue;
        this.removeInformationPointQueue = removeInformationPointQueue;
    }


    @Override
    public void run() {
        // start James
        try {
            Thread.sleep(initialDelay);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        james = new Thread(new GroovyJames(jamesObjectives, jamesInterval));
        james.setDaemon(true);
        james.start();

        while (true) {
            Stopwatch stopwatch = Stopwatch.createStarted();

            LOG.trace("JamesHQ :: addInformationPointQueue [" + addInformationPointQueue.size() + "] | newClassQueue [" + newClassesQueue.size() + "] ");
                processNewInformationPoints();
                processRemoveInformationPoints();
                processNewClasses();
            stopwatch.stop();
            LOG.debug("JamesHQ scan time = " + stopwatch.elapsed());
            try {
                if (scanPeriod - stopwatch.elapsed(TimeUnit.MILLISECONDS) > 0) {
                    Thread.sleep(scanPeriod - stopwatch.elapsed(TimeUnit.MILLISECONDS));
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void processNewClasses() {
        // new class
        Stopwatch newClassesProcessingStopwatch = Stopwatch.createStarted();
        while (!newClassesQueue.isEmpty()) {
            Class newClazz = newClassesQueue.poll();
            if (newClazz != null) {
                // first we need to check if there is any information point on any parent interface of superclass of given newClazz
                List<Class<?>> interfacesAndSuperClasses = new ArrayList<>();
                interfacesAndSuperClasses.addAll(ClassUtils.getAllInterfaces(newClazz));
                interfacesAndSuperClasses.addAll(ClassUtils.getAllSuperclasses(newClazz));
                for (Class superClass : interfacesAndSuperClasses) {
                    if (informationPointService.getInformationPoints(superClass.getName()).size() > 0) {
                        for (InformationPoint informationPoint : informationPointService.getInformationPoints(superClass.getName())) {
                            LOG.trace(" [SUPER] : " + newClazz + " :: " + informationPoint);
                            jamesObjectives.add(new JamesObjective(JamesObjective.ObjectiveType.ADD, newClazz, informationPoint));
                        }
                    }
                }
                // next we check if there is any information points for directly newClazz
                if (informationPointService.getInformationPoints(newClazz.getName()).size() > 0) {
                    for (InformationPoint informationPoint : informationPointService.getInformationPoints(newClazz.getName())) {
                        LOG.trace(" [DIRECT] : " + newClazz + " :: " + informationPoint);
                        jamesObjectives.add(new JamesObjective(JamesObjective.ObjectiveType.ADD, newClazz, informationPoint));
                    }
                }

            }
        }
        newClassesProcessingStopwatch.stop();
        LOG.trace("JamesHQ - all new Classes processing time = " + newClassesProcessingStopwatch.elapsed());
    }

    private void processRemoveInformationPoints() {
        // remove ip
        while (!removeInformationPointQueue.isEmpty()) { //FIXME - starvation issue - so many IP or new classes that everything stops ????
            InformationPoint informationPoint = removeInformationPointQueue.poll();
            if (informationPoint != null) {
                LOG.trace("JamesHQ - processing remove of InformationPoint : " + informationPoint);
                if (classService.getChildrenOf(informationPoint.getClassName()).size() > 0) {
                    // information point is declared on interaface or abstract class - we add information point for every child
                    LOG.trace("JamesHQ - preparing JamesObjectives based on ClassStructure : " + informationPoint);
                    classService.getChildrenOf(informationPoint.getClassName())
                            .forEach(child -> jamesObjectives.add(new JamesObjective(JamesObjective.ObjectiveType.REMOVE, child, informationPoint)));
                } else {
                    // information point is declared on simple class - we add information for every class in every classloader
                    LOG.trace("JamesHQ - preparing simple JamesObjectives " + informationPoint);
                    classService.getAllClasses(informationPoint.getClassName())
                            .forEach(clazz -> jamesObjectives.add(new JamesObjective(JamesObjective.ObjectiveType.REMOVE, clazz, informationPoint)));
                }
            }
        }
    }

    private void processNewInformationPoints() {
        // new ip
        // on already processed classes
        Stopwatch informationPointsProcessingStopwatch = Stopwatch.createStarted();
        while (!addInformationPointQueue.isEmpty()) { //FIXME - starvation issue - so many IP or new classes that everything stops ????
            InformationPoint informationPoint = addInformationPointQueue.poll();
            if (informationPoint != null) {
                LOG.trace("JamesHQ - processing new InformationPoint : " + informationPoint);
                if (classService.getChildrenOf(informationPoint.getClassName()).size() > 0) {
                    // information point is declared on interaface or abstract class - we add information point for every child
                    LOG.trace("JamesHQ - preparing JamesObjectives based on ClassStructure : " + informationPoint);
                    classService.getChildrenOf(informationPoint.getClassName())
                            .forEach(child -> jamesObjectives.add(new JamesObjective(JamesObjective.ObjectiveType.ADD, child, informationPoint)));
                } else {
                    // information point is declared on simple class - we add information for every class in every classloader
                    LOG.trace("JamesHQ - preparing simple JamesObjectives " + informationPoint);
                    classService.getAllClasses(informationPoint.getClassName())
                            .forEach(clazz -> jamesObjectives.add(new JamesObjective(JamesObjective.ObjectiveType.ADD, clazz, informationPoint)));
                }
            }
        }
        informationPointsProcessingStopwatch.stop();
        LOG.trace("JamesHQ - all new InformationPoints processing time = " + informationPointsProcessingStopwatch.elapsed());
    }


}
