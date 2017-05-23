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

import java.util.Map;

public class Gauge implements GaugeMBean {

    private final io.prometheus.client.Gauge gauge;

    public Gauge(io.prometheus.client.Gauge gauge) {
        this.gauge = gauge;
    }

    @Override
    public void set(double value, String... labelValues) {
        gauge.labels(labelValues).set(value);
    }

    @Override
    public void inc(String... labelValues) {
        gauge.labels(labelValues).inc();
    }

    @Override
    public void dec(String... labelValues) {
        gauge.labels(labelValues).dec();
    }

    @Override
    public Map<Map<String, String>, Double> getValues() {
        return MetricsUtil.getValues(gauge);
    }
}
