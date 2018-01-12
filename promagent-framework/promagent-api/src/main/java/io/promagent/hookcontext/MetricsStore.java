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
import java.util.function.BiFunction;

/**
 * Instead of creating Prometheus Metrics directly, Hooks should use the {@link MetricsStore} like this:
 * <pre>
 * Counter httpRequestsTotal = metricsStore.createOrGet(new MetricDef<>(
 *         "http_requests_total",
 *         (name, registry) -> Counter.build()
 *                 .name(name)
 *                 .labelNames("method", "path", "status")
 *                 .help("Total number of http requests.")
 *                 .register(registry)
 * ));
 * </pre>
 * The Promgent framework will take care that each metric is created only once and re-used across re-deployments in an application server.
 */
public class MetricsStore {

    private final CollectorRegistry registry;
    private final ConcurrentMap<String, Collector> metrics = new ConcurrentHashMap<>();

    public MetricsStore(CollectorRegistry registry) {
        this.registry = registry;
    }

    /**
     * See {@link MetricsStore} and {@link MetricDef#MetricDef(String, BiFunction)}.
     */
    @SuppressWarnings("unchecked")
    public <T extends Collector> T createOrGet(MetricDef<T> metricDef) {
        return (T) metrics.computeIfAbsent(metricDef.getMetricName(), s -> metricDef.getProducer().apply(metricDef.getMetricName(), registry));
    }
}
