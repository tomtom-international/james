package com.tomtom.james.newagent.james;

import com.tomtom.james.common.api.informationpoint.InformationPoint;
import com.tomtom.james.common.log.Logger;
import com.tomtom.james.newagent.JVMAgent;
import com.tomtom.james.newagent.JamesObjective;
import javassist.*;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Queue;

public class GroovyJames extends AbstractJames {
    private static final Logger LOG = Logger.getLogger(GroovyJames.class);

    public GroovyJames(Queue<JamesObjective> objectives, int sleepTime) {
        super(objectives, sleepTime);
    }

    private void insertBefore(CtMethod method, InformationPoint informationPoint) throws CannotCompileException {
        method.addLocalVariable("_startTime", CtClass.longType);
        StringBuilder s = new StringBuilder("");
                    s.append(" _startTime = System.nanoTime(); \n");
                    s.append(" com.tomtom.james.informationpoint.advice.ContextAwareAdvice.onEnter($0.getClass().getName(), \"" + informationPoint.getMethodName() + "\");");
        s.append(" ");
        method.insertBefore(s.toString());
    }

    private void insertAfter(Class clazz, CtMethod method, InformationPoint informationPoint) throws CannotCompileException, NoSuchMethodException {

        String script = informationPoint.getScript().get()
                    .replace("\\","\\\\")
                    .replace("\"","\\\"")
                    .replace("\r","\\r")
                    .replace("\n","\\n");
        System.out.println("##########################################################");
        System.out.println(script);
        System.out.println("##########################################################");

        StringBuilder s = new StringBuilder();
        s.append(" com.tomtom.james.informationpoint.advice.ContextAwareAdvice.onExit( _startTime, ");
        s.append("\""+ informationPoint.getClassName() + "\", ");
        s.append("\""+ informationPoint.getMethodName() + "\", ");
        s.append("\""+ script +"\", ");
        s.append(informationPoint.getSampleRate() +", "); // sample rate
        s.append("$0.getClass().getMethod(\""+informationPoint.getMethodName()+"\",$sig), "); // method
        s.append("$0, "); // this
        s.append("$args, ");  // arguments
        s.append("($r)$_, "); // result
        s.append("null ");    // exception
        s.append(" ); ");

        System.out.println("+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
        System.out.println(s.toString());
        System.out.println("+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
        method.insertAfter(s.toString());
    }

    private void addCatch(ClassPool pool, Class clazz, CtMethod method, InformationPoint informationPoint) throws CannotCompileException, NotFoundException {
        CtClass throwableClass = pool.getCtClass("java.lang.Throwable");

        Method origin = null;
        try {
            origin = clazz.getDeclaredMethod(informationPoint.getMethodName());
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }

        StringBuilder s = new StringBuilder();
        method.addCatch(s.toString(), throwableClass);
    }

    private void add(Class clazz, InformationPoint informationPoint) {
        LOG.trace("GroovyJames instrumentation : " + informationPoint + " | " + clazz.getName() + "[" + clazz.hashCode() + "] | classloader:" + clazz.getClassLoader());
        ClassPool pool = ClassPool.getDefault();
        pool.insertClassPath(new LoaderClassPath(clazz.getClassLoader()));
        try {
            CtClass ctClass = pool.get(clazz.getName());
            CtMethod method = ctClass.getDeclaredMethod(informationPoint.getMethodName());
            ctClass.stopPruning(true);
            ctClass.defrost();

            // before method
            insertBefore(method, informationPoint);
            // after method
            insertAfter(clazz, method, informationPoint);
            // catch exceptions
            //addCatch(pool, clazz, method, informationPoint);

            JVMAgent.redefine(clazz, ctClass);
            ctClass.detach();
        } catch (NotFoundException e) {
            e.printStackTrace();
        } catch (CannotCompileException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

    private void remove(Class clazz, String methodName) {
        ClassPool pool = ClassPool.getDefault();
        pool.insertClassPath(new LoaderClassPath(clazz.getClassLoader()));
        try {
            CtClass ctClass = pool.get(clazz.getName());
            CtMethod method = ctClass.getDeclaredMethod(methodName);
            ctClass.stopPruning(true);
            ctClass.defrost();
            JVMAgent.redefine(clazz, ctClass);
            ctClass.detach();
        } catch (NotFoundException e) {
            e.printStackTrace();
        } catch (CannotCompileException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void work(JamesObjective objective) {
        if (objective.getType() == JamesObjective.ObjectiveType.ADD) {
            add(objective.getClazz(), objective.getInformationPoint());
        } else {
            remove(objective.getClazz(), objective.getInformationPoint().getMethodName());
        }
    }
}
