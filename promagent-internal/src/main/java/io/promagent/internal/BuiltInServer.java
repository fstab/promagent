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

package io.promagent.internal;

import com.sun.net.httpserver.HttpServer;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.exporter.common.TextFormat;

import java.io.StringWriter;
import java.net.InetSocketAddress;
import java.util.Collections;

/**
 * Use the Java runtime's built-in {@link HttpServer} to export Prometheus metrics.
 */
class BuiltInServer {

    static void run(String host, String portString, CollectorRegistry registry) throws Exception {
        try {
            int port = Integer.parseInt(portString);
            InetSocketAddress address = host == null ? new InetSocketAddress(port) : new InetSocketAddress(host, port);
            HttpServer httpServer = HttpServer.create(address, 10);
            httpServer.createContext("/metrics", httpExchange -> {
                StringWriter respBodyWriter = new StringWriter();
                TextFormat.write004(respBodyWriter, registry.metricFamilySamples());
                byte[] respBody = respBodyWriter.toString().getBytes("UTF-8");
                httpExchange.getResponseHeaders().put("Context-Type", Collections.singletonList("text/plain; charset=UTF-8"));
                httpExchange.sendResponseHeaders(200, respBody.length);
                httpExchange.getResponseBody().write(respBody);
                httpExchange.getResponseBody().close();
            });
            httpServer.start();
        } catch (NumberFormatException e) {
            throw new RuntimeException("Failed to parse command line arguments: '" + portString + "' is not a valid port number.");
        }
    }
}
