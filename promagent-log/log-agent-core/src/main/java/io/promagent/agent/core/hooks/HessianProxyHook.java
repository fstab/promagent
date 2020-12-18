package io.promagent.agent.core.hooks;

import com.caucho.hessian.client.HessianConnection;

import io.promagent.agent.core.Logger;
import io.promagent.agent.core.config.LogConfig;
import io.promagent.agent.core.internal.LogObjectProxy;
import io.promagent.agent.core.utils.MdcUtils;
import io.promagent.annotations.*;

import java.lang.reflect.Method;

@Hook(instruments = {
        "com.caucho.hessian.client.HessianProxy"
}, skipNestedCalls = false)
public class HessianProxyHook {

    @Before(method = {"addRequestHeaders"})
    public void before(HessianConnection conn) {
        try {
            conn.addHeader(LogConfig.TRACE_ID, MdcUtils.getLogId());
        } catch (Throwable e) {
            Logger.error(e);
        }
    }

    @Before(method = {"invoke"})
    public void before(Object proxy, Method method, Object[] args) {
        try {
            LogObjectProxy.getTempData().put(method.toString(), System.currentTimeMillis());
        } catch (Throwable e) {
            Logger.error(e);
        }
    }

    @After(method = {"invoke"})
    public void after(Object proxy, Method method, Object[] args, @Returned Object ret, @Thrown Throwable t) {
        try {
            long exc = System.currentTimeMillis() - LogObjectProxy.getTempData().getLongValue(method.toString());
            Logger.hessianInfo(exc, t, ret, method, args);
        } catch (Throwable e) {
            Logger.error(e);
        }
    }
}
