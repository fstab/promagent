package io.promagent.agent.test.config;

import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class ApiSignInterceptor implements HandlerInterceptor {
    private final static String NO_PERMISSION_ERROR_MESSAGE = "Api Token Error, You have no permission to access this api";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        try {
            String sign = request.getHeader("sign");

            if (sign.isEmpty()) {
                response.sendError(403, NO_PERMISSION_ERROR_MESSAGE);
                return false;
            }
        } catch (Throwable t) {
            response.sendError(403, NO_PERMISSION_ERROR_MESSAGE);
            return false;
        }
        return true;
    }
}