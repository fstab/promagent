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

package io.promagent.collectors;

import com.sun.management.OperatingSystemMXBean;
import io.prometheus.client.Collector;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

// TODO -- the following is an experiment supporting collectors directly (in addition to hooks)
// TODO -- This class is not loaded by default, see commented-out lines in io.promagent.internal.Promagent.premain()
// See JmxCollector in jmx_exporter
public class JmxCollector extends Collector implements Collector.Describable {

    @Override
    public List<MetricFamilySamples> collect() {
        List<MetricFamilySamples> result = new ArrayList<>();
        result.add(collectOperatingSystemMetrics());
        return Collections.unmodifiableList(result);
    }

    @Override
    public List<MetricFamilySamples> describe() {
        return new ArrayList<>();
    }

    private MetricFamilySamples collectOperatingSystemMetrics() {
        OperatingSystemMXBean operatingSystemMXBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
        MetricFamilySamples.Sample cpuLoad = new MetricFamilySamples.Sample("process_cpu_load", new ArrayList<>(), new ArrayList<>(), operatingSystemMXBean.getProcessCpuLoad());
        return new MetricFamilySamples(cpuLoad.name, Type.GAUGE, "recent cpu usage for the whole system", Arrays.asList(cpuLoad));
    }
}
