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
import org.apache.maven.plugins.annotations.*;
import org.apache.maven.project.MavenProject;

import java.util.List;

import static io.promagent.plugin.AgentJar.Directory.PER_DEPLOYMENT_JARS;
import static io.promagent.plugin.AgentJar.Directory.SHARED_JARS;

@Mojo(name = "build", aggregator = true, defaultPhase = LifecyclePhase.PACKAGE, requiresDependencyResolution = ResolutionScope.RUNTIME)
public class PromagentMojo extends AbstractMojo {

    @Parameter( defaultValue = "${project.artifactId}" )
    private String promagentArtifactId;

    @Parameter( defaultValue = "agent" )
    private String promagentClassifierName;

    @Component
    private MavenProject project;

    @Component
    private PluginDescriptor pluginDescriptor;

    @Parameter(property = "project.build.directory", readonly = true)
    private String outputDirectory;

    @Override
    public void execute() throws MojoExecutionException {

        AgentDependencies agentDependencies = AgentDependencies.init(pluginDescriptor);

        try (AgentJar agentJar = AgentJar.create(outputDirectory, makeAgentJarName())) {
            // Add extracted agent classes
            agentJar.extractJar(agentDependencies.getAgentArtifact().getFile(), new ManifestTransformer(pluginDescriptor));
            // Add project jar
            agentDependencies.assertNoConflict(project.getArtifact());
            agentJar.addFile(project.getArtifact().getFile(), PER_DEPLOYMENT_JARS);
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

    private String makeAgentJarName() {
        Artifact artifact = project.getArtifact();
        return promagentArtifactId + "-" + artifact.getVersion() + "-" + promagentClassifierName + "." + artifact.getArtifactHandler().getExtension();
    }
}
