package io.promagent.log.core;

import com.alibaba.fastjson.JSONObject;
import io.promagent.log.config.LogConfig;
import io.promagent.log.config.LogConstants;
import org.springframework.http.HttpEntity;
import org.springframework.util.CollectionUtils;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class LogObjectUtils {

    public static String getSignature(Method method) {
        return method == null ? LogConstants.NULL : method.getDeclaringClass().getSimpleName() + "." + method.getName();
    }

    protected static String getArgs(Object[] args) {
        if (args == null) {
            return LogConstants.NULL;
        }

        List<String> result = new ArrayList<>();
        for (int i = 0; i < args.length; i++) {
            if (args[i] == null) {
                continue;
            }
            Class argumentClazz = args[i].getClass();
            if (ServletRequest.class.isAssignableFrom(argumentClazz)) {
                continue;
            }
            if (ServletResponse.class.isAssignableFrom(argumentClazz)) {
                continue;
            }
            result.add(JSONObject.toJSONString(args[i]));
        }
        if (CollectionUtils.isEmpty(result)) {
            return LogConstants.NULL;
        }
        return JSONObject.toJSONString(result);
    }

    protected static String getReturn(Object ret) {
        if (ret == null) {
            return LogConstants.NULL;
        }
        if (HttpEntity.class.isAssignableFrom(ret.getClass())){
            return LogConstants.SKIP;
        }

        String retStr = JSONObject.toJSONString(ret);
        if (retStr.length() > LogConfig.MAX_MSG) {
            return "ReturnOver" + LogConfig.MAX_MSG;
        }
        return JSONObject.toJSONString(ret);
    }

    protected static String thrownToString(Throwable thrown) {
        return thrown == null ? LogConstants.NULL : getStackTrace(thrown);
    }

    //    org.apache.commons.lang3.exception.ExceptionUtils.getStackTrace() copy
    private static String getStackTrace(Throwable throwable) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw, true);
        throwable.printStackTrace(pw);
        return sw.getBuffer().toString();
    }
}
