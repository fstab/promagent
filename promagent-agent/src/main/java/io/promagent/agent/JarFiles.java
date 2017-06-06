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
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The promagent.jar contains a lib/ directory with JARs from the promagent-internal module and its dependencies.
 * This class provides URLs to these JAR files.
 */
class JarFiles {

    // The separation between "hook JARs" and "dependency JARs" is still work in progress.
    // The idea is to have one global class loader for the Prometheus client library (dependencyJars),
    // because the Prometheus metric registry should be accessible from all deployments in the application server.
    // The hook classes themselves should be loaded with one class loader per deployment, because hooks might
    // reference classes from the deployment, so each hook class loader should have the current context
    // class loader as its parent.
    // The current implementation works for the ServletHook and JdbcHook, but there are still some TODOs,
    // like making it easily extensible for other hooks, how to deal with the Byte buddy JAR, getting rid
    // of the runtime dependency on servlet-api JAR, etc.

    private final List<URL> hookJars; // The JAR file for the promagent-internal module.
    private final List<URL> dependencyJars; // The JAR files for the Prometheus client and its dependencies.

    private JarFiles(List<URL> hookJars, List<URL> dependencyJars) {
        this.hookJars = Collections.unmodifiableList(hookJars);
        this.dependencyJars = Collections.unmodifiableList(dependencyJars);
    }

    public List<URL> getHookJars() {
        return hookJars;
    }

    public List<URL> getDependencyJars() {
        return dependencyJars;
    }

    /**
     * Theoretically we could return a list of jar:/ URLs without extracting the JARs,
     * but the URLClassLoader has a bug such that jar:/ URLs cannot be used. Therefore, we have
     * to extract the JARs and return a list of file:/ URLs.
     * See https://bugs.openjdk.java.net/browse/JDK-4735639
     */
    static JarFiles extract() {
        List<URL> hookJars = new ArrayList<>();
        List<URL> dependencyJars = new ArrayList<>();
        Path agentJar = findAgentJar();
        try (JarFile jarFile = new JarFile(agentJar.toFile())) {
//            hookJars.add(agentJar.toUri().toURL());
            Path tmpDir = Files.createTempDirectory("promagent-");
            tmpDir.toFile().deleteOnExit();
            Enumeration<JarEntry> jarEntries = jarFile.entries();
            while (jarEntries.hasMoreElements()) {
                JarEntry jarEntry = jarEntries.nextElement();
                if (jarEntry.getName().startsWith("lib/") && jarEntry.getName().endsWith(".jar")) {
                    Path tmpFile = tmpDir.resolve(jarEntry.getName().replaceAll(".*/", ""));
                    Files.copy(jarFile.getInputStream(jarEntry), tmpFile);
                    if (jarEntry.getName().contains("promagent") || jarEntry.getName().contains("javax")) {
                        hookJars.add(tmpFile.toUri().toURL());
                    } else {
                        dependencyJars.add(tmpFile.toUri().toURL());
                    }
                }
            }
            return new JarFiles(hookJars, dependencyJars);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load promagent.jar: " + e.getMessage(), e);
        }
    }

    private static Path findAgentJar() {
        CodeSource cs = Promagent.class.getProtectionDomain().getCodeSource();
        if (cs != null) {
            return findAgentJarFromCodeSource(cs);
        } else {
            // This happens if the Promagent class is loaded from the bootstrap class loader,
            // i.e. in addition to the command line argument -javaagent:/path/to/promagent.jar,
            // the argument -Xbootclasspath/p:/path/to/promagent.jar is used.
            return findAgentJarFromCmdline(ManagementFactory.getRuntimeMXBean().getInputArguments());
        }
    }

    private static Path findAgentJarFromCodeSource(CodeSource cs) {
        try {
            return Paths.get(cs.getLocation().toURI());
        } catch (URISyntaxException e) {
            throw new RuntimeException("Failed to load promagent.jar from " + cs.getLocation() + ": " + e.getMessage(), e);
        }
    }

    static Path findAgentJarFromCmdline(List<String> cmdlineArgs) {
        Pattern p = Pattern.compile("^-javaagent:(.*promagent([^/]*).jar)(=.*)?$");
        for (String arg : cmdlineArgs) {
            Matcher m = p.matcher(arg);
            if (m.matches()) {
                return Paths.get(m.group(1));
            }
        }
        throw new RuntimeException("Failed to locate promagent.jar file.");
    }
}
