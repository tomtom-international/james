package com.tomtom.james.newagent.james;

import com.tomtom.james.common.api.informationpoint.InformationPoint;
import com.tomtom.james.newagent.JVMAgent;
import com.tomtom.james.newagent.JamesObjective;
import javassist.*;

import java.io.IOException;
import java.util.Queue;

public class GroovyJames extends AbstractJames {

    public GroovyJames(Queue<JamesObjective> objectives, int sleepTime) {
        super(objectives, sleepTime);
    }

    private void beforeMethod(CtMethod method, InformationPoint informationPoint) throws CannotCompileException {
        method.insertBefore(" _startTime = System.nanoTime(); System.out.println(\"------------------------------------------FUCK !!!! \"); ");

    }

    private void afterMethod(CtMethod method, InformationPoint informationPoint) throws CannotCompileException {
        StringBuilder code = new StringBuilder("{ ");
        code.append(" _endTime = System.nanoTime(); ");
        code.append(" System.out.println(\"------------------------------------------FUCK AFTER !!!! \"); ");
        if (informationPoint.getSampleRate() < 100) {
            code.append(" java.util.Random RND = new Random();");
            code.append(" if ((" + informationPoint.getSampleRate() + " == 100)||(" + informationPoint.getSampleRate() + " < RND.nextDouble() * 100)) {");
            code.append(" com.tomtom.james.informationpoint.advice.ScriptEngineSupplier.get().invokeSuccessHandler(\n" +
                    "                        " + informationPoint.getClassName() + ",\n" +
                    "                        " + informationPoint.getMethodName() + ",\n" +
                    "                        " + informationPoint.getScript() + ",\n" +
                    "                        null,\n" + // origin FIXME WTF is origin - method ????
                    "                        createParameterList(origin, arguments),\n" + //FIXME need some util ?
                    "                        this,\n" + // instance
                    "                        Thread.currentThread(),\n" + // currentThread
                    "                        (_endTime - _startTime),\n" + // total time //FIXME in nanoseconds ?
                    "                        getCallStack(),\n" + // FIXME should be thre a util or is it problem to achieve that ?
                    "                        $_ \n" + // returned
                    "                );");
            code.append(" }");

        }
        code.append(" } ");
        method.insertAfter(code.toString());
    }

    private void exceptionCatchMethod(CtMethod method, InformationPoint informationPoint) throws CannotCompileException, NotFoundException {
        StringBuilder code = new StringBuilder("{ ");
        code.append(" _endTime = System.nanoTime(); ");
        code.append(" com.tomtom.james.informationpoint.advice.ScriptEngineSupplier.get().invokeSuccessHandler(\n" +
                "                        " + informationPoint.getClassName() + ",\n" +
                "                        " + informationPoint.getMethodName() + ",\n" +
                "                        " + informationPoint.getScript() + ",\n" +
                "                        null,\n" + // origin FIXME WTF is origin - method ????
                "                        createParameterList(origin, arguments),\n" + //FIXME need some util ?
                "                        this,\n" + // instance
                "                        Thread.currentThread(),\n" + // currentThread
                "                        (_endTime - _startTime),\n" + // total time //FIXME in nanoseconds ?
                "                        getCallStack(),\n" + // FIXME should be thre a util or is it problem to achieve that ?
                "                        $e \n" + // throwed exception
                "                );");
        code.append(" }");
        method.addCatch(code.toString(), ClassPool.getDefault().get("java.lang.Throwable"));
    }

    private void add(InformationPoint informationPoint) {
        ClassPool pool = ClassPool.getDefault();
        pool.insertClassPath(new LoaderClassPath(informationPoint.getClass().getClassLoader()));
        try {
            CtClass ctClass = pool.get(informationPoint.getClass().getName());
            CtMethod method = ctClass.getDeclaredMethod(informationPoint.getMethodName());
            ctClass.stopPruning(true);
            ctClass.defrost();
            beforeMethod(method, informationPoint);
            exceptionCatchMethod(method, informationPoint);
            afterMethod(method, informationPoint);
            JVMAgent.redefine(informationPoint.getClass(), ctClass);
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
            add(objective.getInformationPoint() );
        } else {
            remove(objective.getClazz(), objective.getInformationPoint().getMethodName());
        }
    }
}
