package com.tomtom.james.newagent;

import com.fasterxml.jackson.databind.util.ClassUtil;
import com.google.common.base.Stopwatch;
import com.tomtom.james.common.api.ClassScanner;
import com.tomtom.james.common.log.Logger;
import com.tomtom.james.newagent.tools.ClassQueue;
import com.tomtom.james.newagent.tools.ClassStructure;

import java.lang.instrument.Instrumentation;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * responsible for maintenace of the all class map, and structure of parents and children
 */
public class JamesClassScanner extends Thread implements ClassScanner {
    private static final Logger LOG = Logger.getLogger(JamesClassScanner.class);
    private long initialDelay = 10000;
    private long scanPeriod = 5000;
    private ClassStructure processedClasses;
    private ClassStructure classStructure;
    private ClassQueue newClassQueue;
    private Collection<String> ignoredPackages;

    public JamesClassScanner(ClassQueue newClassQueue, ClassStructure processedClasses, ClassStructure classStructure, Collection<String> ignoredPackages, long initialDelay, long scanPeriod) {
        this.newClassQueue = newClassQueue;
        this.classStructure = classStructure;
        this.initialDelay = initialDelay;
        this.scanPeriod = scanPeriod;
        this.ignoredPackages = ignoredPackages;
        this.processedClasses = processedClasses;
        this.setDaemon(true);
        this.setName(getClass().getSimpleName());
        LOG.trace("JamesClassScanner : initDelay [" + initialDelay + "ms] : scanPeriod [" + scanPeriod + "ms]: ignoredPackages = " + ignoredPackages.stream().collect(Collectors.joining(", ")));
    }

    /**
     * get parent interfaces and superclasses
     * update classStructure
     *
     * @return
     */
    private void processClass(Class clazz) {

        if (!clazz.isInterface()) { // interface can not be child - because every method.isEmpty == true, abstractClass could be ...
            Set<Class<?>> parentClassesAndInterfaces = new HashSet<>();
            parentClassesAndInterfaces.addAll(ClassUtil.findRawSuperTypes(clazz, null, false));
            parentClassesAndInterfaces.stream()
                    .filter(c -> ignoredPackages.stream().filter(pack -> c.getName().startsWith(pack)).findFirst().orElse(null) == null) // remove ignored packages // TODO is (.orElse(null) == null) == !.isPresent() ?????????????
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
                    if (ignoredPackages.stream().filter(pack -> c.getName().startsWith(pack)).findFirst().orElse(null) != null) { // TODO is (.orElse(null) == null) == !.isPresent() ?????????????
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
        Optional<Instrumentation> inst = JVMAgent.getInstrumentation();
        if (inst.isPresent()) {
            return inst.get();
        }
        throw new RuntimeException(" JAMES has found that 'instrumentation' is not ready (null) !!!!");
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
            LOG.error("JamesClassScanner thread has been interrupted !");
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

    @Override
    public Collection<String> getIgnoredPackages() {
        return ignoredPackages;
    }

    @Override
    public Collection<String> getClassStructureInfos() {
        return classStructure.getMap().entrySet().stream().map(e -> e.getKey() + " :: " + e.getValue().size()).collect(Collectors.toSet());
    }

    @Override
    public Collection<String> getProcessedClassesInfos() {
        return processedClasses.getMap().entrySet().stream().map(e -> e.getKey() + " :: " + e.getValue().size()).collect(Collectors.toSet());
    }

    @Override
    public Map<String, String> getStatistics() {
        Map<String, String> map = new HashMap<>();
        map.put("processedClassesSize", String.valueOf(processedClasses.getMap().size()));
        map.put("classStructureSize", String.valueOf(classStructure.getMap().size()));
        map.put("ignoredPackagesSize", String.valueOf(ignoredPackages.size()));
        return map;
    }
}
