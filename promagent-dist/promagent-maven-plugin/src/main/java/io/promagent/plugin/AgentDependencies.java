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
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.descriptor.PluginDescriptor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import static java.util.stream.Collectors.toList;

/**
 * Hard-coded list of JAR files that must go into the agent.jar.
 * Versions are resolved through Maven.
 * If the user adds a duplicate JAR with a different version, the plugin fails.
 */
class AgentDependencies {

    private final String pluginGroupId;
    private final List<Artifact> dependencies;

    /**
     * Hard-coded dependencies without versions (versions are resolved dynamically).
     */
    private static class ExpectedDependency {

        final String groupId;
        final String artifactId;

        private ExpectedDependency(String groupId, String artifactId) {
            this.groupId = groupId;
            this.artifactId = artifactId;
        }
    }

    private AgentDependencies(String pluginGroupId, List<Artifact> dependencies) {
        this.pluginGroupId = pluginGroupId;
        this.dependencies = dependencies;
    }

    static AgentDependencies init(PluginDescriptor pluginDescriptor) throws MojoExecutionException {

        String pluginGroupId = pluginDescriptor.getGroupId();
        String pluginArtifactId = pluginDescriptor.getArtifactId();

        List<ExpectedDependency> expectedDependencies = Arrays.asList(
                new ExpectedDependency(pluginGroupId, "promagent-agent"),
                new ExpectedDependency(pluginGroupId, "promagent-internal"),
                new ExpectedDependency(pluginGroupId, "promagent-api"),
                new ExpectedDependency("io.prometheus", "simpleclient_common"),
                new ExpectedDependency("io.prometheus", "simpleclient"),
                new ExpectedDependency("net.bytebuddy", "byte-buddy"),
                new ExpectedDependency("commons-io", "commons-io")
        );

        List<Artifact> actualDependencies = resolveVersions(pluginDescriptor, pluginArtifactId, expectedDependencies);
        failUnlessComplete(actualDependencies, expectedDependencies, pluginArtifactId);
        return new AgentDependencies(pluginGroupId, actualDependencies);
    }

    /**
     * Artifact for the promagent-agent module.
     */
    Artifact getAgentArtifact() {
        return dependencies.stream()
                .filter(isAgent())
                .findFirst()
                .get(); // We know it's present.
    }

    /**
     * Artifact for all other runtime dependencies except promagent-agent.
     */
    List<Artifact> getDependencies() {
        return dependencies.stream()
                .filter(isAgent().negate())
                .collect(toList());
    }

    void assertNoConflict(Artifact artifact) throws MojoExecutionException {
        Optional<String> builtInVersion = dependencies.stream()
                .filter(dependency -> dependency.getGroupId().equals(artifact.getGroupId()))
                .filter(dependency -> dependency.getArtifactId().equals(artifact.getArtifactId()))
                .map(Artifact::getVersion)
                .findFirst();
        if (builtInVersion.isPresent() && ! builtInVersion.get().equals(artifact.getVersion())) {
            String artifactName = artifact.getGroupId() + ":" + artifact.getArtifactId();
            throw new MojoExecutionException("Conflicting dependencies: Your project includes " + artifactName +
                    " version " + artifact.getVersion() + " but the promagent-maven-plugin is built with version " + builtInVersion.get());
        }
    }

    private static List<Artifact> resolveVersions(PluginDescriptor pluginDescriptor, String pluginArtifactId, List<ExpectedDependency> expectedDependencies) throws MojoExecutionException {
        List<Artifact> actualDependencies = new ArrayList<>();
        for (Artifact artifact : pluginDescriptor.getArtifacts()) {
            if (! isExpected(artifact, expectedDependencies)) {
                continue;
            }
            if (isKnown(artifact, actualDependencies)) {
                continue;
            }
            failOnVersionConflict(artifact, actualDependencies, pluginArtifactId);
            actualDependencies.add(artifact);
        }
        return actualDependencies;
    }

    private Predicate<Artifact> isAgent() {
        return artifact -> artifact.getGroupId().equals(pluginGroupId) && artifact.getArtifactId().equals("promagent-agent");
    }


    private static Predicate<ExpectedDependency> expectedDependencyMatcher(Artifact artifact) {
        return expectedDependency -> expectedDependency.groupId.equals(artifact.getGroupId()) &&
                expectedDependency.artifactId.equals(artifact.getArtifactId());
    }

    private static Predicate<Artifact> expectedDependencyMatcher(ExpectedDependency expectedDependency) {
        return artifact -> expectedDependencyMatcher(artifact).test(expectedDependency);
    }

    private static Predicate<Artifact> artifactMatcherWithoutVersion(Artifact artifact) {
        return other -> artifact.getGroupId().equals(other.getGroupId()) &&
                artifact.getArtifactId().equals(other.getArtifactId());
    }

    private static Predicate<Artifact> artifactMatcherWithVersion(Artifact artifact) {
        return other -> artifactMatcherWithoutVersion(artifact).test(other) &&
            artifact.getVersion().equals(other.getVersion());
    }

    private static boolean isExpected(Artifact artifact, List<ExpectedDependency> expectedDependencies) {
        return expectedDependencies.stream().anyMatch(expectedDependencyMatcher(artifact));
    }

    private static boolean isKnown(Artifact artifact, List<Artifact> knownArtifacts) {
        return knownArtifacts.stream().anyMatch(artifactMatcherWithVersion(artifact));
    }

    private static void failOnVersionConflict(Artifact artifact, List<Artifact> knownArtifacts, String pluginArtifactId) throws MojoExecutionException {
        Optional<String> conflictingVersion = knownArtifacts.stream()
                .filter(artifactMatcherWithoutVersion(artifact))
                .filter(artifactMatcherWithVersion(artifact).negate()) // same version -> not conflicting
                .findFirst()
                .map(Artifact::getVersion);
        if (conflictingVersion.isPresent()) {
            String artifactName = artifact.getGroupId() + artifact.getArtifactId();
            throw new MojoExecutionException("version conflict in " + pluginArtifactId + ": " + artifactName + " found in version " + artifact.getVersion() + " and version " + conflictingVersion.get());
        }
    }

    private static void failUnlessComplete(List<Artifact> actualDependencies, List<ExpectedDependency> expectedDependencies, String pluginArtifactId) throws MojoExecutionException {
        for (ExpectedDependency expected : expectedDependencies) {
            if (actualDependencies.stream().noneMatch(expectedDependencyMatcher(expected))) {
                String dependencyName = expected.groupId + ":" + expected.artifactId;
                throw new MojoExecutionException("Plugin dependency " + dependencyName + " missing. This is a bug in " + pluginArtifactId + ".");
            }
        }
    }
}
