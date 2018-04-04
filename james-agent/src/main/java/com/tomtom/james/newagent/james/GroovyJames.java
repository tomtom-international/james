package com.tomtom.james.newagent.james;

import com.tomtom.james.common.api.informationpoint.InformationPoint;
import com.tomtom.james.common.log.Logger;
import com.tomtom.james.newagent.JVMAgent;
import com.tomtom.james.newagent.JamesObjective;
import javassist.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

public class GroovyJames extends AbstractJames {
    private static final Logger LOG = Logger.getLogger(GroovyJames.class);

    public GroovyJames(Queue<JamesObjective> objectives, long sleepTime) {
        super(objectives, sleepTime);
    }


    // FIXME !!!!!!!!!!  is it enough to escape this strings or .... are we creating a gap making whole system liable to groovy / java injection ?
    private String escapeScriptString(String script) {
        return script.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n");
    }


    private void insertBefore(CtMethod method, InformationPoint informationPoint) throws CannotCompileException {
        method.addLocalVariable("_startTime", CtClass.longType);
        StringBuilder s = new StringBuilder("");
        s.append(" System.out.println(\">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> INSTRUMENTATION BEFORE+ " + informationPoint.getClassName() + "#" + informationPoint.getMethodName() + ">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>\"); ");
        s.append(" com.tomtom.james.newagent.GlobalValueStore.put(\"" + informationPoint + "\", System.nanoTime()); ");
        if (Modifier.isStatic(method.getModifiers())) {
            s.append(" com.tomtom.james.informationpoint.advice.ContextAwareAdvice.onEnter(\""+method.getDeclaringClass().getName()+"\", \"" + informationPoint.getMethodName() + "\");");
        } else {
            s.append(" com.tomtom.james.informationpoint.advice.ContextAwareAdvice.onEnter($0.getClass().getName(), \"" + informationPoint.getMethodName() + "\");");
        }
        s.append(" ");
        method.insertBefore(s.toString());
    }

    private void insertAfter(CtMethod method, InformationPoint informationPoint) throws CannotCompileException {
        String script = escapeScriptString(informationPoint.getScript().get());
        StringBuilder s = new StringBuilder();
        s.append(" System.out.println(\"<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<< INSTRUMENTATION AFTER+ " + informationPoint.getClassName() + "#" + informationPoint.getMethodName() + "<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<\"); ");
        s.append(" long _methodStartTime = com.tomtom.james.newagent.GlobalValueStore.getAndRemove(\"" + informationPoint + "\"); \n");
        s.append(" com.tomtom.james.informationpoint.advice.ContextAwareAdvice.onExit( _methodStartTime, \n");
        s.append("\"" + informationPoint.getClassName() + "\", ");
        s.append("\"" + informationPoint.getMethodName() + "\", ");
        s.append("\"" + script + "\", ");
        s.append(informationPoint.getSampleRate() + ", "); // sample rate
        if (Modifier.isStatic(method.getModifiers())) {
            s.append(method.getDeclaringClass().getName() + ".class.getMethod(\"" + informationPoint.getMethodName() + "\",$sig), "); // method
            s.append("null, "); // this is static method - no instance
        } else{
            s.append("$0.getClass().getMethod(\"" + informationPoint.getMethodName() + "\",$sig), "); // method
            s.append("$0, "); // this
        }
        s.append("$args, ");  // arguments
        s.append("($r)$_, "); // result
        s.append("null ");    // exception
        s.append(" ); ");
        method.insertAfter(s.toString(), false);
    }

    private void addCatch(ClassPool pool, CtMethod method, InformationPoint informationPoint) throws CannotCompileException, NotFoundException {
        String script = escapeScriptString(informationPoint.getScript().get());
        StringBuilder s = new StringBuilder();
        s.append(" System.out.println(\"!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!! INSTRUMENTATION EXCEPTION+ " + informationPoint.getClassName() + "#" + informationPoint.getMethodName() + "!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!\"); ");
        s.append(" long _methodStartTime = com.tomtom.james.newagent.GlobalValueStore.getAndRemove(\"" + informationPoint + "\"); ");
        s.append(" com.tomtom.james.informationpoint.advice.ContextAwareAdvice.onExit( _methodStartTime, ");
        s.append("\"" + informationPoint.getClassName() + "\", ");
        s.append("\"" + informationPoint.getMethodName() + "\", ");
        s.append("\"" + script + "\", ");
        s.append(informationPoint.getSampleRate() + ", "); // sample rate
        if (Modifier.isStatic(method.getModifiers())) {
            s.append(method.getDeclaringClass().getName() + ".class.getMethod(\"" + informationPoint.getMethodName() + "\",$sig), "); // method
            s.append("null, "); // this is static method - no instance
        } else{
            s.append("$0.getClass().getMethod(\"" + informationPoint.getMethodName() + "\",$sig), "); // method
            s.append("$0, "); // this
        }
        s.append("$args, ");  // arguments
        s.append("null, "); // result
        s.append("$e");    // exception
        s.append(" ); ");
        s.append(" throw $e; ");
        CtClass exceptionCtClass = pool.getDefault().getCtClass("java.lang.Exception");
        method.addCatch(s.toString(), exceptionCtClass);
    }

    @Override
    public void work(JamesObjective objective) {
        LOG.error("[ADD] - instrumentation : " + objective.getClazz().getName() + " :: informationPoints [" + objective.getInformationPoints().size() + "]");
        Class clazz = objective.getClazz();
        ClassPool pool = ClassPool.getDefault();
        pool.insertClassPath(new LoaderClassPath(clazz.getClassLoader()));
        try {
            CtClass ctClass = pool.get(clazz.getName());
            ctClass.stopPruning(true);
            ctClass.defrost();
            List<CtMethod> ctMethodList = new ArrayList<>();
            for (InformationPoint informationPoint : objective.getInformationPoints()) {
                LOG.error("--------- instrumentation: " + clazz.getName() + "#" + informationPoint.getMethodName() + " [IP:" + informationPoint + "] ------------------- ");
                CtMethod method = null;
                //pool.importPackage("com.tomtom.james.newagent");
                try {
                    method = ctClass.getDeclaredMethod(informationPoint.getMethodName());
                    ctMethodList.add(method);
                } catch (NotFoundException notFound) {
                    LOG.error(" ERROR - Method not found : [" + clazz.getName() + "#" + informationPoint.getMethodName() + "]");
                    continue; // method is inherited from parent class or it's abstract method
                }
                if (method == null || method.isEmpty()) {
                    LOG.error(" ERROR !!!! Method is empty or null [" + clazz.getName() + "#" + informationPoint.getMethodName() + "]");
                }
                else {
                    // before method
                    insertBefore(method, informationPoint);
                    // after method
                    insertAfter(method, informationPoint);
                    // catch exceptions
                    addCatch(pool, method, informationPoint);
                }
            }
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
}
