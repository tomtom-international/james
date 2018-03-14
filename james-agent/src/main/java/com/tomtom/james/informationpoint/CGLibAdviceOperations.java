package com.tomtom.james.informationpoint;

import com.google.common.base.Stopwatch;
import com.tomtom.james.common.api.informationpoint.InformationPoint;
import com.tomtom.james.common.log.Logger;
import com.tomtom.james.informationpoint.advice.ContextAwareAdvice;
import com.tomtom.james.informationpoint.annotations.InformationPointClassName;
import com.tomtom.james.informationpoint.annotations.InformationPointMethodName;
import com.tomtom.james.informationpoint.annotations.InformationPointSampleRate;
import com.tomtom.james.informationpoint.annotations.InformationPointScript;
import net.bytebuddy.agent.builder.ResettableClassFileTransformer;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.asm.AsmVisitorWrapper;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

import java.lang.reflect.Method;

import static net.bytebuddy.matcher.ElementMatchers.hasSuperType;
import static net.bytebuddy.matcher.ElementMatchers.named;

public class CGLibAdviceOperations implements AdviceOperations {

    private static final Logger LOG = Logger.getLogger(CGLibAdviceOperations.class);

    @Override
    public void installAdvice(InformationPoint informationPoint) {
        installedInformationPoints.computeIfAbsent(new ByteBuddyAdviceOperations.Key(informationPoint), key -> {
            Stopwatch stopwatch = Stopwatch.createStarted();

            Class superClass = null;
            try {
                superClass = Class.forName(informationPoint.getClassName());
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
                return;
            }

            Enhancer enhancer = new Enhancer();
            enhancer.setSuperclass(superClass);
            enhancer.setCallback(new MethodInterceptor() {
                @Override
                public Object intercept(Object obj, Method method, Object[] args, MethodProxy proxy) throws Throwable {
                    System.out.println("dupa dupa dupa" + args);
                    return proxy.invokeSuper(obj, args);
                }
            });



            ResettableClassFileTransformer transformer =
                    agentBuilder
                    .type(named(key.getClassName()).or(hasSuperType(named(key.getClassName()))))
                    .transform((builder, typeDescription, classLoader, module) -> {
                        AsmVisitorWrapper visitorWrapper = Advice
                                .withCustomMapping()
                                .bind(InformationPointClassName.class, informationPoint.getClassName())
                                .bind(InformationPointMethodName.class, informationPoint.getMethodName())
                                .bind(InformationPointSampleRate.class, informationPoint.getSampleRate())
                                .bind(InformationPointScript.class, informationPoint.getScript().orElseThrow(() ->
                                        new IllegalStateException("Script is not defined for " + informationPoint)))
                                .to(ContextAwareAdvice.class)
                                .on(named(key.getMethodName()));
                        return builder.visit(visitorWrapper);
                    })
                    .installOn(instrumentation);
            stopwatch.stop();
            LOG.trace(() -> "Advice installed at " + key + " in " + stopwatch.elapsed());
            return transformer;
        });
    }

    @Override
    public void uninstallAdvice(InformationPoint informationPoint) {
        LOG.debug("uninstalling :: CGlib");
    }

}
