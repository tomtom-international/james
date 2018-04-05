package com.tomtom.james.newagent;

import com.google.common.base.Stopwatch;
import com.tomtom.james.common.api.informationpoint.InformationPoint;
import com.tomtom.james.common.api.informationpoint.InformationPointService;
import com.tomtom.james.common.log.Logger;
import com.tomtom.james.newagent.james.GroovyJames;
import com.tomtom.james.newagent.tools.ClassQueue;
import com.tomtom.james.newagent.tools.InformationPointQueue;
import org.apache.commons.lang3.ClassUtils;

import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class JamesHQ implements Runnable {
    private static final Logger LOG = Logger.getLogger(JamesHQ.class);
    private long initialDelay = 10000;
    private long scanPeriod = 1000;
    private long jamesInterval = 1000;
    private InformationPointService informationPointService;
    private ClassService classService;
    private ClassQueue newClassesQueue;
    private InformationPointQueue addInformationPointQueue;
    private InformationPointQueue removeInformationPointQueue;
    private Queue<JamesObjective> jamesObjectives = new ArrayBlockingQueue<>(10000);
    private Thread james;

    public JamesHQ(InformationPointService informationPointService, ClassService classService, InformationPointQueue addInformationPointQueue, InformationPointQueue removeInformationPointQueue, ClassQueue newClassQueue, long initialDelay, long scanPeriod, long jamesInterval) {
        this.scanPeriod = scanPeriod;
        this.initialDelay = initialDelay;
        this.jamesInterval = jamesInterval;
        this.informationPointService = informationPointService;
        this.classService = classService;
        this.newClassesQueue = newClassQueue;
        this.addInformationPointQueue = addInformationPointQueue;
        this.removeInformationPointQueue = removeInformationPointQueue;
    }

    private JamesObjective prepareObjectiveForSingleClass(Class clazz) {
        // simple class
        JamesObjective objective = new JamesObjective(clazz);
        for (InformationPoint informationPoint : informationPointService.getInformationPoints(clazz.getName())) {
            if (!clazz.isInterface() || !Modifier.isAbstract(clazz.getModifiers())) {
                objective.addInformationPoint(informationPoint);
            }
        }
        // prepare IP for every parent class or interface
        List<Class<?>> interfacesAndSuperClasses = new ArrayList<>();
        interfacesAndSuperClasses.addAll(ClassUtils.getAllInterfaces(clazz));
        interfacesAndSuperClasses.addAll(ClassUtils.getAllSuperclasses(clazz));
        for (Class superClass : interfacesAndSuperClasses) {
            if (informationPointService.getInformationPoints(superClass.getName()).size() > 0) {
                for (InformationPoint informationPoint : informationPointService.getInformationPoints(superClass.getName())) {
                    if (superClass.isInterface()) {
                        // interface
                        objective.addInformationPoint(informationPoint);
                    } else if (Modifier.isAbstract(superClass.getModifiers())) {
                        // abstract class
                        long methodsCount = Arrays.stream(superClass.getMethods())
                                .filter(m -> m.getName().equals(informationPoint.getMethodName())) // methods with name from IP
                                .filter(m -> Modifier.isAbstract(m.getModifiers())) // only abstract
                                .count();
                        if (methodsCount > 0) {
                            objective.addInformationPoint(informationPoint);
                        }
                    }
                    // else - normal class
                }
            }
        }
        return objective;
    }

    private int processNewClass() {
        int counter = 0;
        while (!newClassesQueue.isEmpty()) {
            Class clazz = newClassesQueue.poll();
            if (clazz != null) {
                JamesObjective objective = prepareObjectiveForSingleClass(clazz);
                if (objective.getInformationPoints().size() > 0) { // is anything to do ?
                    jamesObjectives.add(objective); // put it to the queue
                }
                counter++;
            }
        }
        return counter;
    }

    private int prapareObjectiveForSignleInformationPoint(InformationPoint informationPoint) {
        int counter = 0;
        LOG.error("----------------------- processing Information Point :  " + informationPoint);
        // for given classname
        for (Class clazz : classService.getAllClasses(informationPoint.getClassName())) {
            LOG.error("------------------------------------------ class : " + clazz.getName());
            JamesObjective objective = prepareObjectiveForSingleClass(clazz);
            if (objective.getInformationPoints().size() > 0) { // if there is anything to do
                jamesObjectives.add(objective);
            }
            // removes all duplicates - duplicate is ip on the same class, because 'prepareObjectiveForSingleClass' gathers all ip for all methods in single JamesObjective
            List<InformationPoint> list = addInformationPointQueue.stream().filter(ip -> objective.getInformationPoints().stream().filter(alreadyProcessed -> ip.getClassName().equals(alreadyProcessed.getClassName())).count() > 0).collect(Collectors.toList());
            addInformationPointQueue.removeAll(list);
        }
        // for all childs
        Set<Class> classes = classService.getChildrenOf(informationPoint.getClassName()); // there we get all childs of interface
        for (Class clazz : classes) {
            LOG.error("------------------------------------------ class : " + clazz.getName());
            JamesObjective objective = prepareObjectiveForSingleClass(clazz);
            if (objective.getInformationPoints().size() > 0) { // if there is anything to do
                jamesObjectives.add(objective);
            }
            // removes all duplicates - duplicate is ip on the same class, because 'prepareObjectiveForSingleClass' gathers all ip for all methods in single JamesObjective
            List<InformationPoint> list = addInformationPointQueue.stream().filter(ip -> objective.getInformationPoints().stream().filter(alreadyProcessed -> ip.getClassName().equals(alreadyProcessed.getClassName())).count() > 0).collect(Collectors.toList());
            addInformationPointQueue.removeAll(list);
        }

        return counter;
    }

    private int processAddInformationPoint() {
        int counter = 0;
        while (!addInformationPointQueue.isEmpty()) {
            InformationPoint informationPoint = addInformationPointQueue.poll();
            if (informationPoint != null) {
                counter += prapareObjectiveForSignleInformationPoint(informationPoint);
            }
        }
        LOG.trace("processInformationPoints add needs redefine " + counter + " classes.");
        return counter;
    }

    private int processRemoveInformationPoint() {
        int counter = 0;
        if (!removeInformationPointQueue.isEmpty()) {
            LOG.trace("Remove InformationPoints: queue length = " + removeInformationPointQueue.size());
            while (!removeInformationPointQueue.isEmpty()) {
                InformationPoint informationPoint = removeInformationPointQueue.poll();
                if (informationPoint != null) {
                    LOG.trace(" remove InformationPoint: " + informationPoint);
                    counter += classService.getAllClasses(informationPoint.getClassName())
                            .stream()
                            .peek(clazz -> jamesObjectives.add(prepareObjectiveForSingleClass(clazz)))
                            .count();
                    counter += classService.getChildrenOf(informationPoint.getClassName())
                            .stream()
                            .peek(clazz -> jamesObjectives.add(prepareObjectiveForSingleClass(clazz)))
                            .count();
                }
            }
            LOG.trace("Remove InformationPoints needs redefine " + counter + " classes.");
        }
        return counter;
    }

    @Override
    public void run() {
        // start James
        try {
            Thread.sleep(initialDelay);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // FIXME clean this shit
        //james = new Thread(new OLD_____GroovyJames(jamesObjectives, jamesInterval));
        //james = new Thread(new TextJames(jamesObjectives, jamesInterval));
        james = new Thread(new GroovyJames(jamesObjectives, jamesInterval));
        //james = new Thread(new TimingJames(jamesObjectives, jamesInterval));
        james.setDaemon(true);
        james.start();

        while (true) {
            Stopwatch stopwatch = Stopwatch.createStarted();

            LOG.trace("JamesHQ :: addInformationPointQueue [" + addInformationPointQueue.size() + "] | newClassQueue [" + newClassesQueue.size() + "] ");

            int removedIPCounter = processRemoveInformationPoint();
            int newIPCounter = processAddInformationPoint();
            int newClassesCounter = processNewClass();

            stopwatch.stop();
            LOG.debug("JamesHQ [newIP:" + newIPCounter + ", removedIP:" + removedIPCounter + ", newClasses:" + newClassesCounter + "] scan time = " + stopwatch.elapsed());
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
