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

import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ClassLoaderCache stores the class loaders used for loading the Hooks (like ServletHook or JdbcHook).
 * <p/>
 * There is one {@link HookClassLoader} per deployment in an application server, because hook classes may
 * reference classes from the deployment, e.g. as parameters to the before() and after() methods.
 * <p/>
 * When {@link #currentClassLoader()} is called for the first time within a class loader context,
 * a new {@link HookClassLoader} is created on the fly. Repeated calls in the same context yield the same class loader.
 */
public class ClassLoaderCache {

    private static ClassLoaderCache instance;

    // TODO: The cache does not free class loaders when applications are undeployed. Maybe use WeakHashMap?
    private final Map<ClassLoader, HookClassLoader> cache = new HashMap<>();
    private final URLClassLoader metricsClassLoader;
    private final List<URL> hookJars;

    private ClassLoaderCache(JarFiles jarFiles) {
        metricsClassLoader = new URLClassLoader(jarFiles.getDependencyJars().toArray(new URL[]{}));
        hookJars = jarFiles.getHookJars();
    }

    public static synchronized ClassLoaderCache getInstance() {
        if (instance == null) {
            instance = new ClassLoaderCache(JarFiles.extract());
        }
        return instance;
    }

    public synchronized ClassLoader currentClassLoader() {
        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        if (! cache.containsKey(contextClassLoader)) {
            cache.put(contextClassLoader, new HookClassLoader(hookJars, metricsClassLoader, contextClassLoader));
        }
        return cache.get(contextClassLoader);
    }
}
