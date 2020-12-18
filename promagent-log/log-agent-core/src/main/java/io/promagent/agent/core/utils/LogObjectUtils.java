package io.promagent.agent.core.utils;

import com.alibaba.fastjson.JSONObject;
import io.promagent.agent.core.config.LogConfig;
import io.promagent.agent.core.config.LogConstants;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.util.ArrayList;
import java.util.List;

public class LogObjectUtils {
    public static String getArgs(Object[] args) {
        if (args == null) {
            return LogConstants.null_string;
        }

        List<Object> result = new ArrayList<>();
        for (int i = 0; i < args.length; i++) {
            if (StringUtils.isEmpty(args[i])) {
                continue;
            }
            Class argumentClazz = args[i].getClass();
            if (ServletRequest.class.isAssignableFrom(argumentClazz)) {
                continue;
            }
            if (ServletResponse.class.isAssignableFrom(argumentClazz)) {
                continue;
            }
            result.add(args[i]);
        }
        if (CollectionUtils.isEmpty(result)) {
            return LogConstants.null_string;
        }
        return JSONObject.toJSONString(result);
    }

    public static String getReturn(Object ret) {
        if (ret == null) {
            return LogConstants.null_string;
        }
        String retStr = JSONObject.toJSONString(ret);
        if (retStr.length() > LogConfig.RET_MAX_LENGTH) {
            return "ReturnOver" + LogConfig.RET_MAX_LENGTH;
        }
        return JSONObject.toJSONString(ret);
    }

}
