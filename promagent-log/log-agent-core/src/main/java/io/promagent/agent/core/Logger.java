package io.promagent.agent.core;


import io.promagent.agent.core.config.GradeConstants;
import io.promagent.agent.core.config.TypeConstants;
import io.promagent.agent.core.internal.LogObjectProxy;
import io.promagent.agent.core.utils.MdcUtils;
import io.promagent.agent.core.utils.MethodUtils;
import io.promagent.agent.core.utils.TypeUtils;

import java.lang.reflect.Method;

/**
 * Description:
 *
 * @Author:zhangyi
 * @Date:2019/10/24
 */
public class Logger {
    public static void info(Long exec, Throwable error, Object ret, Method met, Object[] args) {
        try {
            if (MethodUtils.ignoreMethod(met)) {
                return;
            }
            String type = TypeUtils.getType(met);
            if (type.equals(TypeConstants.CRON)) {
                cronInfo(exec, error, ret, met, type, args);
            } else {
                LogObjectProxy.doLog(exec, GradeConstants.DEFAULT, error, ret, MethodUtils.getSignature(met), type, args);
            }
        } catch (Throwable e) {
            error(e);
        }
    }

    public static void cronInfo(Long exce, Throwable error, Object ret, Method met, String type, Object... args) {
        LogObjectProxy.doLog(exce, GradeConstants.DEFAULT, error, ret, MethodUtils.getSignature(met), type, args);
        //初始化下一次的logId
        LogObjectProxy.Clean();
        MdcUtils.setLogId(null);
    }

    public static void hessianInfo(Long exce, Throwable error, Object ret, Method met, Object... args) {
        LogObjectProxy.doLog(exce, GradeConstants.DEFAULT, error, ret, MethodUtils.getSignature(met), TypeConstants.HESSIAN, args);
    }

    public static void syncInfo(Throwable error, String logId, Object ret) {
        MdcUtils.setLogId(logId);
        LogObjectProxy.doLog(null, GradeConstants.DEFAULT, error, ret, null, TypeConstants.SYNC, null);
    }
    public static void error(Throwable frameError) {
        LogObjectProxy.error(frameError);
    }
}
