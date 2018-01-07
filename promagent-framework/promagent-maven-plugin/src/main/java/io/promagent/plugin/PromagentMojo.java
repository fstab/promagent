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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

import static io.promagent.plugin.AgentJar.Directory.PER_DEPLOYMENT_JARS;
import static io.promagent.plugin.AgentJar.Directory.SHARED_JARS;

@Mojo(name = "build", aggregator = true, defaultPhase = LifecyclePhase.PACKAGE, requiresDependencyResolution = ResolutionScope.RUNTIME)
public class PromagentMojo extends AbstractMojo {

    @Component
    private MavenProject project;

    @Component
    private PluginDescriptor pluginDescriptor;

    @Override
    public void execute() throws MojoExecutionException {

        JarFileNames jarFileNames = JarFileNames.renameOrigJarFile(project);
        AgentDependencies agentDependencies = AgentDependencies.init(pluginDescriptor);

        try (AgentJar agentJar = AgentJar.create(jarFileNames.getFinalName().toFile())) {
            // Add extracted agent classes
            agentJar.extractJar(agentDependencies.getAgentArtifact().getFile(), new ManifestTransformer(pluginDescriptor));
            // Add project jar
            agentDependencies.assertNoConflict(project.getArtifact());
            agentJar.addFile(jarFileNames.getNameAfterMove().toFile(), jarFileNames.getDefaultFinalName().getFileName().toString(), PER_DEPLOYMENT_JARS);
            // Add project dependencies
            for (Artifact artifact : project.getArtifacts()) {
                agentDependencies.assertNoConflict(artifact);
                agentJar.addFile(artifact.getFile(), SHARED_JARS);
            }
            // Add agent internal jars
            for (Artifact artifact : agentDependencies.getDependencies()) {
                agentJar.addFile(artifact.getFile(), SHARED_JARS);
            }
        }
    }
}
