package com.tomtom.james.newagent;

import com.google.common.io.Resources;
import com.tomtom.james.common.log.Logger;
import com.tomtom.james.configuration.AgentConfiguration;
import com.tomtom.james.configuration.ConfigurationInitializationException;

import java.io.File;
import java.io.IOException;
import java.lang.instrument.ClassDefinition;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.lang.management.ManagementFactory;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class JVMAgent {
    private static final Logger LOG = Logger.getLogger(JVMAgent.class);
    private static Instrumentation instrumentation = null;
    private static Thread classScannerThread;

    public static Instrumentation getInstrumentation() {
        try {
            return (Instrumentation) ClassLoader.getSystemClassLoader()
                    .loadClass(JVMAgent.class.getName())
                    .getDeclaredField("instrumentation")
                    .get(null);
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
        setupAgent(instrumentation);
    }

    private static void setupAgent(Instrumentation instrumentation) {
        LOG.trace("JVMAgent agentmain");
        try {
            if (!inst.isRedefineClassesSupported())
                throw new RuntimeException("this JVM does not support redefinition of classes");
            instrumentation = inst;
            //configuration = AgentConfigurationFactory.create();
            //Logger.setCurrentLogLevel(configuration.getLogLevel());
            //printBanner(configuration);



            // execute period class scanner
            NewClassQueue deltaBuffer = new BasicNewClassQueue();
            classScannerThread = new Thread(new JamesClassScanner(deltaBuffer));
            classScannerThread.run();
            LOG.trace("JVMAgent - class scanner is running.");


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
            LOG.trace("JVMAgent needs to find and connect to VM.");
            File agentJar = createJarFile();
            String nameOfRunningVM = ManagementFactory.getRuntimeMXBean().getName();
            String pid = nameOfRunningVM.substring(0, nameOfRunningVM.indexOf('@'));
            VirtualMachine vm = VirtualMachine.attach(pid);
            vm.loadAgent(agentJar.getAbsolutePath(), null);
            vm.detach();
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

}
