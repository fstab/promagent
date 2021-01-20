package io.promagent.agent.core.utils;


import io.promagent.agent.core.config.LogConfig;
import io.promagent.agent.core.config.LogConstants;
import io.promagent.agent.core.internal.LogObjectProxy;

import java.lang.reflect.Method;

public class MethodUtils {
    public static boolean ignoreMethod(Method method) {
        if (StringUtils.isEmpty(method)) {
            return true;
        }
        String sig = getSignature(method);
        if (LogConfig.ignoreSignatures.contains(sig)) {
            return true;
        }
        String preSig = LogObjectProxy.getTempData().getString(LogConstants.PreviousSignature);
        if (!StringUtils.isEmpty(preSig) && preSig.equals(sig)) {
            return true;
        } else {
            LogObjectProxy.getTempData().put(LogConstants.PreviousSignature, sig);
            return false;
        }
    }

    public static String getSignature(Method method) {
        return method == null ? LogConstants.null_string : method.getDeclaringClass().getSimpleName() + "." + method.getName();
    }
    public static boolean existMethod(String name, Class obj, Class<?>... parameterTypes) {
        try {
            obj.getMethod(name, parameterTypes);
            return true;
        } catch (Exception ignore1) {
            try {
                obj.getDeclaredMethod(name, parameterTypes);
                return true;
            } catch (Exception ignore2) {
                return false;
            }
        }
    }
}
