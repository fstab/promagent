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

package io.promagent.internal.metrics;

import io.prometheus.client.Collector;
import io.prometheus.client.CollectorRegistry;

import java.lang.management.ManagementFactory;

import static io.promagent.internal.metrics.MetricsUtil.makeObjectName;

/**
 * This is like the regular {@link CollectorRegistry}, except that when you {@link #register(Collector)} a metric,
 * the metric will also be registered as an MBean in the JMX platform server.
 */
public class PromagentCollectorRegistry extends CollectorRegistry {

    @Override
    public void register(Collector metric) {
        super.register(metric);
        try {
            if (io.prometheus.client.Counter.class.isAssignableFrom(metric.getClass())) {
                io.prometheus.client.Counter counter = (io.prometheus.client.Counter) metric;
                ManagementFactory.getPlatformMBeanServer().registerMBean(new Counter(counter), makeObjectName(counter));
            } else if (io.prometheus.client.Summary.class.isAssignableFrom(metric.getClass())) {
                io.prometheus.client.Summary summary = (io.prometheus.client.Summary) metric;
                ManagementFactory.getPlatformMBeanServer().registerMBean(new Summary(summary), makeObjectName(summary));
            } else {
                throw new Exception("Unsupported metric type: " + metric.getClass().getName());
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to register Prometheus metric: " + e.getMessage(), e);
        }
    }
}
