package io.promagent.internal;


import io.promagent.agent.ClassLoaderCache;
import net.bytebuddy.implementation.bytecode.assign.Assigner;

import java.lang.reflect.Method;

import static net.bytebuddy.asm.Advice.*;

/**
 * Description:
 *
 * @Author:zhangyi
 * @Date:2019/7/31
 */
public class CustomPromagentAdvice {

    @OnMethodEnter
    @SuppressWarnings("unchecked")
    public static Long before() {
        return System.currentTimeMillis();
    }

    @OnMethodExit(onThrowable = Throwable.class)
    public static void after(@Enter Long startTime,
                             @Origin Method method,
                             @AllArguments Object[] args,
                             @Return(typing = Assigner.Typing.DYNAMIC) Object returned,
                             @Thrown Throwable thrown) {
        Class<?> logUtilsClass = null;
        try {
            logUtilsClass = ClassLoaderCache.getInstance().currentClassLoader().loadClass(System.getProperty("agent.callClass"));
            Method logMethod = logUtilsClass.getMethod(System.getProperty("agent.callMethod"), Long.class, Throwable.class, Object.class, Method.class, Object[].class);
            Long exc = System.currentTimeMillis() - startTime;
            logMethod.invoke(null, exc, thrown, returned, method, args);
        } catch (Throwable frameError) {
            frameError.printStackTrace();
            try {
                if (logUtilsClass == null) {
                    logUtilsClass = ClassLoaderCache.getInstance().currentClassLoader().loadClass(System.getProperty("agent.callClass"));
                }
                if (logUtilsClass != null) {
                    Method frameErrorMethod = logUtilsClass.getMethod(System.getProperty("agent.callErrorMethod"), Throwable.class);
                    frameErrorMethod.invoke(null, frameError);
                }
            } catch (Throwable ignore) {
                ignore.printStackTrace();
            }
        }
    }
}
