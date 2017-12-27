// Copyright 2017 The Promagent Authors
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package io.promagent.hooks;

import io.promagent.annotations.After;
import io.promagent.annotations.Before;
import io.promagent.annotations.Hook;
import io.promagent.hookcontext.HookContext;
import io.promagent.hookcontext.Key;
import io.promagent.hookcontext.MetricDef;
import io.promagent.hookcontext.MetricsStore;
import io.prometheus.client.Counter;
import io.prometheus.client.Summary;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.concurrent.TimeUnit;

@Hook(instruments = {
        "javax.servlet.Servlet",
        "javax.servlet.Filter"}
//        skipNestedCalls = true
)
public class ServletHook {

    static final Key<String> SERVLET_HOOK_METHOD = new Key<>("servlet.hook.method");
    static final Key<String> SERVLET_HOOK_PATH = new Key<>("servlet.hook.path");
    private final HookContext context;
    private final Counter httpRequestsTotal;
    private final Summary httpRequestsDuration;
    private long startTime = 0;
    private int stackDepth = 0;

    public ServletHook(HookContext context) {

        this.context = context;

        MetricsStore metricsStore = context.getMetricsStore();

        httpRequestsTotal = metricsStore.createOrGet(new MetricDef<>(
                "http_requests_total",
                registry -> Counter.build()
                        .name("http_requests_total")
                        .labelNames("method", "path", "status")
                        .help("Total number of http requests.")
                        .register(registry)
        ));

        httpRequestsDuration = metricsStore.createOrGet(new MetricDef<>(
                "http_request_duration",
                registry -> Summary.build()
                        .quantile(0.5, 0.05)   // Add 50th percentile (= median) with 5% tolerated error
                        .quantile(0.9, 0.01)   // Add 90th percentile with 1% tolerated error
                        .quantile(0.99, 0.001) // Add 99th percentile with 0.1% tolerated error
                        .name("http_request_duration")
                        .labelNames("method", "path", "status")
                        .help("Duration for serving the http requests in seconds.")
                        .register(registry)
        ));
    }

    private String stripPathParameters(String path) {

        // The URL path may include path parameters.
        // For example, REST URLs for querying an item might look like this:
        //
        //   /item/1
        //   /item/2
        //   /item/3
        //   etc.
        //
        // We don't want to create a new Prometheus label for each of these paths.
        // Rather, we want a single label like this:
        //
        //   /item/{id}
        //
        // This method replaces path parameters with placeholders. It is application specific and
        // should be adapted depending on the actual paths in an application.
        // For the demo, we just replace all numbers with {id}.

        return path
                .replaceAll("/[0-9]+", "/{id}")
                .replaceAll("/;jsessionid=\\w*", "")
                .replaceAll("/$", "")
                .replaceAll("\\?.*", ""); // Also remove path parameters, like "?jsessionid=..."
    }

    @Before(method = {"service", "doFilter"})
    public void before(ServletRequest request, ServletResponse response) {
        stackDepth++;
        System.err.println("servlet before call number " + stackDepth + " on object " + this);
        if (stackDepth > 1) {
            return; // recursive call
        }
        if (HttpServletRequest.class.isAssignableFrom(request.getClass()) && HttpServletResponse.class.isAssignableFrom(response.getClass())) {
            HttpServletRequest req = (HttpServletRequest) request;
            context.getThreadLocal().put(SERVLET_HOOK_METHOD, req.getMethod());
            context.getThreadLocal().put(SERVLET_HOOK_PATH, stripPathParameters(req.getRequestURI()));
            startTime = System.nanoTime();
        }
    }

    // Return Werte und Exceptions als Parameter
    @After(method = {"service", "doFilter"})
    public void after(ServletRequest request, ServletResponse response/*, @Returned int i, @Thrown Throwable t*/) throws Exception {
        System.err.println("servlet after call number " + stackDepth + " on object " + this);
        stackDepth--;
        if (stackDepth > 0) {
            return; // recursive call
        }
        if (HttpServletRequest.class.isAssignableFrom(request.getClass()) && HttpServletResponse.class.isAssignableFrom(response.getClass())) {
            HttpServletResponse resp = (HttpServletResponse) response;
            try {
                double duration = ((double) System.nanoTime() - startTime) / (double) TimeUnit.SECONDS.toNanos(1L);
                String method = context.getThreadLocal().get(SERVLET_HOOK_METHOD).get();
                String path = context.getThreadLocal().get(SERVLET_HOOK_PATH).get();
                httpRequestsTotal.labels(method, path, Integer.toString(resp.getStatus())).inc();
                httpRequestsDuration.labels(method, path, Integer.toString(resp.getStatus())).observe(duration);
            } finally {
                context.getThreadLocal().clear(SERVLET_HOOK_METHOD, SERVLET_HOOK_PATH);
            }
        }
    }
}
