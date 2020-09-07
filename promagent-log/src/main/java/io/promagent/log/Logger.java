package io.promagent.log;


import io.promagent.log.config.LogConfig;
import io.promagent.log.config.LogConstants;
import io.promagent.log.config.TypeConstants;
import io.promagent.log.core.LogObjectProxy;
import io.promagent.log.core.LogObjectUtils;
import io.promagent.log.utils.MdcUtils;
import io.promagent.log.utils.TypeUtils;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.util.Map;

/**
 * Description:
 *
 * @Author:zhangyi
 * @Date:2019/10/24
 */
public class Logger {
    public static void info(Long exce, Throwable thrown, Object ret, Method met, Object[] args) {
        try {
            String sig = LogObjectUtils.getSignature(met);
            if (LogConstants.NULL.equals(sig) || LogConfig.ignoreSignatures.contains(sig)) {
                return;
            }
            String preSignature = (String) LogObjectProxy.getTempData(LogConstants.preSignature);
            if (!StringUtils.isEmpty(preSignature) && preSignature.equals(sig)) {
                return;
            } else {
                LogObjectProxy.setTempData(LogConstants.preSignature, sig);
            }
            String type = TypeUtils.getType(met);
            if (type.equals(TypeConstants.CRON)) {
                cronInfo(exce, thrown, ret, met, args, type);
            } else {
                LogObjectProxy.doLog(exce, thrown, ret, met, args, type);
            }
        } catch (Throwable e) {
            error(e);
        }
    }

    public static void cronInfo(Long exce, Throwable thrown, Object ret, Method sig, Object[] args, String type) {
        LogObjectProxy.doLog(exce, thrown, ret, sig, args, type);
        //初始化下一次的logId
        httpServletAfter();
        MdcUtils.setLogId(null);
    }

    public static void hessianInfo(Long exce, Throwable thrown, Object ret, Method met, Object[] args) {
        LogObjectProxy.doLog(exce, thrown, ret, met, args, TypeConstants.HESSIAN);
    }

    public static void syncInfo(Throwable thrown, String logId, Object ret) {
        MdcUtils.setLogId(logId);
        LogObjectProxy.doLog(-1L, thrown, ret, null, null, TypeConstants.SYNC);
    }

    public static void httpServletBefore(String logId, Map<String, String> header, String uri, Map<String, String> params) {
        MdcUtils.setLogId(logId);
        LogObjectProxy.setRequest(header, uri, params);
    }

    public static void httpServletAfter() {
        MdcUtils.mdcClear();
        LogObjectProxy.clean();
    }

    public static void error(Throwable frameError) {
        try {
            frameError.printStackTrace();
            LogObjectProxy.error(frameError);
        } catch (Throwable ignore) {
            ignore.printStackTrace();
        }
    }
}
