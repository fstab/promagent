package io.promagent.log.hooks;


import io.promagent.annotations.After;
import io.promagent.annotations.Before;
import io.promagent.annotations.Hook;
import io.promagent.annotations.Thrown;
import io.promagent.log.Logger;
import io.promagent.log.config.LogConstants;
import io.promagent.log.utils.MdcUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.RequestAttributes;

import java.util.concurrent.Callable;

/**
 * Description:
 *
 * @Author:zhangyi
 * @Date:2019/10/10
 */
@Hook(instruments = {
        "org.springframework.web.context.request.async.CallableProcessingInterceptor",
})
public class CallableProcessingInterceptorHook {

    @Before(method = {"beforeConcurrentHandling"})
    public void before(NativeWebRequest request, Callable<Object> task) {
        try {
            String accessId = (String) request.getAttribute(LogConstants.mdc_logId, RequestAttributes.SCOPE_REQUEST);
            if (StringUtils.isEmpty(accessId)) {
                request.setAttribute(LogConstants.mdc_logId, MdcUtils.getLogId(), RequestAttributes.SCOPE_REQUEST);
            }
        } catch (Throwable e) {
            Logger.error(e);
        }
    }

    @After(method = {"postProcess"})
    public void after(NativeWebRequest request, Callable<Object> task, Object concurrentResult, @Thrown Throwable throwable) {
        try {
            String accessId = (String) request.getAttribute(LogConstants.mdc_logId, RequestAttributes.SCOPE_REQUEST);
            if (!StringUtils.isEmpty(accessId)) {
                Logger.syncInfo(throwable, accessId, concurrentResult);
                request.removeAttribute(LogConstants.mdc_logId, RequestAttributes.SCOPE_REQUEST);
            }
        } catch (Throwable e) {
            Logger.error(e);
        }
    }
}
