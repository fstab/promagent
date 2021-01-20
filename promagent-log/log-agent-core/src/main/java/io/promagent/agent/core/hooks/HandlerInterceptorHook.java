package io.promagent.agent.core.hooks;


import io.promagent.agent.core.Logger;
import io.promagent.agent.core.config.LogConstants;
import io.promagent.agent.core.internal.LogObjectProxy;
import io.promagent.agent.core.utils.StringUtils;
import io.promagent.annotations.After;
import io.promagent.annotations.Hook;
import io.promagent.annotations.Returned;
import io.promagent.annotations.Thrown;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Hook(instruments = {"org.springframework.web.servlet.HandlerInterceptor"})
public class HandlerInterceptorHook {

    @After(method = {"preHandle"})
    public void after(HttpServletRequest request, HttpServletResponse response, Object handler, @Returned Object ret, @Thrown Throwable t) {
        try {
            if (!StringUtils.isEmpty(t)) {
                LogObjectProxy.getTempData().put(LogConstants.HandlerInterceptorSign, "HandlerInterceptorException");
            } else if (!Boolean.valueOf((Boolean) ret)) {
                LogObjectProxy.getTempData().put(LogConstants.HandlerInterceptorSign, "HandlerInterceptorFalse");
            }
        } catch (Throwable e) {
            Logger.error(e);
        }
    }
}
