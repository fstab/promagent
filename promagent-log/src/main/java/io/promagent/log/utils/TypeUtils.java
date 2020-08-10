package io.promagent.log.utils;



import io.promagent.log.config.LogConfig;
import io.promagent.log.config.TypeConstants;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

public class TypeUtils {
    public static String getType(Method injectionMethod) {
        String methodName = injectionMethod.toString();
        if (LogConfig.type.containsKey(methodName)) {
            return LogConfig.type.get(methodName);
        }

        String returnType = getTypeFromReg(injectionMethod.getDeclaringClass().getCanonicalName());
        if (returnType.equals(TypeConstants.SYSTEM)) {
            returnType = getTypeFromAnnotations(injectionMethod);
        }
        LogConfig.type.put(methodName, returnType);
        return returnType;
    }

    private static String getTypeFromAnnotations(Method methodInjection) {
        String result = TypeConstants.SYSTEM;

        for (Annotation annotationIter : methodInjection.getAnnotations()) {
            String annotationMethod = annotationIter.annotationType().getCanonicalName();
            if (LogConfig.annMethodType.containsKey(annotationMethod)) {
                return LogConfig.annMethodType.get(annotationMethod);
            }
        }
        for (Annotation annotationIter : methodInjection.getDeclaringClass().getAnnotations()) {
            String annotationClass = annotationIter.annotationType().getCanonicalName();
            if (LogConfig.annClassType.containsKey(annotationClass)) {
                return LogConfig.annClassType.get(annotationClass);
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
