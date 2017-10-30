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

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ClassLoaderCache stores the class loaders used for loading the Promagent modules,
 * i.e. promagent-hooks, promagent-annotations, promagent-internals, and their dependencies.
 * <p/>
 * For the Hooks (like ServletHook or JdbcHook) there is one class loader per deployment,
 * because hook classes may reference classes from the deployment,
 * e.g. as parameters to the before() and after() methods.
 * All other modules and their dependencies are loaded through a shared class loader.
 * <p/>
 * When {@link #currentClassLoader()} is called for the first time within a class loader context,
 * a new {@link PerDeploymentClassLoader} is created on the fly.
 * Repeated calls in the same context yield the same {@link PerDeploymentClassLoader}.
 */
public class ClassLoaderCache {

    private static ClassLoaderCache instance;

    // TODO: The cache does not free class loaders when applications are undeployed. Maybe use WeakHashMap?
    private final Map<ClassLoader, PerDeploymentClassLoader> cache = new HashMap<>();
    private final URLClassLoader sharedClassLoader; // shared across multiple deployments
    private final List<Path> perDeploymentJars; // one class loader for each deployment for these JARs

    private ClassLoaderCache(JarFiles jarFiles) {
        sharedClassLoader = new URLClassLoader(pathsToURLs(jarFiles.getSharedJars()));
        perDeploymentJars = jarFiles.getPerDeploymentJars();
    }

    public static synchronized ClassLoaderCache getInstance() {
        if (instance == null) {
            instance = new ClassLoaderCache(JarFiles.extract());
        }
        return instance;
    }

    public List<Path> getPerDeploymentJars() {
        return perDeploymentJars;
    }

    public synchronized ClassLoader currentClassLoader() {
        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        if (! cache.containsKey(contextClassLoader)) {
            cache.put(contextClassLoader, new PerDeploymentClassLoader(pathsToURLs(perDeploymentJars), sharedClassLoader, contextClassLoader));
        }
        return cache.get(contextClassLoader);
    }

    private static URL[] pathsToURLs(List<Path> paths) {
        try {
            URL[] result = new URL[paths.size()];
            for (int i=0; i<paths.size(); i++) {
                result[i] = paths.get(i).toUri().toURL();
            }
            return result;
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }
}
