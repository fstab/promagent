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
import io.prometheus.client.SimpleCollector;

import javax.management.*;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

public class MetricsUtil {

    /**
     * The fullName must refer to a Counter or a Gauge metric.
     */
    public static void inc(String fullName, String... labelValues) throws MalformedObjectNameException, MBeanException, InstanceNotFoundException, ReflectionException {
        performOperation("inc", fullName, labelValues);
    }

    /**
     * The fullName must refer to a Gauge metric.
     */
    public static void dec(String fullName, String... labelValues) throws MalformedObjectNameException, MBeanException, InstanceNotFoundException, ReflectionException {
        performOperation("dec", fullName, labelValues);
    }

    /**
     * The fullName must refer to a Gauge metric.
     */
    public static void set(double value, String fullName, String... labelValues) throws MalformedObjectNameException, MBeanException, InstanceNotFoundException, ReflectionException {
        performOperation("set", fullName, value, labelValues);
    }

    /**
     * The fullName must refer to a Summary or a Histogram metric.
     */
    public static void observe(double value, String fullName, String... labelValues) throws MalformedObjectNameException, MBeanException, InstanceNotFoundException, ReflectionException {
        performOperation("observe", fullName, value, labelValues);
    }

    /**
     * Get the values in a representation that can be used in MXBeans in JMX.
     * <p/>
     * The result is a map of labels -> value.
     * The labels themselves are represented as a key -> value map.
     */
    static Map<Map<String, String>, Double> getValues(Collector metric) {
        Map<Map<String, String>, Double> result = new HashMap<>();
        for (Collector.MetricFamilySamples samples : metric.collect()) {
            for (Collector.MetricFamilySamples.Sample sample : samples.samples) {
                Map<String, String> labels = new HashMap<>();
                for (int i = 0; i < sample.labelNames.size(); i++) {
                    labels.put(sample.labelNames.get(i), sample.labelValues.get(i));
                }
                result.put(labels, sample.value);
            }
        }
        return result;
    }

    static ObjectName makeObjectName(SimpleCollector metric) throws MalformedObjectNameException {
        return makeObjectName(getFullName(metric));
    }

    private static ObjectName makeObjectName(String fullname) throws MalformedObjectNameException {
        return new ObjectName("io.promagent:type=metrics,name=" + fullname);
    }

    private static void performOperation(String operationName, String metricFullname, String... labelValues) throws MalformedObjectNameException, MBeanException, InstanceNotFoundException, ReflectionException {
        ManagementFactory.getPlatformMBeanServer().invoke(
                makeObjectName(metricFullname),
                operationName,
                new Object[]{labelValues},
                new String[]{String[].class.getName()});
    }

    private static void performOperation(String operationName, String metricFullname, double value, String... labelValues) throws MalformedObjectNameException, MBeanException, InstanceNotFoundException, ReflectionException {
        ManagementFactory.getPlatformMBeanServer().invoke(
                makeObjectName(metricFullname),
                operationName,
                new Object[]{value, labelValues},
                new String[]{"double", String[].class.getName()});
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
}
