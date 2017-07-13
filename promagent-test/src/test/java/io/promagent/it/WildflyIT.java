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

package io.promagent.it;

import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class WildflyIT {

    /**
     * Run some HTTP queries against a Docker container from image promagent/wildfly-kitchensink-promagent.
     * <p/>
     * The Docker container is started by the maven-docker-plugin when running <tt>mvn verify</tt>.
     */
    @Test
    public void testWildfly() throws Exception {
        OkHttpClient client = new OkHttpClient();
        Request restRequest = new Request.Builder().url(System.getProperty("wildfly.url") + "/wildfly-kitchensink/rest/members").build();
        Request metricsRequest = new Request.Builder().url(System.getProperty("promagent.url") + "/metrics").build();

        // Execute REST call
        Response restResponse = client.newCall(restRequest).execute();
        Assertions.assertTrue(restResponse.body().string().contains("John Smith"));

        // requests_total metric should have count 1
        Response metricsResponse = client.newCall(metricsRequest).execute();
        Assertions.assertTrue(contains(metricsResponse.body().string(), "http_requests_total", "method=\"GET\"", "path=\"/kitchensink/rest/members\"", "status=\"200\"", "1.0"));

        // Execute REST call again
        restResponse = client.newCall(restRequest).execute();
        Assertions.assertTrue(restResponse.body().string().contains("John Smith"));

        // requests_total metric should have count 2
        metricsResponse = client.newCall(metricsRequest).execute();
        Assertions.assertTrue(contains(metricsResponse.body().string(), "http_requests_total", "method=\"GET\"", "path=\"/kitchensink/rest/members\"", "status=\"200\"", "2.0"));
    }

    /**
     * Test if there is a line containing all fragments.
     */
    private boolean contains(String lines, String... fragments) {
        for (String line : lines.split(System.lineSeparator())) {
            for (String fragment : fragments) {
                if (! line.contains(fragment)) {
                    continue;
                }
                return true;
            }
        }
        return false;
    }
}
