package io.promagent.log.hooks;

import com.caucho.hessian.client.HessianConnection;

import io.promagent.annotations.*;
import io.promagent.log.Logger;
import io.promagent.log.config.LogConfig;
import io.promagent.log.core.LogObjectProxy;
import io.promagent.log.utils.MdcUtils;

import java.lang.reflect.Method;

@Hook(instruments = {
        "com.caucho.hessian.client.HessianProxy"
}, skipNestedCalls = false)
public class HessianProxyHook {

    @Before(method = {"addRequestHeaders"})
    public void before(HessianConnection conn) {
        try {
            conn.addHeader(LogConfig.REQUEST_ID, MdcUtils.getLogId());
        } catch (Throwable e) {
            Logger.error(e);
        }
    }

    @Before(method = {"invoke"})
    public void before(Object proxy, Method method, Object[] args) {
        try {
            LogObjectProxy.setTempData(method.toString(), System.currentTimeMillis());
        } catch (Throwable e) {
            Logger.error(e);
        }
    }

    @After(method = {"invoke"})
    public void after(Object proxy, Method method, Object[] args, @Returned Object ret, @Thrown Throwable t) {
        try {
            long excBefore = (long) LogObjectProxy.getTempData(method.toString());
            long exc = System.currentTimeMillis() - excBefore;
            Logger.hessianInfo(exc, t, ret, method, args);
        } catch (Throwable e) {
            Logger.error(e);
        }
    }
}
