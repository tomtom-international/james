package com.tomtom.james.newagent;

import com.google.common.base.Stopwatch;
import com.tomtom.james.common.log.Logger;
import org.apache.commons.lang3.ClassUtils;

import java.lang.instrument.Instrumentation;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * responsible for maintenace of the all class map, and structure of parents and children
 */
public class JamesClassScanner implements Runnable {
    private static final Logger LOG = Logger.getLogger(JamesClassScanner.class);
    private int initialDelay = 10000;
    private int scanPeriod = 5000;
    private ClassStructure processedClasses = new BasicClassStructure();
    private ClassStructure classStructure;
    private NewClassQueue newClassQueue;

    public JamesClassScanner(NewClassQueue newClassQueue, ClassStructure classStructure, int initialDelay, int scanPeriod) {
        this.newClassQueue = newClassQueue;
        this.classStructure = classStructure;
        this.initialDelay = initialDelay;
        this.scanPeriod = scanPeriod;
    }

    @SuppressWarnings("unused")
    private void logCurrentClassStructure() {
        LOG.trace("--class structure begin -----------------------------------------------------------------------------");
        classStructure.getMap().forEach((className, children) -> {
            LOG.trace("     " + className);
            children.forEach(child -> LOG.trace("          - " + child.getName() + " ::: " + child.toString()));
        });
        LOG.trace("--class structure end -------------------------------------------------------------------------------");
    }


    /**
     * get parent interfaces and superclasses
     * update classStructure
     *
     * @param source
     * @return
     */
    private void processClass(Class clazz) {
        if (!clazz.isInterface()) { // interface can not be child - because every method.isEmpty == true, abstractClass could be ...
            Set<Class<?>> parentClassesAndInterfaces = new HashSet<>();
            parentClassesAndInterfaces.addAll(ClassUtils.getAllInterfaces(clazz)); // interfaces
            parentClassesAndInterfaces.addAll(ClassUtils.getAllSuperclasses(clazz)); // superclasses
            for (Class parentClass : parentClassesAndInterfaces) {
                if (classStructure.contains(parentClass.getName())) {
                    processClass(parentClass); // FIXME recurrent call !!!!!!!!!!!!!!!!!!!!
                }
                if (parentClass.isInterface() || Modifier.isAbstract(parentClass.getModifiers())) {
                    // we cache children only for interfaces and abstract classes
                    classStructure.addChild(parentClass.getName(), clazz);
                }
            }
        }
    }

    private void processClasses(List<Class> clazz) {
        clazz.stream().forEach(this::processClass);
    }

    /**
     * 0. gets all loaded classes
     * 1. prepare delta - only new loaded classes
     * 2. process whole delta - prepare list of parents and children for delta classes, and update globalClassStructure
     * 3. push delta to queue
     *
     * @param source
     * @return
     */
    @Override
    public void run() {
        LOG.trace("JamesClassScanner ready and sleep " + initialDelay + " ms of initialDelay.");
        Instrumentation instrumentation = JVMAgent.getInstrumentation();
        if (instrumentation == null) {
            throw new RuntimeException(" JAMES has found that 'instrumentation' is not ready (null) !!!!");
        }
        while (true) {
            LOG.trace("JamesClassScanner - scan started.");
            Stopwatch stopwatch = Stopwatch.createStarted();
            // FIXME - optimize getting delta
            List<Class> newScan = Arrays.asList(instrumentation.getAllLoadedClasses());
            // removed already processed classes
            newScan.removeAll(processedClasses
                    .values()
                    .stream()
                    .flatMap(Collection::stream)
                    .collect(Collectors.toList()));
            LOG.trace(String.format("JamesClassScanner - delta size : %h  from %h processing delta time = %h", newScan.size(), newScan.size(), stopwatch.elapsed().toString()));

            // build ClassStructure
            Stopwatch processingStopWatch = Stopwatch.createStarted();
            processClasses(newScan); // process and prepare parents and children
            processingStopWatch.stop();
            LOG.trace("JamesClassScanner - class processing time = " + processingStopWatch.elapsed());

            // add new classes to processed classes
            newScan.forEach(clazz -> processedClasses.addChild(clazz.getName(), clazz));

            //logCurrentClassStructure(); // FIXME set if log level is trace

            //pass all classes to the Queue for HQ processing (process class from queue versus all information points and check if any changes is needed)
            newClassQueue.addAll(newScan); // put all processed to queue
            stopwatch.stop();
            LOG.debug("JamesClassScanner - finished scan time = " + stopwatch.elapsed());
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
