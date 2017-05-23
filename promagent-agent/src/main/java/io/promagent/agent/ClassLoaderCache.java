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

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.CodeSource;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * To understand the ClassLoaderCache, think of the ServletHook as an example.
 * The ServletHook instruments the Servlet method service(ServletRequest request, ServletResponse response).
 * We need to make sure that the ServletRequest and ServletResponse classes are loaded with the
 * context class loader Thread.currentThread().getContextClassLoader(), because this is the class loader used by
 * the Servlet implementation.
 * On the other hand, the ServletHook class itself can only be loaded by reading promagent-internal.jar.
 * Therefore, we need to create a URLClassLoader for promagent-internal.jar with the current context class loader as its parent.
 * The ClassLoaderCache caches these class loaders so that we create only one URLClassLoader per context class loader.
 */
class ClassLoaderCache {

    private static ClassLoaderCache instance;

    private final List<URL> urls;

    // TODO: Potential memory leak if applications are redeployed without restarting the application server,
    // because class loaders are kept in this cache even if they are not needed anymore.
    private final Map<ClassLoader, ClassLoader> classLoaderCache;

    private ClassLoaderCache(List<URL> urls) {
        this.urls = Collections.unmodifiableList(urls);
        this.classLoaderCache = new HashMap<>();
    }

    synchronized static ClassLoaderCache getInstance() {
        if (instance == null) {
            List<URL> urls = new ArrayList<>();
            Path agentJar = findAgentJar();
            // The URLClassLoader does not directly support jar: URLs, so we need to extract the nested
            // JARs to a temporary directory. See https://bugs.openjdk.java.net/browse/JDK-4735639
            try (JarFile jarFile = new JarFile(agentJar.toFile())) {
                urls.add(agentJar.toUri().toURL());
                Path tmpDir = Files.createTempDirectory("promagent-");
                tmpDir.toFile().deleteOnExit();
                Enumeration<JarEntry> jarEntries = jarFile.entries();
                while (jarEntries.hasMoreElements()) {
                    JarEntry jarEntry = jarEntries.nextElement();
                    if (jarEntry.getName().startsWith("lib/") && jarEntry.getName().endsWith(".jar")) {
                        Path tmpFile = tmpDir.resolve(jarEntry.getName().replaceAll(".*/", ""));
                        Files.copy(jarFile.getInputStream(jarEntry), tmpFile);
                        urls.add(tmpFile.toUri().toURL());
                    }
                }
                instance = new ClassLoaderCache(urls);
            } catch (IOException e) {
                throw new RuntimeException("Failed to load promagent.jar: " + e.getMessage(), e);
            }
        }
        return instance;
    }

    private static Path findAgentJar() {
        CodeSource cs = Promagent.class.getProtectionDomain().getCodeSource();
        if (cs == null) {
            // This happens if the Promagent class is loaded from the bootstrap classloader,
            // i.e. in addition to the command line argument -javaagent:/path/to/promagent.jar,
            // the argument -Xbootclasspath/p:/path/to/promagent.jar is used.
            for (String arg : ManagementFactory.getRuntimeMXBean().getInputArguments()) {
                if (arg.matches("^-javaagent:.*promagent([^/]*).jar$")) {
                    String path = arg.replace("-javaagent:", "");
                    return Paths.get(path);
                }
            }
        } else {
            try {
                return Paths.get(cs.getLocation().toURI());
            } catch (URISyntaxException e) {
                throw new RuntimeException("Failed to load promagent.jar from " + cs.getLocation() + ": " + e.getMessage(), e);
            }
        }
        throw new RuntimeException("Failed to locate promagent.jar file.");
    }

    synchronized Class<?> loadClass(String className) throws ClassNotFoundException, MalformedURLException {
        ClassLoader parent = Thread.currentThread().getContextClassLoader();
        ClassLoader composedClassLoader = classLoaderCache.get(parent);
        if (composedClassLoader == null) {
            composedClassLoader = URLClassLoader.newInstance(urls.toArray(new URL[urls.size()]), parent);
            classLoaderCache.put(parent, composedClassLoader);
        }
        return composedClassLoader.loadClass(className);
    }
}
