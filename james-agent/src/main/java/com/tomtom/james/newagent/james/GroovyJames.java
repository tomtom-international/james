package com.tomtom.james.newagent.james;

import com.tomtom.james.common.api.informationpoint.ExtendedInformationPoint;
import com.tomtom.james.common.api.informationpoint.InformationPoint;
import com.tomtom.james.common.log.Logger;
import com.tomtom.james.newagent.JamesObjective;
import javassist.*;

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

    protected void insertBefore(CtMethod method, ExtendedInformationPoint informationPoint) throws CannotCompileException {
        method.addLocalVariable("_startTime", CtClass.longType);
        StringBuilder s = new StringBuilder("");
        s.append(" System.out.println(\">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> INSTRUMENTATION BEFORE+ " + informationPoint.getClassName() + "#" + informationPoint.getMethodName() + ">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>\"); ");
        s.append(" com.tomtom.james.newagent.GlobalValueStore.put(\"" + informationPoint + "\", System.nanoTime()); ");
        s.append(" ");
        method.insertBefore(s.toString());
    }

    protected void insertAfter(CtMethod method, ExtendedInformationPoint informationPoint) throws CannotCompileException {
        String script = escapeScriptString(informationPoint.getScript().get());
        StringBuilder s = new StringBuilder();
        s.append(" System.out.println(\"<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<< INSTRUMENTATION AFTER+ " + informationPoint.getClassName() + "#" + informationPoint.getMethodName() + "<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<\"); ");
        s.append(" long _methodStartTime = com.tomtom.james.newagent.GlobalValueStore.get(\"" + informationPoint + "\"); \n");
        s.append(" com.tomtom.james.informationpoint.advice.ContextAwareAdvice.onExit( _methodStartTime, \n");
        s.append("\"" + informationPoint.getClassName() + "\", ");
        s.append("\"" + informationPoint.getMethodName() + "\", ");
        s.append("\"" + script + "\", ");
        s.append(informationPoint.getSampleRate() + ", "); // sample rate
        s.append( informationPoint.getMethodBodyClassName() + ".class.getDeclaredMethod(\"" + informationPoint.getMethodName() + "\",$sig), "); // method

        if (Modifier.isStatic(method.getModifiers()) || method.isEmpty()) {
            s.append("null, "); // this is static method - no instance
        } else {
            s.append("$0, "); // this
        }

//        if (Modifier.isStatic(method.getModifiers()) || method.isEmpty()) {
//            s.append("$class.getDeclaredMethod(\"" + informationPoint.getMethodName() + "\",$sig), "); // method
//            s.append("null, "); // this is static method - no instance
//        } else {
//            s.append("$0.getClass().getDeclaredMethod(\"" + informationPoint.getMethodName() + "\",$sig), "); // method
//            s.append("$0, "); // this
//        }

        s.append("$args, ");  // arguments
        s.append("($r)$_, "); // result
        s.append("null ");    // exception
        s.append(" ); ");
        method.insertAfter(s.toString(), false);

    }

    protected void addCatch(ClassPool pool, CtMethod method, ExtendedInformationPoint informationPoint) throws CannotCompileException, NotFoundException {
        String script = escapeScriptString(informationPoint.getScript().get());
        StringBuilder s = new StringBuilder();
        s.append(" System.out.println(\"!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!! INSTRUMENTATION EXCEPTION+ " + informationPoint.getClassName() + "#" + informationPoint.getMethodName() + "!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!\"); ");
        s.append(" long _methodStartTime = com.tomtom.james.newagent.GlobalValueStore.get(\"" + informationPoint + "\"); ");
        s.append(" com.tomtom.james.informationpoint.advice.ContextAwareAdvice.onExit( _methodStartTime, ");
        s.append("\"" + informationPoint.getClassName() + "\", ");
        s.append("\"" + informationPoint.getMethodName() + "\", ");
        s.append("\"" + script + "\", ");
        s.append(informationPoint.getSampleRate() + ", "); // sample rate
        s.append( informationPoint.getMethodBodyClassName() + ".class.getDeclaredMethod(\"" + informationPoint.getMethodName() + "\",$sig), "); // method

        if (Modifier.isStatic(method.getModifiers()) || method.isEmpty()) {
            s.append("null, "); // this is static method - no instance
        } else {
            s.append("$0, "); // this
        }


//        if (Modifier.isStatic(method.getModifiers()) || method.isEmpty()) {
//            s.append("$class.getDeclaredMethod(\"" + informationPoint.getMethodName() + "\",$sig), "); // method
//            s.append("null, "); // this is static method - no instance
//        } else {
//            s.append("$0.getClass().getDeclaredMethod(\"" + informationPoint.getMethodName() + "\",$sig), "); // method
//            s.append("$0, "); // this
//        }
        s.append("$args, ");  // arguments
        s.append("null, "); // result
        s.append("$e");    // exception
        s.append(" ); ");
        s.append(" throw $e; ");
        CtClass exceptionCtClass = pool.getDefault().getCtClass("java.lang.Exception"); // FIXME static accessed from instance !!!!
        method.addCatch(s.toString(), exceptionCtClass);

        // finally block
        StringBuilder f = new StringBuilder("");
        f.append(" System.out.println(\"---------------------------------------------------------------------------- FINALLY ----------------------------------------------------------------------------\"); \n");
        f.append(" com.tomtom.james.newagent.GlobalValueStore.getAndRemove(\""+informationPoint+"\"); \n");
        method.insertAfter(f.toString(),true);
    }
}
