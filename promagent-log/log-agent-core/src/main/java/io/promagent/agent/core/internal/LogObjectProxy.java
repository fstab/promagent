package io.promagent.agent.core.internal;


import com.alibaba.fastjson.JSONObject;

import io.promagent.agent.core.config.GradeConstants;
import io.promagent.agent.core.config.TypeConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Description:
 *
 * @Author:zhangyi
 * @Date:2019/7/19
 */
public class LogObjectProxy {
    private static final Logger logger = LoggerFactory.getLogger(LogObjectProxy.class);

    public static JSONObject getTempData() {
        return HttpContext.get().getTempData();
    }

    public static void Clean() {
        HttpContext.clean();
    }

    public static void addRequest(Map<String, Object> map) {
        HttpContext.get().getRequest().putAll(map);
    }

    /**
     * 系统打印日志，当thrown 不为空时候，用户未捕获异常 grade=EXCEPTION
     *
     * @param exec
     * @param error
     * @param ret
     * @param sig
     * @param args
     * @param type
     */
    public static void doLog(Long exec, String grade, Throwable error, Object ret, String sign, String type, Object... args) {
        String msg = HttpContext.get()
                .setMethod(exec, error, ret, sign, args, type, grade)
                .getLogJson();
        logger.info(msg);
    }

    /**
     * type is FRAME, grade is error(框架打印，报警为最高级别)
     *
     * @param error
     */
    public static void error(Throwable error) {
        error.printStackTrace();
        String msg = HttpContext.get()
                .setMethod(null, error, null, null, null, TypeConstants.FRAME, GradeConstants.ERROR)
                .getLogJson();
        logger.info(msg);
    }

    private static class HttpContext {
        private static final ThreadLocal<LogObject> threadLocal = new InheritableThreadLocal<LogObject>() {
            @Override
            protected LogObject initialValue() {
                return new LogObject();
            }
        };

        private static LogObject get() {
            return threadLocal.get();
        }

        private static void clean() {
            threadLocal.remove();
        }
    }
}