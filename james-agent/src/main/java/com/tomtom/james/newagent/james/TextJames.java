package com.tomtom.james.newagent.james;

import com.tomtom.james.common.api.informationpoint.InformationPoint;
import com.tomtom.james.newagent.JamesObjective;
import javassist.*;

import java.util.Queue;

public class TextJames extends AbstractFullJames {

    public TextJames(Queue<JamesObjective> objectives, long sleepTime) {
        super(objectives, sleepTime);
    }

    @Override
    void insertBefore(CtMethod method, InformationPoint informationPoint) {
        try {
            StringBuilder s = new StringBuilder("");
            s.append(" System.out.println(\">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> INSTRUMENTATION BEFORE + " + informationPoint.getClassName() + "#" + informationPoint.getMethodName() + ">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>\"); ");
            method.insertBefore(s.toString());
        } catch (CannotCompileException e) {
            e.printStackTrace();
        }
    }

    @Override
    void insertAfter(CtMethod method, InformationPoint informationPoint) {
        try {
            StringBuilder s = new StringBuilder("");
            s.append(" System.out.println(\">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> INSTRUMENTATION AFTER + " + informationPoint.getClassName() + "#" + informationPoint.getMethodName() + ">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>\"); ");
            method.insertAfter(s.toString());
        } catch (CannotCompileException e) {
            e.printStackTrace();
        }
    }

    @Override
    void addCatch(ClassPool pool, CtMethod method, InformationPoint informationPoint) throws CannotCompileException, NotFoundException {
        StringBuilder s = new StringBuilder("");
        s.append(" System.out.println(\"!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!! INSTRUMENTATION EXCEPTION+ " + informationPoint.getClassName() + "#" + informationPoint.getMethodName() + "!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!\"); ");
        s.append(" throw $e; ");
        CtClass exceptionCtClass = pool.getDefault().getCtClass("java.lang.Exception");
        method.addCatch(s.toString(), exceptionCtClass);
    }

}
