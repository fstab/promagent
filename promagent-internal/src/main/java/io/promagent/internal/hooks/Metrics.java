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

import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.Counter;
import io.prometheus.client.Summary;

public class Metrics {

    static final String HTTP_REQUESTS_TOATAL = "http_requests_total";
    static final String HTTP_REQUEST_DURATION = "http_request_duration";

    static final String SQL_QUERIES_TOTAL = "sql_queries_total";
    static final String SQL_QUERY_DURATION = "sql_query_duration";

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

        Counter.build()
                .name(SQL_QUERIES_TOTAL)
                .labelNames("query", "method", "path")
                .help("Total number of sql queries.")
                .register(registry);

        Summary.build()
                .quantile(0.5, 0.05)   // Add 50th percentile (= median) with 5% tolerated error
                .quantile(0.9, 0.01)   // Add 90th percentile with 1% tolerated error
                .quantile(0.99, 0.001) // Add 99th percentile with 0.1% tolerated error
                .name(SQL_QUERY_DURATION)
                .labelNames("query", "method", "path")
                .help("Duration for serving the sql queries in seconds.")
                .register(registry);
    }
}
