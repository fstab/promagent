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

package io.promagent.plugin;

import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.plexus.util.IOUtil;

import java.io.*;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;

class AgentJar implements AutoCloseable {

    private final String jarName;
    private final JarOutputStream jarOutputStream;
    private final Set<String> content = new HashSet<>();

    enum Directory {
        SHARED_JARS("shared-jars/"), // Directory entries in JAR file must end in '/'
        PER_DEPLOYMENT_JARS("per-deployment-jars/");

        private final String name;

        Directory(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    private AgentJar(String jarName, JarOutputStream jarOutputStream) {
        this.jarName = jarName;
        this.jarOutputStream = jarOutputStream;
    }

    static AgentJar create(String targetDirectory, String jarName) throws MojoExecutionException {
        File agentJarFile = new File(targetDirectory, jarName);
        try {
            JarOutputStream jarOutputStream = new JarOutputStream(new BufferedOutputStream(new FileOutputStream(agentJarFile)));
            return new AgentJar(jarName, jarOutputStream);
        } catch (IOException e) {
            throw new MojoExecutionException("Error creating " + jarName + ": " + e.getMessage(), e);
        }
    }

    void addFile(File srcFile, Directory targetDir) throws MojoExecutionException {
        String destPath = targetDir.getName() + srcFile.getName();
        if (content.contains(destPath)) {
            return;
        }
        makeDirsRecursively(destPath);
        content.add(destPath);
        try (InputStream in = new FileInputStream(srcFile)) {
            jarOutputStream.putNextEntry(new JarEntry(targetDir.getName() + srcFile.getName()));
            IOUtil.copy(in, jarOutputStream);
        } catch (IOException e) {
            throw new MojoExecutionException("Error adding " + srcFile.getName() + " to target JAR: " + e.getMessage(), e);
        }
    }

    void extractJar(File jar, ManifestTransformer manifestTransformer) throws MojoExecutionException {
        try (JarFile jarFile = new JarFile(jar)) {
            for (Enumeration<JarEntry> jarEntries = jarFile.entries(); jarEntries.hasMoreElements(); ) {
                JarEntry jarEntry = jarEntries.nextElement();
                if (manifestTransformer.canTransform(jarEntry)) {
                    jarEntry = manifestTransformer.transform(jarEntry);
                }
                if (!jarEntry.isDirectory() && !content.contains(jarEntry.getName())) {
                    content.add(jarEntry.getName());
                    makeDirsRecursively(jarEntry.getName());
                    try (InputStream in = getInputStream(jarEntry, jarFile, manifestTransformer)) {
                        jarOutputStream.putNextEntry(jarEntry);
                        IOUtil.copy(in, jarOutputStream);
                    }
                }
            }
        } catch (IOException e) {
            throw new MojoExecutionException("Error adding " + jar.getName() + " to target JAR: " + e.getMessage(), e);
        }
    }

    private InputStream getInputStream(JarEntry jarEntry, JarFile jarFile, ManifestTransformer manifestTransformer) throws IOException, MojoExecutionException {
        InputStream in = jarFile.getInputStream(jarEntry);
        if (manifestTransformer.canTransform(jarEntry)) {
            in = manifestTransformer.transform(in);
        }
        return in;
    }

    private void makeDirsRecursively(String path) throws MojoExecutionException {
        String[] parts = path.split("/+");
        String segment = "";
        for (int i=0; i<parts.length-1; i++) {
            segment += parts[i] + "/";
            if (!content.contains(segment)) {
                try {
                    jarOutputStream.putNextEntry(new JarEntry(segment));
                } catch (IOException e) {
                    throw new MojoExecutionException("Error adding directory " + segment + " to target JAR: " + e.getMessage(), e);
                }
                content.add(segment);
            }
        }
    }

    @Override
    public void close() throws MojoExecutionException {
        try {
            jarOutputStream.close();
        } catch (IOException e) {
            throw new MojoExecutionException("Error finalizing " + jarName + ": " + e.getMessage(), e);
        }
    }
}
