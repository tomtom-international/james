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

    @Override
    public void work(JamesObjective objective) {
        LOG.trace("TimingJames : " + objective);
        ClassPool pool = ClassPool.getDefault();
        pool.insertClassPath(new LoaderClassPath(objective.getClazz().getClassLoader()));
        try {
            CtClass ctClass = pool.get(objective.getClazz().getName());
            CtMethod method = ctClass.getDeclaredMethod(objective.getInformationPoint().getMethodName());
            ctClass.stopPruning(true);
            ctClass.defrost();
            method.addLocalVariable("_startTime", CtClass.longType);
            method.insertBefore("_startTime = System.nanoTime();");
            method.insertAfter("System.out.println(\"#### INJECTED :: Execution Duration "+ "(nano sec): \"+ (System.nanoTime() - _startTime) );");
            JVMAgent.redefine(objective.getClazz(), ctClass);
            ctClass.detach();
            LOG.debug("TimingJames : " + objective + " :: DONE !");
        } catch (NotFoundException e) {
            e.printStackTrace();
        } catch (CannotCompileException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}
