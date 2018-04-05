package com.tomtom.james.newagent.james;

import com.tomtom.james.common.api.informationpoint.InformationPoint;
import com.tomtom.james.common.log.Logger;
import com.tomtom.james.newagent.JVMAgent;
import com.tomtom.james.newagent.JamesObjective;
import javassist.*;

import java.io.IOException;
import java.util.Queue;

public class TimingJames extends AbstractJames {


    public TimingJames(Queue<JamesObjective> objectives, long sleepTime) {
        super(objectives, sleepTime);
    }

    @Override
    void insertBefore(CtMethod method, InformationPoint informationPoint) throws CannotCompileException {
        StringBuilder s = new StringBuilder("");
        s.append(" com.tomtom.james.newagent.GlobalValueStore.put(\"" + informationPoint + "\", System.nanoTime()); ");
        method.insertBefore(s.toString());
    }

    @Override
    void insertAfter(CtMethod method, InformationPoint informationPoint) throws CannotCompileException {
        StringBuilder s = new StringBuilder("");
        s.append(" long _methodStartTime = com.tomtom.james.newagent.GlobalValueStore.getAndRemove(\"" + informationPoint + "\"); \n");
        s.append(" System.out.println(\" TIMING JAMES : " + informationPoint +" | executionTime= \" + (System.nanoTime() - _methodStartTime) + \" ns \"); ");
        method.insertAfter(s.toString(),true);
    }

    @Override
    void addCatch(ClassPool pool, CtMethod method, InformationPoint informationPoint) throws CannotCompileException, NotFoundException {

    }
}
