package io.promagent.agent.core.config;

public interface GradeConstants {
    /**
     * 报警规划	        type	grade
     *
     * 框架异常	        frame	error
     * 用户未捕获异常	    自定义	exception
     * 用户捕获异常	    自定义	warn
     * 其他日志	        自定义	default
     */
    String ERROR = "agentError";
    String EXCEPTION = "requestException";
    String WARN = "requestWarn";
    String DEFAULT = "requestDefault";
}
