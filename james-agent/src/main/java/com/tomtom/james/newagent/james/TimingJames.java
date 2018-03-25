package com.tomtom.james.newagent.james;

import com.tomtom.james.common.log.Logger;
import com.tomtom.james.newagent.JVMAgent;
import com.tomtom.james.newagent.JamesObjective;
import javassist.*;

import java.io.IOException;
import java.util.Queue;

public class TimingJames extends AbstractJames {
    private static final Logger LOG = Logger.getLogger(TimingJames.class);

    public TimingJames(Queue<JamesObjective> objectives, int sleepTime) {
        super(objectives, sleepTime);
    }

    public void add(Class clazz, String methodName) {
        ClassPool pool = ClassPool.getDefault();
        pool.insertClassPath(new LoaderClassPath(clazz.getClassLoader()));
        try {
            CtClass ctClass = pool.get(clazz.getName());
            CtMethod method = ctClass.getDeclaredMethod(methodName);
            ctClass.stopPruning(true);
            ctClass.defrost();
            method.addLocalVariable("_startTime", CtClass.longType);
            method.insertBefore("_startTime = System.nanoTime();");
            method.insertAfter("System.out.println(\"#### INJECTED :: Execution Duration " + "(nano sec): \"+ (System.nanoTime() - _startTime) );");
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

    public void remove(Class clazz, String methodName) {
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
        LOG.trace("TimingJames : " + objective);
        if (objective.getType() == JamesObjective.ObjectiveType.ADD) {
            add(objective.getClazz(), objective.getInformationPoint().getMethodName());
        } else {
            remove(objective.getClazz(), objective.getInformationPoint().getMethodName());
        }
        LOG.debug("TimingJames : " + objective + " :: " + objective.getType() + " :: DONE !");
    }

}
