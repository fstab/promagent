package io.promagent.agent.core.utils;


import io.promagent.agent.core.config.LogConfig;
import io.promagent.agent.core.config.LogConstants;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

public class RequestUtils {
    public static Map<String, String> getHeaders(HttpServletRequest requestHttp) {

        if (CollectionUtils.isEmpty(LogConfig.headers) || LogConfig.headers.contains("none")) {
            return null;
        }

        Map<String, String> result = new HashMap<>();
        if (LogConfig.headers.contains("all")) {
            Enumeration headerNames = requestHttp.getHeaderNames();
            while (headerNames.hasMoreElements()) {
                String key = (String) headerNames.nextElement();
                String value = requestHttp.getHeader(key);
                result.put(key, value);
            }
        } else {
            for (String key : LogConfig.headers) {
                String value = requestHttp.getHeader(key);
                if (StringUtils.isEmpty(value)) {
                    result.put(key, LogConstants.null_string);
                } else {
                    result.put(key, value);
                }
            }
        }
        if (CollectionUtils.isEmpty(result)) {
            return null;
        }
        return result;
    }

    public static Map<String, String> getParams(HttpServletRequest request) {
        Map<String, String> map = new HashMap();
        Enumeration parameterNames = request.getParameterNames();
        while (parameterNames.hasMoreElements()) {
            String key = (String) parameterNames.nextElement();
            map.put(key, request.getParameter(key));
        }
        if (CollectionUtils.isEmpty(map)) {
            return null;
        }
        return map;
    }

    public static boolean isHttpServlet(ServletRequest request, ServletResponse response) {
        return HttpServletRequest.class.isAssignableFrom(request.getClass())
                && HttpServletResponse.class.isAssignableFrom(response.getClass());
    }
}
