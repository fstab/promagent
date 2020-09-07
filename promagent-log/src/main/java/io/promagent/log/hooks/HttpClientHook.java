
package io.promagent.log.hooks;

import io.promagent.annotations.Before;
import io.promagent.annotations.Hook;
import io.promagent.log.Logger;
import io.promagent.log.config.LogConfig;
import io.promagent.log.utils.MdcUtils;
import org.apache.http.HttpHost;
import org.apache.http.HttpMessage;
import org.apache.http.HttpRequest;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.protocol.HttpContext;

/**
 * Description:
 *
 * @Author:zhangyi
 * @Date:2019/9/25
 */
@Hook(instruments = {
        "org.apache.http.client.HttpClient"
})
public class HttpClientHook {

    private void addHeader(HttpMessage httpMessage) {
        try {
            httpMessage.addHeader(LogConfig.TRACE_ID, MdcUtils.getLogId());
        } catch (Throwable e) {
            Logger.error(e);
        }
    }

    @Before(method = {"execute"})
    public void execute(HttpUriRequest httpUriRequest) {
        addHeader(httpUriRequest);
    }

    @Before(method = {"execute"})
    public void execute(HttpUriRequest httpUriRequest, HttpContext httpContext) {
        addHeader(httpUriRequest);
    }

    @Before(method = {"execute"})
    public void execute(HttpHost httpHost, HttpRequest httpRequest) {
        addHeader(httpRequest);
    }

    @Before(method = {"execute"})
    public void execute(HttpHost httpHost, HttpRequest httpRequest, HttpContext httpContext) {
        addHeader(httpRequest);
    }

    @Before(method = {"execute"})
    public void execute(HttpUriRequest httpUriRequest, ResponseHandler responseHandler) {
        addHeader(httpUriRequest);
    }

    @Before(method = {"execute"})
    public void execute(HttpUriRequest httpUriRequest, ResponseHandler responseHandler, HttpContext httpContext) {
        addHeader(httpUriRequest);
    }

    @Before(method = {"execute"})
    public void execute(HttpHost httpHost, HttpRequest httpRequest, ResponseHandler responseHandler) {
        addHeader(httpRequest);
    }

    @Before(method = {"execute"})
    public void execute(HttpHost httpHost, HttpRequest httpRequest, ResponseHandler responseHandler, HttpContext httpContext) {
        addHeader(httpRequest);
    }
}
