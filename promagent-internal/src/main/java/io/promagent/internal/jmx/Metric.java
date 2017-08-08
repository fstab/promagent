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

import java.util.HashMap;
import java.util.Map;

public class Metric implements MetricMBean {

    private final Collector metric;

    Metric(Collector metric) {
        this.metric = metric;
    }

    /**
     * @see MetricMBean#getValues()
     */
    @Override
    public Map<Map<String, String>, Double> getValues() {
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
}
