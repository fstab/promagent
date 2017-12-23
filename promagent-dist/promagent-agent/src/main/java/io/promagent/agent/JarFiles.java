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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.function.Predicate;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The promagent.jar contains a lib/ directory with JARs from the promagent modules and their dependencies.
 * This class provides URLs to these JAR files.
 */
class JarFiles {

    // The separation between "per deployment JARs" and "shared JARs" is done because
    // in an application server some classes must be loaded for each deployment,
    // while other classes can be shared across multiple deployments.
    // Currently, only the "promagent-hooks" module is in the "per deployment JARs",
    // all other modules and their dependencies are in the "shared JARs".

    private final List<Path> perDeploymentJars; // classes loaded for each deployment
    private final List<Path> sharedJars; // classes shared across multiple deployments

    private JarFiles(List<Path> perDeploymentJars, List<Path> sharedJars) {
        this.perDeploymentJars = Collections.unmodifiableList(perDeploymentJars);
        this.sharedJars = Collections.unmodifiableList(sharedJars);
    }

    List<Path> getPerDeploymentJars() {
        return perDeploymentJars;
    }

    List<Path> getSharedJars() {
        return sharedJars;
    }

    /**
     * Theoretically we could return a list of jar:/ URLs without extracting the JARs,
     * but the URLClassLoader has a bug such that jar:/ URLs cannot be used. Therefore, we have
     * to extract the JARs and return a list of file:/ URLs.
     * See https://bugs.openjdk.java.net/browse/JDK-4735639
     */
    static JarFiles extract() {
        List<Path> perDeploymentJars = new ArrayList<>();
        List<Path> sharedJars = new ArrayList<>();
        Path agentJar = findAgentJar();
        List<Path> extractedJars;
        try {
            Path tmpDir = Files.createTempDirectory("promagent-");
            tmpDir.toFile().deleteOnExit();
            extractedJars = unzip(agentJar, tmpDir, entry -> entry.getName().endsWith(".jar"));
        } catch (IOException e) {
            throw new RuntimeException("Failed to load promagent.jar: " + e.getMessage(), e);
        }
        for (Path jar : extractedJars) {
            if (jar.getParent().getFileName().toString().equals("per-deployment-jars")) {
                perDeploymentJars.add(jar);
            } else {
                sharedJars.add(jar);
            }
        }
        return new JarFiles(perDeploymentJars, sharedJars);
    }

    private static List<Path> unzip(Path jarFile, Path destDir, Predicate<JarEntry> filter) throws IOException {
        List<Path> result = new ArrayList<>();
        try (JarFile agentJar = new JarFile(jarFile.toFile())) {
            Enumeration<JarEntry> jarEntries = agentJar.entries();
            while (jarEntries.hasMoreElements()) {
                JarEntry jarEntry = jarEntries.nextElement();
                if (filter.test(jarEntry)) {
                    Path destFile = destDir.resolve(jarEntry.getName());
                    if (!destFile.getParent().toFile().exists()) {
                        if (!destFile.getParent().toFile().mkdirs()) {
                            throw new IOException("Failed to make directory: " + destFile.getParent());
                        }
                    }
                    Files.copy(agentJar.getInputStream(jarEntry), destFile);
                    result.add(destFile);
                }
            }
        }
        return result;
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
