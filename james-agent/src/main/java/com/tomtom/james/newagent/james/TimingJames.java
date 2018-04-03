package com.tomtom.james.newagent.james;

import com.tomtom.james.common.api.informationpoint.InformationPoint;
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

    private void add(Class clazz, String methodName) {
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
        } catch (NotFoundException | IOException | CannotCompileException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void work(JamesObjective objective) {
        LOG.trace("TimingJames : " + objective);

        Class clazz = objective.getClazz();
        ClassPool pool = ClassPool.getDefault();
        pool.insertClassPath(new LoaderClassPath(clazz.getClassLoader()));
        try {
            CtClass ctClass = pool.get(clazz.getName());
            ctClass.stopPruning(true);
            ctClass.defrost();
            for (InformationPoint ip : objective.getInformationPoints()) {
                CtMethod method = ctClass.getDeclaredMethod(ip.getMethodName());
                method.addLocalVariable("_startTime", CtClass.longType);
                method.insertBefore("_startTime = System.nanoTime();");
                method.insertAfter("System.out.println(\"#### INJECTED :: Execution Duration " + "(nano sec): \"+ (System.nanoTime() - _startTime) );");
                LOG.debug("TimingJames instruments method : " + clazz.getName() + "#" + ip.getMethodName() + " :: DONE !");
            }
            JVMAgent.redefine(clazz, ctClass);
            ctClass.detach();
        } catch (NotFoundException | IOException | CannotCompileException e) {
            e.printStackTrace();
        }


        LOG.debug("TimingJames : " + objective + " :: DONE !");
    }

}
