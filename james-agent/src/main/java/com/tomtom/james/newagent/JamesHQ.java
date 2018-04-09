package com.tomtom.james.newagent;

import com.google.common.base.Stopwatch;
import com.tomtom.james.common.api.informationpoint.ExtendedInformationPoint;
import com.tomtom.james.common.api.informationpoint.InformationPoint;
import com.tomtom.james.common.api.informationpoint.InformationPointService;
import com.tomtom.james.common.log.Logger;
import com.tomtom.james.newagent.james.GroovyJames;
import com.tomtom.james.newagent.james.James;
import com.tomtom.james.newagent.tools.ClassQueue;
import com.tomtom.james.newagent.tools.InformationPointQueue;
import org.apache.commons.lang3.ClassUtils;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

public class JamesHQ extends Thread {
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
    private James james;

    // TODO builder
    public JamesHQ(InformationPointService informationPointService, ClassService classService, InformationPointQueue addInformationPointQueue, InformationPointQueue removeInformationPointQueue, ClassQueue newClassQueue, long initialDelay, long scanPeriod, long jamesInterval) {
        this.scanPeriod = scanPeriod;
        this.initialDelay = initialDelay;
        this.jamesInterval = jamesInterval;
        this.informationPointService = informationPointService;
        this.classService = classService;
        this.newClassesQueue = newClassQueue;
        this.addInformationPointQueue = addInformationPointQueue;
        this.removeInformationPointQueue = removeInformationPointQueue;
        this.setDaemon(true);
    }

    private JamesObjective prepareObjectiveForSingleClass(Class clazz) {
        JamesObjective objective = new JamesObjective(clazz);
        // directly for given clazz
        if (informationPointService.getInformationPoints(clazz.getName()).size() > 0) {
            LOG.trace("------------------------------- " + clazz.getName() + " direct IP");
            for (InformationPoint informationPoint : informationPointService.getInformationPoints(clazz.getName())) {
                long clazzCheck = Arrays.stream(clazz.getDeclaredMethods())
                        .filter(method -> method.getName().equals(informationPoint.getMethodName()))
                        .filter(method -> !Modifier.isAbstract(method.getModifiers()) && !Modifier.isInterface(method.getModifiers())) // FIXME check if method in interface has Modifier.isInterface(..) == true
                        .count();
                if (clazzCheck > 0) {
                    LOG.trace("---------------------------------------[o]" + informationPoint);
                    objective.addInformationPoint(new ExtendedInformationPoint(informationPoint, clazz.getName()));
                } // else this is interface or abstract method
            }
        }

        List<Class<?>> superIC = ClassUtils.getAllSuperclasses(clazz);
        superIC.addAll(ClassUtils.getAllInterfaces(clazz));
        for (Class superClassOrInterface : superIC) {
            // look for ip
            if (informationPointService.getInformationPoints(superClassOrInterface.getName()).size() > 0) {
                // there are ip for given superClassOrInterface
                for (InformationPoint informationPoint : informationPointService.getInformationPoints(superClassOrInterface.getName())) {
                    long superClassCheck = Arrays.stream(superClassOrInterface.getDeclaredMethods())
                            .filter(method -> method.getName().equals(informationPoint.getMethodName()))
                            .filter(method -> Modifier.isAbstract(method.getModifiers()) || Modifier.isInterface(method.getModifiers())) // FIXME check if method in interface has Modifier.isInterface(..) == true
                            .count();
                    if (superClassCheck > 0) {
                        // this IP is defined on interface of abstract method and should be instrumented in clazz
                        // but first we need to check if there is any non abstract/interface method in clazz
                        long clazzCheck = Arrays.stream(clazz.getDeclaredMethods())
                                .filter(method -> method.getName().equals(informationPoint.getMethodName()))
                                .filter(method -> !Modifier.isAbstract(method.getModifiers()) && !Modifier.isInterface(method.getModifiers())) // FIXME check if method in interface has Modifier.isInterface(..) == true
                                .count();
                        if (clazzCheck > 0) {
                            // there are IP on superClass on abstract/interface method and implemented methods in sublcass
                            LOG.trace("------------------------------[o] " + clazz.getName() + " inherited IP : " + informationPoint);
                            objective.addInformationPoint(new ExtendedInformationPoint(informationPoint, clazz.getName()));
                        }
                    }
                }
            }
        }
        return objective;
    }

    private List<JamesObjective> prapareObjectiveForSignleInformationPoint(InformationPoint informationPoint) {
        List<JamesObjective> objectives = new ArrayList<>();
        LOG.trace("--------[ip] " + informationPoint);
        // directrly for given czas ?
        for(Class clazz : classService.getAllClasses(informationPoint.getClassName())) {
            LOG.trace("---------------[ofc] " + clazz.getName() + " direct IP : " + informationPoint );
            objectives.add(prepareObjectiveForSingleClass(clazz));
        }

        // for all children
        for(Class child : classService.getChildrenOf(informationPoint.getClassName())) {
            LOG.trace("---------------[ofc] " + child.getName() + " child IP : " + informationPoint );
            objectives.add(prepareObjectiveForSingleClass(child));
        }
        return objectives;
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
        LOG.trace("processNewClass add needs redefine " + counter + " classes.");
        return counter;
    }

    private int processAddInformationPoint() {
        int counter = 0;
        while (!addInformationPointQueue.isEmpty()) {
            InformationPoint informationPoint = addInformationPointQueue.poll();
            if (informationPoint != null) {
                List<JamesObjective> objectives = prapareObjectiveForSignleInformationPoint(informationPoint);
                counter += objectives.size();
                jamesObjectives.addAll(objectives);
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
            LOG.warn("Initial delay has been interrupted !!!");
        }

        james = new GroovyJames(jamesObjectives, jamesInterval);
        james.start();

        while (true) {
            Stopwatch stopwatch = Stopwatch.createStarted();

            LOG.trace("JamesHQ :: addInformationPointQueue [" + addInformationPointQueue.size() + "] | removeInformationPoint [" + removeInformationPointQueue.size() + "] | newClassQueue [" + newClassesQueue.size() + "] ");

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
                LOG.warn("ScanPeriod sleep has been interrupted !");
            }
        }
    }

}
