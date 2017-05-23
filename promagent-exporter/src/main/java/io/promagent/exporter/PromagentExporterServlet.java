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

package io.promagent.exporter;

import javax.management.ObjectName;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.management.ManagementFactory;

/**
 * This servlet simply calls the ExporterMBean via JMX and provides the result.
 */
@WebServlet("/")
public class PromagentExporterServlet extends HttpServlet {

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            String result = (String) ManagementFactory.getPlatformMBeanServer()
                    .getAttribute(new ObjectName("io.promagent:type=exporter"), "TextFormat");
            response.getWriter().println(result);
        } catch (Exception e) {
            response.setStatus(500);
            response.getWriter().println("Failed to load Exporter MBean. Are you sure the Prometheus agent is running?");
            e.printStackTrace();
        }
    }
}
