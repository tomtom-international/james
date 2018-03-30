package com.tomtom.james.newagent;

import com.google.common.base.Stopwatch;
import com.google.common.io.Resources;
import com.tomtom.james.agent.ControllersManager;
import com.tomtom.james.agent.PluginManager;
import com.tomtom.james.agent.ShutdownHook;
import com.tomtom.james.agent.ToolkitManager;
import com.tomtom.james.common.api.informationpoint.InformationPointService;
import com.tomtom.james.common.api.publisher.EventPublisher;
import com.tomtom.james.common.api.script.ScriptEngine;
import com.tomtom.james.common.log.Logger;
import com.tomtom.james.configuration.AgentConfiguration;
import com.tomtom.james.configuration.AgentConfigurationFactory;
import com.tomtom.james.configuration.ConfigurationInitializationException;
import com.tomtom.james.informationpoint.InformationPointServiceImpl;
import com.tomtom.james.newagent.tools.*;
import com.tomtom.james.publisher.EventPublisherFactory;
import com.tomtom.james.script.ScriptEngineFactory;
import com.tomtom.james.store.InformationPointStore;
import com.tomtom.james.store.InformationPointStoreFactory;
import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.NotFoundException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.instrument.ClassDefinition;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.stream.Collectors;


public class JVMAgent {
    private static final Logger LOG = Logger.getLogger(JVMAgent.class);
    public static Instrumentation instrumentation = null;
    private static Thread jamesHQ;

    /**
     * do not change this method, don't even think about it !!!
     * @return
     */
    public static Instrumentation getInstrumentation() {
        try {
            if (instrumentation == null) {
                return (Instrumentation) ClassLoader.getSystemClassLoader()
                        .loadClass(JVMAgent.class.getName())
                        .getDeclaredField("instrumentation")
                        .get(null);
            } else {
                return instrumentation;
            }
        } catch (Exception e) {
            LOG.error("JVMAgent - instrumentation not found in SystemClassLoader, probably JVMAgent is not installed in the JVM.");
            return null;
        }
    }

    private static void printBanner(AgentConfiguration agentConfiguration) {
        if (!agentConfiguration.isQuiet()) {
            URL bannerURL = Resources.getResource(JVMAgent.class, "banner.txt");
            try {
                Resources.readLines(bannerURL, StandardCharsets.UTF_8).forEach(System.err::println);
            } catch (IOException e) {
                LOG.warn("Error reading banner resource, looks like something is wrong with the agent jar", e);
            }
        }
    }

    /**
     * The entry point invoked when this agent is started by {@code -javaagent}.
     */
    public static void premain(String agentArgs, Instrumentation inst) throws Throwable {
        LOG.trace("JVMAgent premain.");
        agentmain(agentArgs, inst);
    }

    /**
     * The entry point invoked when this agent is started after the JVM starts.
     */
    public static void agentmain(String agentArgs, Instrumentation inst) throws Throwable {
        LOG.trace("JVMAgent agentmain");
        if (!inst.isRedefineClassesSupported())
            throw new RuntimeException("this JVM does not support redefinition of classes");
        instrumentation = inst;
        setupAgent(instrumentation);
    }

    /**
     * should prepare whole james world to work
     * @param inst
     */
    private static void setupAgent(Instrumentation inst) {
        LOG.trace("JVMAgent agentmain");
        try {
            if (inst != null) {
                if (!inst.isRedefineClassesSupported()) {
                    throw new RuntimeException("this JVM does not support redefinition of classes");
                }
            }
            AgentConfiguration configuration = AgentConfigurationFactory.create();
            Logger.setCurrentLogLevel(configuration.getLogLevel());
            printBanner(configuration);

            Stopwatch stopwatch = Stopwatch.createStarted();

            PluginManager pluginManager = new PluginManager(configuration.getPluginIncludeDirectories(), configuration.getPluginIncludeFiles());
            LOG.trace("pluginManager time=" + stopwatch.elapsed());

            EventPublisher publisher = EventPublisherFactory.create(pluginManager, configuration.getPublishersConfigurations());
            LOG.trace("publisher time=" + stopwatch.elapsed());

            InformationPointStore store = InformationPointStoreFactory.create(configuration.getInformationPointStoreConfiguration());
            LOG.trace("store time=" + stopwatch.elapsed());

            ToolkitManager toolkitManager = new ToolkitManager(pluginManager, configuration.getToolkitsConfigurations());
            LOG.trace("toolkitManager time=" + stopwatch.elapsed());

            ScriptEngine engine = ScriptEngineFactory.create(publisher, configuration, toolkitManager);
            LOG.trace("engine time=" + stopwatch.elapsed());

            ControllersManager controllersManager = new ControllersManager(pluginManager, configuration.getControllersConfigurations());
            LOG.trace("controllerManager time=" + stopwatch.elapsed());

            InformationPointQueue addInformationPointQueue = new BasicInformationPointQueue();
            InformationPointQueue removeInformationPointQueue = new BasicInformationPointQueue();
            NewClassQueue newClassQueue = new BasicNewClassQueue();

            // iformation point provider
            InformationPointService informationPointService = new InformationPointServiceImpl(store, addInformationPointQueue, removeInformationPointQueue);
            LOG.trace("informationPointService time=" + stopwatch.elapsed());

            controllersManager.initializeControllers(informationPointService, engine, publisher);
            LOG.trace("initialize controllers time=" + stopwatch.elapsed());

            // ClassService - scans JVM loaded classes and put every new class to the newClassQuery
            LOG.trace("ClassService init :: ignoredPackages=" + configuration.getClassScannerConfiguration().getIgnoredPackages().stream().collect(Collectors.joining(", ")));
            ClassService classService = new BasicClassService(newClassQueue,
                    configuration.getClassScannerConfiguration().getIgnoredPackages(),
                    configuration.getClassScannerConfiguration().getInitialDelay(),
                    configuration.getClassScannerConfiguration().getScanPeriod());
            LOG.trace("classService time=" + stopwatch.elapsed());

            LOG.debug("JVMAgent - ClassService is executed.");

            jamesHQ = new Thread(
                        new JamesHQ(informationPointService,
                                classService,
                                addInformationPointQueue,
                                removeInformationPointQueue,
                                newClassQueue,
                                configuration.getJamesHQConfiguration().getInitialDelay(),
                                configuration.getJamesHQConfiguration().getScanPeriod(),
                                configuration.getJamesHQConfiguration().getJamesInterval()));
            jamesHQ.setDaemon(true);
            jamesHQ.start();
            LOG.trace("James HQ time=" + stopwatch.elapsed());
            LOG.debug("JVMAgent - JamesHQ is executed.");

            ShutdownHook shutdownHook = new ShutdownHook(controllersManager, engine, publisher, configuration);
            Runtime.getRuntime().addShutdownHook(shutdownHook);
            LOG.trace("shutdownHook time=" + stopwatch.elapsed());
            stopwatch.stop();
            LOG.info("JVMAgent - initialization complete - time=" + stopwatch.elapsed());

        } catch (ConfigurationInitializationException e) {
            e.printStackTrace();
        }
    }

    /**
     * Redefines a class.
     */
    public static void redefine(Class<?> oldClass, CtClass newClass) throws NotFoundException, IOException, CannotCompileException {
        Class<?>[] old = {oldClass};
        CtClass[] newClasses = {newClass};
        redefine(old, newClasses);
    }

    /**
     * Redefines classes.
     */
    public static void redefine(Class<?>[] oldClasses, CtClass[] newClasses) throws NotFoundException, IOException, CannotCompileException {
        startAgent();
        ClassDefinition[] defs = new ClassDefinition[oldClasses.length];
        for (int i = 0; i < oldClasses.length; i++)
            defs[i] = new ClassDefinition(oldClasses[i], newClasses[i].toBytecode());

        try {
            getInstrumentation().redefineClasses(defs);
        } catch (ClassNotFoundException e) {
            throw new NotFoundException(e.getMessage(), e);
        } catch (UnmodifiableClassException e) {
            throw new CannotCompileException(e.getMessage(), e);
        }
    }

    /**
     * Ensures that the agent is ready.
     * It attempts to dynamically start the agent if necessary.
     */
    public static void startAgent() throws NotFoundException {
        LOG.trace("JVMAgent startAgent");
        if (instrumentation != null)
            return;
        try {
//            LOG.trace("JVMAgent needs to find and connect to VM.");
//            File agentJar = createJarFile();
//            String nameOfRunningVM = ManagementFactory.getRuntimeMXBean().getName();
//            String pid = nameOfRunningVM.substring(0, nameOfRunningVM.indexOf('@'));
//            VirtualMachine vm = VirtualMachine.attach(pid);
//            vm.loadAgent(agentJar.getAbsolutePath(), null);
//            vm.detach();
        } catch (Exception e) {
            throw new NotFoundException("JVMAgent", e);
        }

        for (int sec = 0; sec < 10 /* sec */; sec++) {
            if (instrumentation != null)
                return;
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        throw new NotFoundException("JVMAgent agent (timeout)");
    }


    public static File createAgentJarFile(String fileName) throws IOException, CannotCompileException, NotFoundException {
        return createJarFile(new File(fileName));
    }

    private static File createJarFile() throws IOException, CannotCompileException, NotFoundException {
        File jar = File.createTempFile("jvmagent", ".jar");
        jar.deleteOnExit();
        return createJarFile(jar);
    }

    private static File createJarFile(File jar)
            throws IOException, CannotCompileException, NotFoundException {
        Manifest manifest = new Manifest();
        Attributes attrs = manifest.getMainAttributes();
        attrs.put(Attributes.Name.MANIFEST_VERSION, "1.0");
        attrs.put(new Attributes.Name("Premain-Class"), JVMAgent.class.getName());
        attrs.put(new Attributes.Name("Agent-Class"), JVMAgent.class.getName());
        attrs.put(new Attributes.Name("Can-Retransform-Classes"), "true");
        attrs.put(new Attributes.Name("Can-Redefine-Classes"), "true");

        JarOutputStream jos = null;
        try {
            jos = new JarOutputStream(new FileOutputStream(jar), manifest);
            String cname = JVMAgent.class.getName();
            JarEntry e = new JarEntry(cname.replace('.', '/') + ".class");
            jos.putNextEntry(e);
            ClassPool pool = ClassPool.getDefault();
            CtClass clazz = pool.get(cname);
            jos.write(clazz.toBytecode());
            jos.closeEntry();
        } finally {
            if (jos != null)
                jos.close();
        }

        return jar;
    }

}
