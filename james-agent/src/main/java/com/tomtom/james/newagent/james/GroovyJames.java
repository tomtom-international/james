package com.tomtom.james.newagent.james;

import com.tomtom.james.common.api.informationpoint.InformationPoint;
import com.tomtom.james.common.log.Logger;
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

    private void add(Class clazz, InformationPoint informationPoint) {
        LOG.trace("GroovyJames instrumentation : " + informationPoint + " | " + clazz.getName() + "["+clazz.hashCode()+"] | classloader:" + clazz.getClassLoader());
        ClassPool pool = ClassPool.getDefault();
        pool.insertClassPath(new LoaderClassPath(clazz.getClassLoader()));
        try {
            CtClass ctClass = pool.get(clazz.getName());
            pool.importPackage("com.tomtom.james.common.log");
            CtMethod method = ctClass.getDeclaredMethod(informationPoint.getMethodName());
            ctClass.stopPruning(true);
            ctClass.defrost();
            CtClass LOGCtClass = pool.get(com.tomtom.james.common.log.Logger.class.getName());
            method.addLocalVariable("_LOG", LOGCtClass);
            method.addLocalVariable("_startTime", CtClass.longType);
            method.insertBefore(
                    " _startTime = System.nanoTime(); \n" +
                    " _LOG = com.tomtom.james.common.log.Logger.getLogger(com.tomtom.james.informationpoint.advice.ContextAwareAdvice.class); \n" +
                    " if (_LOG.isTraceEnabled()) {\n" +
                    "   _LOG.trace(\"onEnter: START [originTypeName= " + informationPoint.getClassName() + ", originMethodName= " + informationPoint.getMethodName() + "]\"); \n" +
                    " }\n");







                //method.insertAfter(" _LOG.trace(\" >>>>>>>>>>>>>>>>>>>>>>dupa !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!\"); \n" );

            method.insertAfter(" " +
//                    "try {" +
                      " {       _LOG.trace(\" timing = \" + (System.nanoTime() - _startTime) + \"ns \"); \n }"
//                    "} catch (Throwable _e) { \n" +
//                    "   _LOG.error(_e);" +
//                    "}"
                    );




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
