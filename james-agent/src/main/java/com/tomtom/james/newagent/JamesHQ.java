package com.tomtom.james.newagent;

import com.google.common.base.Stopwatch;
import com.tomtom.james.common.api.informationpoint.InformationPoint;
import com.tomtom.james.common.api.informationpoint.InformationPointService;
import com.tomtom.james.common.log.Logger;
import com.tomtom.james.newagent.james.TextJames;
import com.tomtom.james.newagent.james.TimingJames;
import com.tomtom.james.newagent.tools.NewClassQueue;
import com.tomtom.james.newagent.tools.NewInformationPointQueue;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

public class JamesHQ implements Runnable {
    private static final Logger LOG = Logger.getLogger(JamesHQ.class);
    private int scanPeriod = 1000;
    private InformationPointService informationPointService;
    private ClassService classService;
    private NewClassQueue newClassesQueue;
    private NewInformationPointQueue newInformationPointQueue;
    private Queue<JamesObjective> jamesObjectives = new ConcurrentLinkedQueue<>();
    private Thread james;

    public JamesHQ(InformationPointService informationPointService, ClassService classService, NewInformationPointQueue newInformationPointQueue, NewClassQueue newClassQueue, int scanPeriod) {
        this.scanPeriod = scanPeriod;
        this.informationPointService = informationPointService;
        this.classService = classService;
        this.newClassesQueue = newClassQueue;
        this.newInformationPointQueue = newInformationPointQueue;
    }

    @Override
    public void run() {
        // start James
        //james = new Thread(new TextJames(jamesObjectives, 1000));
        james = new Thread(new TimingJames(jamesObjectives, 1000));
        james.setDaemon(true);
        james.start();

        while (true) {
            Stopwatch stopwatch = Stopwatch.createStarted();

            LOG.trace("JamesHQ :: newInformationPointQueue [" + newInformationPointQueue.size() + "] | newClassQueue [" + newClassesQueue.size() + "] ");

            // new ip
            // on already processed classes
            Stopwatch informationPointsProcessingStopwatch = Stopwatch.createStarted();
            while (!newInformationPointQueue.isEmpty()) { //FIXME - starvation issue - so many IP or new classes that everything stops ????
                InformationPoint informationPoint = newInformationPointQueue.poll();
                if (informationPoint != null) {
                    LOG.trace("JamesHQ - processing new InformationPoint : " + informationPoint);
                    if (classService.getChildrenOf(informationPoint.getClassName()).size() > 0) {
                        // information point is declared on interaface or abstract class - we add information point for every child
                        LOG.trace("JamesHQ - preparing JamesObjectives based on ClassStructure : " + informationPoint);
                        classService.getChildrenOf(informationPoint.getClassName())
                                .forEach(child -> jamesObjectives.add(new JamesObjective(child, informationPoint)));
                    } else {
                        // information point is declared on simple class - we add information for every class in every classloader
                        LOG.trace("JamesHQ - preparing simple JamesObjectives " + informationPoint);
                        classService.getAllClasses(informationPoint.getClassName())
                                .forEach(clazz -> jamesObjectives.add(new JamesObjective(clazz, informationPoint)));
                    }

                }
            }
            informationPointsProcessingStopwatch.stop();
            LOG.trace("JamesHQ - all new InformationPoints processing time = " + informationPointsProcessingStopwatch.elapsed());

            Stopwatch newClassesProcessingStopwatch = Stopwatch.createStarted();
            while (!newClassesQueue.isEmpty()) {
                Class newClazz = newClassesQueue.poll();
                if (newClazz != null) {
                    if (classService.getChildrenOf(newClazz.getName()).size() > 0) {
                        // this means that newClazz is interface or abstract class and we put information points on every child
                        LOG.trace("JamesHQ - processing new interface or abstract class : " + newClazz.getName());

                        classService.getChildrenOf(newClazz.getName()).forEach(clazz -> informationPointService
                                .getInformationPoints(clazz.getName())
                                .forEach(informationPoint -> jamesObjectives.add(new JamesObjective(clazz, informationPoint))));
                    } else {
                        // we get all information points for new class (by class name) and prepare Objectives
                        LOG.trace("JamesHQ - processing new class : " + newClazz.getName());
                        informationPointService.getInformationPoints(newClazz.getName())
                                .forEach(informationPoint -> jamesObjectives.add(new JamesObjective(newClazz, informationPoint)));
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
