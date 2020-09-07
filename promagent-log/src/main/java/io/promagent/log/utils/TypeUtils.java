package io.promagent.log.utils;

import io.promagent.log.config.LogConfig;
import io.promagent.log.config.TypeConstants;
import org.springframework.util.CollectionUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

public class TypeUtils {
    public static String getType(Method injectionMethod) {
        String methodName = injectionMethod.toString();
        if (LogConfig.type.containsKey(methodName)) {
            return LogConfig.type.get(methodName);
        }

        String type = getTypeFromReg(injectionMethod.getDeclaringClass().getCanonicalName());
        if (type.equals(TypeConstants.SYSTEM)) {
            type = getTypeFromAnn(injectionMethod);
        }
        LogConfig.type.put(methodName, type);
        return type;
    }

    private static String getTypeFromAnn(Method methodInjection) {
        String result = TypeConstants.SYSTEM;
        if (!CollectionUtils.isEmpty(LogConfig.annMethodType)) {
            for (Annotation annIter : methodInjection.getAnnotations()) {
                String annMethod = annIter.annotationType().getCanonicalName();
                if (LogConfig.annMethodType.containsKey(annMethod)) {
                    return LogConfig.annMethodType.get(annMethod);
                }
            }
        }
        if (!CollectionUtils.isEmpty(LogConfig.annMethodType)) {
            for (Annotation annIter : methodInjection.getDeclaringClass().getAnnotations()) {
                String annClass = annIter.annotationType().getCanonicalName();
                if (LogConfig.annClassType.containsKey(annClass)) {
                    return LogConfig.annClassType.get(annClass);
                }
            }
        }
        return result;
    }

    private static String getTypeFromReg(String injectionClassName) {
        for (String regClass : LogConfig.regType.keySet()) {
            if (injectionClassName.matches(regClass)) {
                return LogConfig.regType.get(regClass);
            }
        }
        return TypeConstants.SYSTEM;
    }
}
