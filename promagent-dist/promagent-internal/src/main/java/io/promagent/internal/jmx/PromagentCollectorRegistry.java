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

package io.promagent.internal.jmx;

import io.prometheus.client.Collector;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.SimpleCollector;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Field;

/**
 * This is like the regular {@link CollectorRegistry}, except that when you {@link #register(Collector)} a metric,
 * the metric will also be registered as an MBean in the JMX platform server.
 */
public class PromagentCollectorRegistry extends CollectorRegistry {

    @Override
    public void register(Collector metric) {
        super.register(metric);
        try {
            ManagementFactory.getPlatformMBeanServer().registerMBean(new Metric(metric), makeObjectName((SimpleCollector) metric));
        } catch (Exception e) {
            throw new RuntimeException("Failed to register Prometheus metric: " + e.getMessage(), e);
        }
    }

    private static ObjectName makeObjectName(SimpleCollector metric) throws MalformedObjectNameException {
        return makeObjectName(getFullName(metric));
    }

    private static ObjectName makeObjectName(String fullname) throws MalformedObjectNameException {
        return new ObjectName("io.promagent:type=metrics,name=" + fullname);
    }

    private static String getFullName(SimpleCollector metric) {
        // Unfortunately, there is no public API to get the 'fullname' of a metric. We use reflection to get it anyway.
        try {
            Field field = SimpleCollector.class.getDeclaredField("fullname");
            field.setAccessible(true);
            return (String) field.get(metric);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            throw new RuntimeException("Failed to access " + metric.getClass().getName() + ".fullname. " +
                    "This is probably because the internal implementation of the Prometheus client library has changed. " +
                    "You should adapt the Promagent accordingly.", e);
        }
    }

    public void registerNoJmx(Collector collector) {
        super.register(collector);
    }
}
