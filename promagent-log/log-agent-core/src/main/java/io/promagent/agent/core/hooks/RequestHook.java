package io.promagent.agent.core.hooks;


import io.promagent.agent.core.Logger;
import io.promagent.agent.core.config.GradeConstants;
import io.promagent.agent.core.config.LogConfig;
import io.promagent.agent.core.config.LogConstants;
import io.promagent.agent.core.config.TypeConstants;
import io.promagent.agent.core.internal.LogObjectProxy;
import io.promagent.agent.core.utils.*;
import io.promagent.annotations.After;
import io.promagent.annotations.Before;
import io.promagent.annotations.Hook;
import io.promagent.annotations.Thrown;

import javax.servlet.FilterChain;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

@Hook(instruments = {
        "javax.servlet.Filter",
        "javax.servlet.http.HttpServlet"
})
public class RequestHook {

    @Before(method = {"doFilter"})
    public void before(ServletRequest request, ServletResponse response, FilterChain chain) {
        doBefore(request, response);
    }

    @Before(method = {"service"})
    public void before(ServletRequest request, ServletResponse response) {
        doBefore(request, response);
    }

    @After(method = {"doFilter"})
    public void after(ServletRequest request, ServletResponse response, FilterChain chain, @Thrown Throwable t) {
        doAfter(request, response, t, LogConstants.FilterSign);
    }

    @After(method = {"service"})
    public void after(ServletRequest request, ServletResponse response, @Thrown Throwable t) {
        doAfter(request, response, t, LogConstants.HttpServletSign);
    }

    private void doBefore(ServletRequest request, ServletResponse response) {
        try {
            if (RequestUtils.isHttpServlet(request, response)
                    && MethodUtils.existMethod("getStatus", response.getClass(), null)
                    && !LogObjectProxy.getTempData().containsKey(LogConstants.RequestTimeStamp)) {

                LogObjectProxy.getTempData().put(LogConstants.RequestTimeStamp, System.currentTimeMillis());
                HttpServletRequest httpServletRequest = (HttpServletRequest) request;
                MdcUtils.setLogId(httpServletRequest.getHeader(LogConfig.TRACE_ID));

                Map<String, String> headers = RequestUtils.getHeaders(httpServletRequest);

                Map<String, Object> addRequestMap = new HashMap<>();
                if (!CollectionUtils.isEmpty(headers)) {
                    addRequestMap.put(LogConstants.reg_header, headers);
                }

                addRequestMap.put(LogConstants.reg_url, httpServletRequest.getRequestURI());
                LogObjectProxy.addRequest(addRequestMap);
            }
        } catch (Throwable e) {
            Logger.error(e);
        }
    }

    private void doAfter(ServletRequest request, ServletResponse response, Throwable t, String signature) {
        try {
            if (RequestUtils.isHttpServlet(request, response)
                    && MethodUtils.existMethod("getStatus", response.getClass(), null)
                    && LogObjectProxy.getTempData().containsKey(LogConstants.RequestTimeStamp)) {
                long exec = System.currentTimeMillis() - LogObjectProxy.getTempData().getLongValue(LogConstants.RequestTimeStamp);
                HttpServletResponse httpServletResponse = (HttpServletResponse) response;
                HttpServletRequest httpServletRequest = (HttpServletRequest) request;

                Map<String, String> params = RequestUtils.getParams(httpServletRequest);
                Map<String, Object> addRequestMap = new HashMap<>();

                if (!CollectionUtils.isEmpty(params)) {
                    addRequestMap.put(LogConstants.reg_params, params);
                }
                addRequestMap.put(LogConstants.reg_status, httpServletResponse.getStatus());
                LogObjectProxy.addRequest(addRequestMap);

                String grade = GradeConstants.DEFAULT;
                String ret = null;

                if (!StringUtils.isEmpty(t)) {
                    grade = GradeConstants.WARN;
                    ret = "RequestException";
                } else if (LogObjectProxy.getTempData().containsKey(LogConstants.HandlerInterceptorSign)) {
                    grade = GradeConstants.WARN;
                    ret = LogObjectProxy.getTempData().getString(LogConstants.HandlerInterceptorSign);
                }
                LogObjectProxy.doLog(exec, grade, null, ret, signature, TypeConstants.FILTER, null);

                LogObjectProxy.Clean();
            }
        } catch (Throwable e) {
            Logger.error(e);
        }
    }
}
