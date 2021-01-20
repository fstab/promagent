package io.promagent.agent.core.hooks;


import io.promagent.agent.core.config.GradeConstants;
import io.promagent.agent.core.config.LogConstants;
import io.promagent.agent.core.config.TypeConstants;
import io.promagent.agent.core.internal.LogObjectProxy;
import io.promagent.agent.core.utils.ThrowableUtils;
import io.promagent.annotations.Before;
import io.promagent.annotations.Hook;

@Hook(instruments = {"org.slf4j.Logger"})
public class LoggerHook {

    @Before(method = {"error"})
    public void error(String var1) {
        log(TypeConstants.USER, GradeConstants.WARN, null, LogConstants.LoggerErrSign, null, var1);
    }

    @Before(method = {"error"})
    public void error(String var1, Object var2) {
        log(TypeConstants.USER, GradeConstants.WARN, null, LogConstants.LoggerErrSign, null, var1, var2);
    }

    @Before(method = {"error"})
    public void error(String var1, Object var2, Object var3) {
        log(TypeConstants.USER, GradeConstants.WARN, null, LogConstants.LoggerErrSign, null, var1, var2, var3);
    }

    @Before(method = {"error"})
    public void error(String var1, Object... var2) {
        log(TypeConstants.USER, GradeConstants.WARN, null, LogConstants.LoggerErrSign, null, var1, var2);
    }

    @Before(method = {"error"})
    public void error(String var1, Throwable var2) {
        log(TypeConstants.USER, GradeConstants.WARN, null, LogConstants.LoggerErrSign, null, var1, ThrowableUtils.getStackTrace(var2));
    }

    @Before(method = {"info"})
    public void info(String var1, Throwable var2) {
        log(TypeConstants.USER, GradeConstants.WARN, null, LogConstants.LoggerInfoSign, null, var1, ThrowableUtils.getStackTrace(var2));
    }

    private void log(String type, String grade, Object ret, String sign, Throwable t, Object... args) {
        try {
            LogObjectProxy.doLog(null, grade, t, ret, sign, type, args);
        } catch (Throwable e) {
            LogObjectProxy.error(e);
        }
    }
}
