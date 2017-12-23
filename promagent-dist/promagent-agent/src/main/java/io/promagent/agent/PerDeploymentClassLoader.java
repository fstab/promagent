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

package io.promagent.agent;

import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;

/**
 * Class loader for loading Hooks (like ServletHook or JdbcHook).
 * <p/>
 * There is one instance per deployment in an application server, because hook classes may
 * reference classes from the deployment, e.g. as parameters to the before() and after() methods.
 * <p/>
 * However, loading shared classes like the Prometheus client library is delegated to the {@link #sharedClassLoader},
 * because the Prometheus metric registry should be accessible across all deployments within an application server.
 */
class PerDeploymentClassLoader extends URLClassLoader {

    private final URLClassLoader sharedClassLoader; // for loading the Prometheus client library

    PerDeploymentClassLoader(URL[] perDeploymentJars, URLClassLoader sharedClassLoader, ClassLoader parent) {
        super(perDeploymentJars, parent);
        this.sharedClassLoader = sharedClassLoader;
    }

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        try {
            return sharedClassLoader.loadClass(name); // The Prometheus client library should all have the same initiating loader across deployments.
        } catch (ClassNotFoundException e) {
            return super.loadClass(name); // Hooks should have different initiating loaders if the context loader differs.
        }
    }

    // Called by Byte buddy to load the PromagentAdvice.
    @Override
    public InputStream getResourceAsStream(String name) {
        InputStream result = sharedClassLoader.getResourceAsStream(name);
        if (result == null) {
            result = super.getResourceAsStream(name);
        }
        return result;
    }
}
