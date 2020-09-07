
package io.promagent.log.hooks;

import io.promagent.annotations.After;
import io.promagent.annotations.Before;
import io.promagent.annotations.Hook;
import io.promagent.log.Logger;
import io.promagent.log.config.LogConfig;
import io.promagent.log.config.LogConstants;
import org.springframework.util.StringUtils;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Description:
 *
 * @Author:zhangyi
 * @Date:2019/9/25
 */
@Hook(instruments = {
        "javax.servlet.http.HttpServlet"
})
public class HttpServletHook {
    private static Map<String, String> getHeaders(HttpServletRequest requestHttp) {
        Map<String, String> result = new HashMap<>();
        List<String> headerConfig = LogConfig.headers;

        if (headerConfig.size() == 0 || headerConfig.contains("none")) {
            return result;
        }

        if (headerConfig.contains("all")) {
            Enumeration headerNames = requestHttp.getHeaderNames();
            while (headerNames.hasMoreElements()) {
                String key = (String) headerNames.nextElement();
                String value = requestHttp.getHeader(key);
                result.put(key, value);
            }
            return result;
        }

        for (String key : headerConfig) {
            String value = requestHttp.getHeader(key);
            value = StringUtils.isEmpty(value) ? LogConstants.NULL : value;
            result.put(key, value);
        }

        return result;
    }

    private Map<String, String> getParams(HttpServletRequest requestHttp) {
        Map parmasMap = new HashMap();
        Enumeration parameterNames = requestHttp.getParameterNames();
        while (parameterNames.hasMoreElements()) {
            String paramName = (String) parameterNames.nextElement();
            String paramValue = requestHttp.getParameter(paramName);
            parmasMap.put(paramName, paramValue);
        }
        return parmasMap;
    }

    @Before(method = {"service"})
    public void before(ServletRequest request, ServletResponse response) {
        try {
            if (HttpServletRequest.class.isAssignableFrom(request.getClass())
                    && HttpServletResponse.class.isAssignableFrom(response.getClass())) {

                HttpServletRequest requestHttp = (HttpServletRequest) request;

                String logId = requestHttp.getHeader(LogConfig.TRACE_ID);
                Map<String, String> header = getHeaders(requestHttp);
                String uri = requestHttp.getRequestURI();

                Logger.httpServletBefore(logId, header, uri, getParams(requestHttp));
            }
        } catch (Throwable e) {
            Logger.error(e);
        }
    }

    @After(method = {"service"})
    public void after(ServletRequest request, ServletResponse response) {
        try {
            if (HttpServletRequest.class.isAssignableFrom(request.getClass())
                    && HttpServletResponse.class.isAssignableFrom(response.getClass())) {

                Logger.httpServletAfter();
            }
        } catch (Throwable e) {
            Logger.error(e);
        }
    }
}
