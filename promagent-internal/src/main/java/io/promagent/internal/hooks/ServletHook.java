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

package io.promagent.internal.hooks;

import io.promagent.agent.annotations.After;
import io.promagent.agent.annotations.Before;
import io.promagent.agent.annotations.Hook;
import io.promagent.internal.metrics.MetricsUtil;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.Counter;
import io.prometheus.client.Summary;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.concurrent.TimeUnit;

import static io.promagent.internal.hooks.Context.SERVLET_HOOK_METHOD;
import static io.promagent.internal.hooks.Context.SERVLET_HOOK_PATH;

@Hook(instruments = {javax.servlet.Servlet.class, javax.servlet.Filter.class})
public class ServletHook {

    private static final String HTTP_REQUESTS_TOATAL = "http_requests_total";
    private static final String HTTP_REQUEST_DURATION = "http_request_duration";
    private final Context context;
    private long startTime = 0;
    private boolean relevant = false;

    public ServletHook(Context context) {
        this.context = context;
    }

    public static void init(CollectorRegistry registry) {

        // These example metrics are redundant, as the Summary already contains a count.
        // However, I want to show two types of metrics in the example code.

        Counter.build()
                .name(HTTP_REQUESTS_TOATAL)
                .labelNames("method", "path", "status")
                .help("Total number of http requests.")
                .register(registry);

        Summary.build()
                .quantile(0.5, 0.05)   // Add 50th percentile (= median) with 5% tolerated error
                .quantile(0.9, 0.01)   // Add 90th percentile with 1% tolerated error
                .quantile(0.99, 0.001) // Add 99th percentile with 0.1% tolerated error
                .name(HTTP_REQUEST_DURATION)
                .labelNames("method", "path", "status")
                .help("Duration for serving the http requests in seconds.")
                .register(registry);
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

        return path.replaceAll("[0-9]+", "{id}");
    }

    @Before(method = {"service", "doFilter"})
    public void before(ServletRequest request, ServletResponse response) {
        if (HttpServletRequest.class.isAssignableFrom(request.getClass()) && HttpServletResponse.class.isAssignableFrom(response.getClass())) {
            HttpServletRequest req = (HttpServletRequest) request;
            if (context.get(SERVLET_HOOK_METHOD).isPresent()) {
                // This is a nested call, i.e. this Servlet or Filter is called from within another Servlet or Filter.
                // We only instrument the outer-most call and ignore nested calls.
                // Returning here will leave the variable relevant=false, so the @After method does not do anything.
                return;
            }
            context.put(SERVLET_HOOK_METHOD, req.getMethod());
            context.put(SERVLET_HOOK_PATH, stripPathParameters(req.getRequestURI()));
            startTime = System.nanoTime();
            relevant = true;
        }
    }

    @After(method = {"service", "doFilter"})
    public void after(ServletRequest request, ServletResponse response) throws Exception {
        if (HttpServletRequest.class.isAssignableFrom(request.getClass()) && HttpServletResponse.class.isAssignableFrom(response.getClass())) {
            HttpServletResponse resp = (HttpServletResponse) response;
            if (relevant) {
                try {
                    double duration = ((double) System.nanoTime() - startTime) / (double) TimeUnit.SECONDS.toNanos(1L);
                    String method = context.get(SERVLET_HOOK_METHOD).get();
                    String path = context.get(SERVLET_HOOK_PATH).get();
                    MetricsUtil.inc(HTTP_REQUESTS_TOATAL, method, path, Integer.toString(resp.getStatus()));
                    MetricsUtil.observe(duration, HTTP_REQUEST_DURATION, method, path, Integer.toString(resp.getStatus()));
                } finally {
                    context.clear(SERVLET_HOOK_METHOD, SERVLET_HOOK_PATH);
                }
            }
        }
    }
}
