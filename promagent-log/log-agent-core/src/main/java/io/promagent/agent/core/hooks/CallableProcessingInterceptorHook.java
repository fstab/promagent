package io.promagent.agent.core.hooks;


import io.promagent.agent.core.Logger;
import io.promagent.agent.core.config.LogConstants;
import io.promagent.agent.core.utils.MdcUtils;
import io.promagent.agent.core.utils.StringUtils;
import io.promagent.annotations.After;
import io.promagent.annotations.Before;
import io.promagent.annotations.Hook;
import io.promagent.annotations.Thrown;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.RequestAttributes;

import java.util.concurrent.Callable;

@Hook(instruments = {
        "org.springframework.web.context.request.async.CallableProcessingInterceptor",
})
public class CallableProcessingInterceptorHook {

    @Before(method = {"beforeConcurrentHandling"})
    public void before(NativeWebRequest request, Callable<Object> task) {
        try {
            String logId = (String) request.getAttribute(LogConstants.mdc_logId, RequestAttributes.SCOPE_REQUEST);
            if (StringUtils.isEmpty(logId)) {
                request.setAttribute(LogConstants.mdc_logId, MdcUtils.getLogId(), RequestAttributes.SCOPE_REQUEST);
            }
        } catch (Throwable e) {
            Logger.error(e);
        }
    }

    @After(method = {"postProcess"})
    public void after(NativeWebRequest request, Callable<Object> task, Object concurrentResult, @Thrown Throwable throwable) {
        try {
            String logId = (String) request.getAttribute(LogConstants.mdc_logId, RequestAttributes.SCOPE_REQUEST);
            if (!StringUtils.isEmpty(logId)) {
                Logger.syncInfo(throwable, logId, concurrentResult);
                request.removeAttribute(LogConstants.mdc_logId, RequestAttributes.SCOPE_REQUEST);
            }
        } catch (Throwable e) {
            Logger.error(e);
        }
    }
}
