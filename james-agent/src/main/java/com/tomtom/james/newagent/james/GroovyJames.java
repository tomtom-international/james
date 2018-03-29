package com.tomtom.james.newagent.james;

import com.tomtom.james.common.api.informationpoint.InformationPoint;
import com.tomtom.james.common.log.Logger;
import com.tomtom.james.newagent.GlobalValueStore;
import com.tomtom.james.newagent.JVMAgent;
import com.tomtom.james.newagent.JamesObjective;
import javassist.*;

import java.io.IOException;
import java.util.Queue;

public class GroovyJames extends AbstractJames {
    private static final Logger LOG = Logger.getLogger(GroovyJames.class);

    public GroovyJames(Queue<JamesObjective> objectives, int sleepTime) {
        super(objectives, sleepTime);
    }

    private void insertBefore(CtMethod method, InformationPoint informationPoint) throws CannotCompileException {
        method.addLocalVariable("_startTime", CtClass.longType);
        StringBuilder s = new StringBuilder("");
                    s.append(" com.tomtom.james.newagent.GlobalValueStore.put(\""+informationPoint+"\", System.nanoTime()); ");
                    s.append(" com.tomtom.james.informationpoint.advice.ContextAwareAdvice.onEnter($0.getClass().getName(), \"" + informationPoint.getMethodName() + "\");");
        s.append(" ");
        method.insertBefore(s.toString());
    }

    // FIXME !!!!!!!!!!  is it enough to escape this strings or .... are we creating a gap making whole system liable to groovy / java injection ?
    private String escapeScriptString(String script) {
        return script.replace("\\","\\\\")
                .replace("\"","\\\"")
                .replace("\r","\\r")
                .replace("\n","\\n");
    }

    private void insertAfter(Class clazz, CtMethod method, InformationPoint informationPoint) throws CannotCompileException {
        String script = escapeScriptString(informationPoint.getScript().get());
        StringBuilder s = new StringBuilder();
        s.append(" long _methodStartTime = com.tomtom.james.newagent.GlobalValueStore.getAndRemove(\""+informationPoint+"\"); \n");
        s.append(" com.tomtom.james.informationpoint.advice.ContextAwareAdvice.onExit( _methodStartTime, \n");
        s.append("\""+ informationPoint.getClassName() + "\", ");
        s.append("\""+ informationPoint.getMethodName() + "\", ");
        s.append("\""+ script +"\", ");
        s.append(informationPoint.getSampleRate() +", "); // sample rate
        s.append("$0.getClass().getMethod(\""+informationPoint.getMethodName()+"\",$sig), "); // method
        s.append("$0, "); // this
        s.append("$args, ");  // arguments
        s.append("($r)$_, "); // resulto
        s.append("null ");    // exception
        s.append(" ); ");
        method.insertAfter(s.toString(),false);
    }

    private void addCatch(ClassPool pool, Class clazz, CtMethod method, InformationPoint informationPoint) throws CannotCompileException, NotFoundException {
        String script = escapeScriptString(informationPoint.getScript().get());
        StringBuilder s = new StringBuilder();
        s.append(" long _methodStartTime = com.tomtom.james.newagent.GlobalValueStore.getAndRemove(\""+informationPoint+"\"); ");
        s.append(" com.tomtom.james.informationpoint.advice.ContextAwareAdvice.onExit( _methodStartTime, ");
        s.append("\""+ informationPoint.getClassName() + "\", ");
        s.append("\""+ informationPoint.getMethodName() + "\", ");
        s.append("\""+ script +"\", ");
        s.append(informationPoint.getSampleRate() +", "); // sample rate
        s.append("$0.getClass().getMethod(\""+informationPoint.getMethodName()+"\",$sig), "); // method
        s.append("$0, "); // this
        s.append("$args, ");  // arguments
        s.append("null, "); // result
        s.append("$e");    // exception
        s.append(" ); ");
        s.append(" throw $e; ");
        CtClass exceptionCtClass = pool.getDefault().getCtClass("java.lang.Exception");
        method.addCatch(s.toString(), exceptionCtClass);
    }

    private void add(Class clazz, InformationPoint informationPoint) {
        LOG.trace("GroovyJames instrumentation : " + informationPoint + " | " + clazz.getName() + "[" + clazz.hashCode() + "] | classloader:" + clazz.getClassLoader());
        ClassPool pool = ClassPool.getDefault();
        pool.insertClassPath(new LoaderClassPath(clazz.getClassLoader()));
        try {
            CtClass ctClass = pool.get(clazz.getName());
            CtMethod method = null;
            //pool.importPackage("com.tomtom.james.newagent");
            try {
                method = ctClass.getDeclaredMethod(informationPoint.getMethodName());
            } catch (NotFoundException notFound) {
                return; // class is abstact and doesn't contain methods body
            }
            if (method == null || method.isEmpty()) {
                return; // skip instrumentation for all not implemented methods (in abstract classes)
            }
            ctClass.stopPruning(true);
            ctClass.defrost();

            // before method
            insertBefore(method, informationPoint);
            // after method
            insertAfter(clazz, method, informationPoint);
            // catch exceptions
            addCatch(pool, clazz, method, informationPoint);

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
