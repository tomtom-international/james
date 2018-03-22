package com.tomtom.james.newagent;

import com.google.common.base.Stopwatch;
import com.tomtom.james.common.log.Logger;
import org.apache.commons.lang3.ClassUtils;

import java.lang.instrument.Instrumentation;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * responsible for maintenace of the all class map, and structure of parents and children
 */
public class JamesClassScanner implements Runnable {
    private static final Logger LOG = Logger.getLogger(JamesClassScanner.class);
    private int initialDelay = 10000;
    private int scanPeriod = 5000;
    private GlobalClassStructure globalClassStructure;
    private NewClassQueue newClassQueue;

    public JamesClassScanner(NewClassQueue newClassQueue, GlobalClassStructure globalClassStructure, int initialDelay, int scanPeriod) {
        this.newClassQueue = newClassQueue;
        this.globalClassStructure = globalClassStructure;
        this.initialDelay = initialDelay;
        this.scanPeriod = scanPeriod;
    }


    /**
     * get parent interfaces and superclasses
     * update globalClassStructure
     *
     * @param source
     * @return
     */
    private ClassDescriptor processClass(Class<?> clazz) {

        Set<Class<?>> parentClassesAndInterfaces = new HashSet<>();
        parentClassesAndInterfaces.addAll(ClassUtils.getAllInterfaces(clazz)); // interfaces
        parentClassesAndInterfaces.addAll(ClassUtils.getAllSuperclasses(clazz)); // superclasses
        parentClassesAndInterfaces.forEach(parent -> {
            if(!globalClassStructure.contains(parent)) {
                processClass(parent); // FIXME - possible fuckup - recurrency !!!!!!
            }
            // FIXME - think twice - do we have to add interface as a child of interfacess ??? (.. I suppose - NO )
            // add clazz as a child of parents but only for abstract classes and interfaces
            if (parent.isInterface() || Modifier.isAbstract(parent.getModifiers())) {
                globalClassStructure.addChild(parent, clazz);
            }
        });
        globalClassStructure.addEmpty(clazz);
    }

    private void processClasses(List<Class<?>> source) {
        source.stream().forEach(this::processClass);
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
            newScan.removeAll(globalClassStructure.keySet()); // removed already processed classes
            LOG.trace(String.format("JamesClassScanner - delta size : %h  from %h processing delta time = %h", newScan.size(), newScan.size(), stopwatch.elapsed().toString()));

            Stopwatch processingStopWatch = Stopwatch.createStarted();
            List<ClassDescriptor> delta = processClasses(newScan); // process and prepare parents and children
            processingStopWatch.stop();
            LOG.trace("JamesClassScanner - class processing time = " + processingStopWatch.elapsed());

            newClassQueue.addAll(delta); // put all processed to queue
            stopwatch.stop();
            LOG.trace("JamesClassScanner - finished scan time = " + stopwatch.elapsed());
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
