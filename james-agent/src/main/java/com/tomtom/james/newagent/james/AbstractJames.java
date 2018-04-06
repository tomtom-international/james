package com.tomtom.james.newagent.james;

import com.tomtom.james.common.api.informationpoint.InformationPoint;
import com.tomtom.james.common.log.Logger;
import com.tomtom.james.newagent.JVMAgent;
import com.tomtom.james.newagent.JamesObjective;
import javassist.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Queue;

public abstract class AbstractJames implements James {
    private long sleepTime = 1000;
    private Queue<JamesObjective> objectives;
    private static final Logger LOG = Logger.getLogger(AbstractJames.class);

    public AbstractJames(Queue<JamesObjective> objectives, long sleepTime) {
        this.objectives = objectives;
        this.sleepTime = sleepTime;
    }

    abstract void insertBefore(CtMethod method, InformationPoint informationPoint) throws CannotCompileException;

    abstract void insertAfter(CtMethod method, InformationPoint informationPoint) throws CannotCompileException;

    abstract void addCatch(ClassPool pool, CtMethod method, InformationPoint informationPoint) throws CannotCompileException, NotFoundException;

    private void work(JamesObjective objective) {
        Class clazz = objective.getClazz();
        ClassPool pool = ClassPool.getDefault();
        //pool.importPackage("com.tomtom.james.newagent");
        pool.insertClassPath(new LoaderClassPath(clazz.getClassLoader()));
        try {
            CtClass ctClass = pool.get(clazz.getName());
            ctClass.stopPruning(true);
            ctClass.defrost();
            List<CtMethod> ctMethodList = new ArrayList<>();
            for (InformationPoint informationPoint : objective.getInformationPoints()) {
                LOG.debug("instrumentation: " + clazz.getName() + "#" + informationPoint.getMethodName() + " [IP:" + informationPoint + "]");
                try {
                    CtMethod[] methods = ctClass.getDeclaredMethods(informationPoint.getMethodName()); // we need instrument aaa(String s) and aaa(Integer i) and aaa() ....
                    ctMethodList.addAll(Arrays.asList(methods));
                    for(CtMethod method : methods) {
                        if (method == null || method.isEmpty()) {
                            LOG.error(" ERROR !!!! Method is empty or null [" + clazz.getName() + "#" + informationPoint.getMethodName() + "]");
                        } else {
                            LOG.trace(" JAMES INSTRUMENTATION :: " + clazz.getName() + " : " + informationPoint);
                            // before method
                            insertBefore(method, informationPoint);
                            // after method
                            insertAfter(method, informationPoint);
                            // catch exceptions
                            addCatch(pool, method, informationPoint);
                        }
                    }
                } catch (NotFoundException notFound) {
                    LOG.error(" ERROR !!!! Method not found : [" + clazz.getName() + "#" + informationPoint.getMethodName() + "]");
                    continue; // method is inherited from parent class or it's abstract method
                }
            }
            JVMAgent.redefine(clazz, ctClass);
            ctClass.detach();
        } catch (NotFoundException | CannotCompileException | IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        while (true) {
            if (objectives.isEmpty()) {
                try {
                    Thread.sleep(sleepTime);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } else {
                JamesObjective objective = objectives.poll();
                if (objective != null) {
                    work(objective);
                }
            }
        }
    }
}
