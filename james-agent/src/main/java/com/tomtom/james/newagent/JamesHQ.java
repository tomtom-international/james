package com.tomtom.james.newagent;

import com.google.common.base.Stopwatch;
import com.tomtom.james.common.api.informationpoint.InformationPoint;
import com.tomtom.james.common.api.informationpoint.InformationPointService;
import com.tomtom.james.common.log.Logger;
import com.tomtom.james.newagent.james.GroovyJames;
import com.tomtom.james.newagent.james.TimingJames;
import com.tomtom.james.newagent.tools.InformationPointQueue;
import com.tomtom.james.newagent.tools.NewClassQueue;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

public class JamesHQ implements Runnable {
    private static final Logger LOG = Logger.getLogger(JamesHQ.class);
    private int initialDelay = 10000;
    private int scanPeriod = 1000;
    private InformationPointService informationPointService;
    private ClassService classService;
    private NewClassQueue newClassesQueue;
    private InformationPointQueue addInformationPointQueue;
    private InformationPointQueue removeInformationPointQueue;
    private Queue<JamesObjective> jamesObjectives = new ConcurrentLinkedQueue<>();
    private Thread james;

    public JamesHQ(InformationPointService informationPointService, ClassService classService, InformationPointQueue addInformationPointQueue, InformationPointQueue removeInformationPointQueue, NewClassQueue newClassQueue, int scanPeriod, int initialDelay) {
        this.scanPeriod = scanPeriod;
        this.initialDelay = initialDelay;
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

        //james = new Thread(new TextJames(jamesObjectives, 1000));
        //james = new Thread(new TimingJames(jamesObjectives, 1000));
        james = new Thread(new GroovyJames(jamesObjectives, 1000));
        james.setDaemon(true);
        james.start();

        while (true) {
            Stopwatch stopwatch = Stopwatch.createStarted();

            LOG.trace("JamesHQ :: addInformationPointQueue [" + addInformationPointQueue.size() + "] | newClassQueue [" + newClassesQueue.size() + "] ");

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


            Stopwatch newClassesProcessingStopwatch = Stopwatch.createStarted();
            while (!newClassesQueue.isEmpty()) {
                Class newClazz = newClassesQueue.poll();
                if (newClazz != null) {
                    if (classService.getChildrenOf(newClazz.getName()).size() > 0) {
                        // this means that newClazz is interface or abstract class and we put information points on every child
                        LOG.trace("JamesHQ - processing new interface or abstract class : " + newClazz.getName());

                        classService.getChildrenOf(newClazz.getName()).forEach(clazz -> informationPointService
                                .getInformationPoints(clazz.getName())
                                .forEach(informationPoint -> jamesObjectives.add(new JamesObjective(JamesObjective.ObjectiveType.ADD, clazz, informationPoint))));
                    } else {
                        // we get all information points for new class (by class name) and prepare Objectives
                        LOG.trace("JamesHQ - processing new class : " + newClazz.getName() + " :: size = [" + informationPointService.getInformationPoints(newClazz.getName()).size() + "]");
                        informationPointService.getInformationPoints(newClazz.getName())
                                .forEach(informationPoint -> jamesObjectives.add(new JamesObjective(JamesObjective.ObjectiveType.ADD, newClazz, informationPoint)));
                    }
                }
            }
            newClassesProcessingStopwatch.stop();
            LOG.trace("JamesHQ - all new Classes processing time = " + newClassesProcessingStopwatch.elapsed());


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


}
