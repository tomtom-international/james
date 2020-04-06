package com.tomtom.james.newagent.james;

import com.tomtom.james.common.api.informationpoint.ExtendedInformationPoint;
import com.tomtom.james.common.log.Logger;
import com.tomtom.james.newagent.JamesObjective;
import javassist.*;

import java.util.Queue;

// TODO remove sysouts from strings
public class GroovyJames extends AbstractJames {
    private static final Logger LOG = Logger.getLogger(GroovyJames.class);

    public GroovyJames(Queue<JamesObjective> objectives, long sleepTime) {
        super(objectives, sleepTime);
        this.setDaemon(true);
        this.setName(getClass().getSimpleName());
    }

    // TODO check double if that is all chars that we need to escape
    private String escapeScriptString(String script) {
        return script.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n");
    }

    protected void insertBefore(CtMethod method, ExtendedInformationPoint informationPoint) throws CannotCompileException {
        StringBuilder s = new StringBuilder("");
        s.append(" com.tomtom.james.newagent.MethodExecutionTimeHelper.executionStarted();\n");
        s.append(" com.tomtom.james.informationpoint.advice.ContextAwareAdvice.onEnter(");
        s.append("\"" + informationPoint.getClassName() + "\", ");
        s.append("\"" + informationPoint.getMethodName() + "\", ");
        s.append(informationPoint.getMethodBodyClassName() + ".class.getDeclaredMethod(\"" + informationPoint.getMethodName() + "\",$sig), "); // method
        if (Modifier.isStatic(method.getModifiers()) || method.isEmpty()) {
            s.append("null"); // this is static method - no instance
        } else {
            s.append("$0"); // this
        }
        s.append(", $args);\n");
        method.insertBefore(s.toString());
    }

    protected void insertAfter(CtMethod method, ExtendedInformationPoint informationPoint) throws CannotCompileException {
        String script = escapeScriptString(informationPoint.getScript().get());
        StringBuilder s = new StringBuilder();
        s.append(" com.tomtom.james.informationpoint.advice.ContextAwareAdvice.onExit( com.tomtom.james.newagent.MethodExecutionTimeHelper.getStartTime(), \n");
        s.append("\"" + informationPoint.getClassName() + "\", ");
        s.append("\"" + informationPoint.getMethodName() + "\", ");
        s.append(informationPoint.getMethodBodyClassName() + ".class.getDeclaredMethod(\"" + informationPoint.getMethodName() + "\",$sig), "); // method

        if (Modifier.isStatic(method.getModifiers()) || method.isEmpty()) {
            s.append("null, "); // this is static method - no instance
        } else {
            s.append("$0, "); // this
        }

        s.append("$args, ");  // arguments
        s.append("($w)$_, "); // result
        s.append("null ");    // exception
        s.append(" ); ");
        method.insertAfter(s.toString(), false);

    }

    protected void addCatch(ClassPool pool, CtMethod method, ExtendedInformationPoint informationPoint) throws CannotCompileException, NotFoundException {
        String script = escapeScriptString(informationPoint.getScript().get());
        StringBuilder s = new StringBuilder();
        s.append(" com.tomtom.james.informationpoint.advice.ContextAwareAdvice.onExit( com.tomtom.james.newagent.MethodExecutionTimeHelper.getStartTime(), ");
        s.append("\"" + informationPoint.getClassName() + "\", ");
        s.append("\"" + informationPoint.getMethodName() + "\", ");
        s.append(informationPoint.getMethodBodyClassName() + ".class.getDeclaredMethod(\"" + informationPoint.getMethodName() + "\",$sig), "); // method

        if (Modifier.isStatic(method.getModifiers()) || method.isEmpty()) {
            s.append("null, "); // this is static method - no instance
        } else {
            s.append("$0, "); // this
        }

        s.append("$args, ");  // arguments
        s.append("null, "); // result
        s.append("$e");    // exception
        s.append(" ); ");
        s.append(" throw $e; ");
        CtClass exceptionCtClass = ClassPool.getDefault().getCtClass("java.lang.Exception"); // FIXME should here be Exception or Throwable ?????
        method.addCatch(s.toString(), exceptionCtClass);

        // finally block
        StringBuilder f = new StringBuilder("");
        f.append(" com.tomtom.james.newagent.MethodExecutionTimeHelper.executionFinished(); \n");
        method.insertAfter(f.toString(), true);
    }
}
