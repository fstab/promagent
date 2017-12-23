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

package io.promagent.hookcontext;

import io.prometheus.client.Collector;
import io.prometheus.client.CollectorRegistry;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Use the MetricsStore when creating metrics in Hooks to make sure these metrics are created only once.
 * <p/>
 * In regular Java applications, you can create Prometheus metrics as a static variable and call register(), like this:
 * <pre>
 *     private static final Counter httpRequestsTotal = Counter
 *             .build()
 *             .name("http_requests_total")
 *             .help("Total number of HTTP requests")
 *             .register();
 * </pre>
 * This works because a static variable is equivalent to a globally unique variable in most applications.
 * <p/>
 * However, in promagent, Hook classes may be loaded multiple times in different class loaders.
 * Therefore, if you define a static variable in a Hook, there might be multiple instances of that variable.
 * <p/>
 * Use the MetricsStore to make sure that Prometheus metrics are created only once.
 */
public class MetricsStore {

    private final CollectorRegistry registry;
    private final ConcurrentMap<String, Collector> metrics = new ConcurrentHashMap<>();

    public MetricsStore(CollectorRegistry registry) {
        this.registry = registry;
    }

    @SuppressWarnings("unchecked")
    public <T extends Collector> T createOrGet(MetricDef<T> metricDef) {
        return (T) metrics.computeIfAbsent(metricDef.getMetricName(), s -> metricDef.getProducer().apply(registry));
    }
}
