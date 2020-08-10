package io.promagent.log.core;

import com.alibaba.fastjson.JSONObject;
import io.promagent.log.config.LogConfig;
import io.promagent.log.config.LogConstants;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class LogObjectUtils {


    protected static String getSignature(Method method) {
        if (method == null) {
            return LogConstants.NULL;
        }
        return method.getDeclaringClass().getSimpleName() + "." + method.getName();
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
            if (HttpServletRequest.class.isAssignableFrom(argumentClazz)
                    || HttpServletResponse.class.isAssignableFrom(argumentClazz)) {
                continue;
            }
            result.add(JSONObject.toJSONString(args[i]));
        }
        return JSONObject.toJSONString(result);
    }

    protected static String getReturn(Object ret) {
        if (ret == null) {
            return LogConstants.NULL;
        }

        String retStr = JSONObject.toJSONString(ret);
        if (retStr.length() > LogConfig.MAX_MSG) {
            return "ReturnOver" + LogConfig.MAX_MSG;
        }

        return JSONObject.toJSONString(ret);
    }

    protected static String thrownToString(Throwable thrown) {
        if(thrown == null){
            return LogConstants.NULL;
        }

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw, true);
        thrown.printStackTrace(pw);
        return sw.getBuffer().toString();
    }

}
