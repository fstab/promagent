package io.promagent.agent.core.utils;



import io.promagent.agent.core.config.LogConfig;
import io.promagent.agent.core.config.TypeConstants;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;


public class TypeUtils {
    public static String getType(Method method) {

        String methodName = method.toString();
        if (LogConfig.typeCache.containsKey(methodName)) {
            return LogConfig.typeCache.get(methodName);
        }

        String type = getTypeFromReg(method.getDeclaringClass().getCanonicalName());
        if (StringUtils.isEmpty(type)) {
            type = getTypeFromAnn(method);
        }

        if (StringUtils.isEmpty(type)) {
            type = TypeConstants.SYSTEM;
        }
        LogConfig.typeCache.put(methodName, type);
        return type;
    }

    private static String getTypeFromAnn(Method method) {
        if (!CollectionUtils.isEmpty(LogConfig.annMethodType)) {
            for (Annotation annIter : method.getAnnotations()) {
                String annMethod = annIter.annotationType().getCanonicalName();
                if (LogConfig.annMethodType.containsKey(annMethod)) {
                    return LogConfig.annMethodType.get(annMethod);
                }
            }
        }
        if (!CollectionUtils.isEmpty(LogConfig.annClassType)) {
            for (Annotation annIter : method.getDeclaringClass().getAnnotations()) {
                String annClass = annIter.annotationType().getCanonicalName();
                if (LogConfig.annClassType.containsKey(annClass)) {
                    return LogConfig.annClassType.get(annClass);
                }
            }
        }
        return null;
    }

    private static String getTypeFromReg(String className) {
        for (String regClass : LogConfig.regType.keySet()) {
            if (className.matches(regClass)) {
                return LogConfig.regType.get(regClass);
            }
        }
        return null;
    }
}
