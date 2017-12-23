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

import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.exporter.common.TextFormat;

import java.io.IOException;
import java.io.StringWriter;

public class Exporter implements ExporterMBean {

    private final CollectorRegistry registry;

    public Exporter(CollectorRegistry registry) {
        this.registry = registry;
    }

    @Override
    public String getTextFormat() {
        try {
            StringWriter result = new StringWriter();
            TextFormat.write004(result, registry.metricFamilySamples());
            return result.toString();
        } catch (IOException e) {
            throw new RuntimeException("Unexpected error when writing metrics to a String: " + e.getMessage(), e);
        }
    }
}
