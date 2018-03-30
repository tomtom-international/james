package com.tomtom.james.newagent;

import com.google.common.base.Stopwatch;
import com.tomtom.james.common.log.Logger;
import com.tomtom.james.newagent.tools.ClassStructure;
import com.tomtom.james.newagent.tools.NewClassQueue;
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
    private long initialDelay = 10000;
    private long scanPeriod = 5000;
    private ClassStructure processedClasses;
    private ClassStructure classStructure;
    private NewClassQueue newClassQueue;
    private Collection<String> ignoredPackages;

    public JamesClassScanner(NewClassQueue newClassQueue, ClassStructure processedClasses, ClassStructure classStructure, Collection<String> ignoredPackages, long initialDelay, long scanPeriod) {
        this.newClassQueue = newClassQueue;
        this.classStructure = classStructure;
        this.initialDelay = initialDelay;
        this.scanPeriod = scanPeriod;
        this.ignoredPackages = ignoredPackages;
        this.processedClasses = processedClasses;
        LOG.trace("JamesClassScanner : initDelay [" + initialDelay + "ms] : scanPeriod [" + scanPeriod + "ms]: ignoredPackages = " + ignoredPackages.stream().collect(Collectors.joining(", ")));
    }

    @SuppressWarnings("unused")
    private void logCurrentClassStructure(ClassStructure data) {
        LOG.trace("--class structure begin -----------------------------------------------------------------------------");
        data.getMap().forEach((className, children) -> {
            System.out.println("     " + className);
            children.forEach(child -> System.out.println("          - " + child.getName() + " ::: " + child.toString()));
        });
        LOG.trace("--class structure end -------------------------------------------------------------------------------");
    }

    @SuppressWarnings("unused")
    private void logCurrentClassStructure(ClassStructure data, String root) {
        LOG.trace("-- start @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@--");
        for (String key : data.getMap().keySet().stream().filter(c -> c.startsWith(root)).collect(Collectors.toList())) {
            Set<Class> set = data.getChildren(key);
            LOG.trace(" [" + key + "] " + set.size());
            for (Class c : set) {
                LOG.trace("               :: " + c.getName());
            }
        }
        LOG.trace("-- end @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@--");
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
            parentClassesAndInterfaces.stream()
                    .filter(c -> ignoredPackages.stream().filter(pack -> c.getName().startsWith(pack)).findFirst().orElse(null) == null) // remove ignored packages
                    .forEach(c -> {
                        if (c.isInterface() || Modifier.isAbstract(c.getModifiers())) {
                            classStructure.addChild(c.getName(), clazz);
                        }
                    });
        }
    }

    private void processClasses(Collection<Class> clazz) {
        Stopwatch processingStopWatch = Stopwatch.createStarted();
        clazz.stream().forEach(this::processClass);
        processingStopWatch.stop();
        LOG.trace("JamesClassScanner - all new classes processing time = " + processingStopWatch.elapsed());
    }


    private Set<Class> prepareDelta(Class[] newScan) {
        Stopwatch deltaStopwatch = Stopwatch.createStarted();
        Set<Class> alreadyProcessed = processedClasses.values().stream().flatMap(Collection::stream).collect(Collectors.toSet());
        // calculating delta = newscan without ( already processedClasses and ignored packages )
        Set<Class> delta = Arrays.asList(newScan)
                .stream()
                .filter(c -> {
                    // check if it's already processed
                    if (alreadyProcessed.contains(c)) {
                        return false;
                    }
                    // check if it's in ignored packages - from configuration
                    if (ignoredPackages.stream().filter(pack -> c.getName().startsWith(pack)).findFirst().orElse(null) != null) {
                        return false;
                    }
                    return true;
                })
                .collect(Collectors.toSet());
        deltaStopwatch.stop();
        LOG.trace(String.format("JamesClassScanner - delta size : %d  from %d processing delta time = %s | class structure size %d", delta.size(), newScan.length, deltaStopwatch.elapsed(), alreadyProcessed.size()));
        return delta;
    }

    private Instrumentation getInstrumentation() {
        Instrumentation instrumentation = JVMAgent.getInstrumentation();
        if (instrumentation == null) {
            throw new RuntimeException(" JAMES has found that 'instrumentation' is not ready (null) !!!!");
        }
        return instrumentation;
    }

    private void waitThere(Long period) {
        waitThere(period, 0L);
    }

    private void waitThere(long period, long elapsed) {
        try {
            if ((period - elapsed) > 0) {
                Thread.sleep(scanPeriod - elapsed);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * 0. gets all loaded classes
     * 1. prepare delta - only new loaded classes
     * 2. process whole delta - prepare list of parents and children for delta classes, and update globalClassStructure
     * 3. push delta to queue
     *
     * @return
     */
    @Override
    public void run() {
        LOG.trace("JamesClassScanner ready and sleep " + initialDelay + " ms of initialDelay.");
        Instrumentation instrumentation = getInstrumentation();
        waitThere(initialDelay);
        while (true) {
            Stopwatch stopwatch = Stopwatch.createStarted();
                // prepare delta - new classes, not already processed and not from ignored packages
                Set<Class> delta = prepareDelta(instrumentation.getAllLoadedClasses());

                // process delta - process every class and prepare parents and children structure
                processClasses(delta);

                // add new classes to processed classes
                delta.forEach(clazz -> processedClasses.addChild(clazz.getName(), clazz));

                //pass all classes to the Queue for HQ processing (process class from queue versus all information points and check if any changes is needed)
                newClassQueue.addAll(delta); // put all processed to queue
            stopwatch.stop();
            LOG.debug("JamesClassScanner - finished scan [delta size = " + delta.size() + "]time = " + stopwatch.elapsed());
            waitThere(scanPeriod, stopwatch.elapsed(TimeUnit.MILLISECONDS));
        }
    }

}
