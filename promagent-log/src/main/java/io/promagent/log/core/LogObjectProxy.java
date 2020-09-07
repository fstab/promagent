package io.promagent.log.core;


import io.promagent.log.config.TypeConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.Map;

/**
 * Description:
 *
 * @Author:zhangyi
 * @Date:2019/7/19
 */
public class LogObjectProxy {
    private static final Logger logger = LoggerFactory.getLogger(LogObjectProxy.class);

    public static Object getTempData(String tempKey) {
        return HttpContext.get().getTempData(tempKey);
    }

    public static void setTempData(String key, Object tempData) {
        HttpContext.get().setTempData(key, tempData);
    }

    public static void clean() {
        HttpContext.clean();
    }


    public static void setRequest(Map<String, String> header, String uri, Map<String, String> params) {
        LogObject logObject = HttpContext.get();
        logObject.setRequest(header, uri, params);
    }

    public static void doLog(Long exce, Throwable thrown, Object ret, Method sig, Object[] args, String type) {
        LogObject logObject = HttpContext.get();
        logObject.setMethod(exce, thrown, ret, sig, args, type);
        String msg = logObject.getLogJson();
        if (thrown != null) {
            logger.error(msg);
        } else {
            logger.info(msg);
        }
    }

    public static void error(Throwable error) {
        LogObject logObject = HttpContext.get();
        logObject.setType(TypeConstants.FRAME);
        logObject.setMsg(error);
        logger.error(logObject.getLogJson());
    }
}