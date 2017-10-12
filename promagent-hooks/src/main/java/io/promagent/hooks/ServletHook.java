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

import io.promagent.agent.annotations.After;
import io.promagent.agent.annotations.Before;
import io.promagent.agent.annotations.Hook;
import io.promagent.internal.Context;
import io.promagent.metrics.Metrics;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.concurrent.TimeUnit;

@Hook(instruments = {
        "javax.servlet.Servlet",
        "javax.servlet.Filter"
})
public class ServletHook {

    private long startTime = 0;
    private boolean relevant = false;

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
        if (HttpServletRequest.class.isAssignableFrom(request.getClass()) && HttpServletResponse.class.isAssignableFrom(response.getClass())) {
            HttpServletRequest req = (HttpServletRequest) request;
            if (Context.get(Context.SERVLET_HOOK_METHOD).isPresent()) {
                // This is a nested call, i.e. this Servlet or Filter is called from within another Servlet or Filter.
                // We only instrument the outer-most call and ignore nested calls.
                // Returning here will leave the variable relevant=false, so the @After method does not do anything.
                return;
            }
            Context.put(Context.SERVLET_HOOK_METHOD, req.getMethod());
            Context.put(Context.SERVLET_HOOK_PATH, stripPathParameters(req.getRequestURI()));
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
                    String method = Context.get(Context.SERVLET_HOOK_METHOD).get();
                    String path = Context.get(Context.SERVLET_HOOK_PATH).get();
                    Metrics.httpRequestsTotal.labels(method, path, Integer.toString(resp.getStatus())).inc();
                    Metrics.httpRequestsDuration.labels(method, path, Integer.toString(resp.getStatus())).observe(duration);
                } finally {
                    Context.clear(Context.SERVLET_HOOK_METHOD, Context.SERVLET_HOOK_PATH);
                }
            }
        }
    }
}
