package io.promagent.plugin;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class JarFileNames {

    private final Path finalName; // The agent JAR file.
    private final Path defaultFinalName; // The hook implementation's JAR within the agent JAR.
    private final Path nameAfterMove; // The hook implementation's JAR in the target/ directory.

    private JarFileNames(Path finalName, Path defaultFinalName, Path nameAfterMove) {
        this.finalName = finalName;
        this.defaultFinalName = defaultFinalName;
        this.nameAfterMove = nameAfterMove;
    }

    public Path getFinalName() {
        return finalName;
    }

    public Path getDefaultFinalName() {
        return defaultFinalName;
    }

    public Path getNameAfterMove() {
        return nameAfterMove;
    }

    public static JarFileNames renameOrigJarFile(MavenProject project) throws MojoExecutionException {
        Path finalName = makePath(project.getBuild().getFinalName(), project);
        if (!Files.exists(finalName)) {
            throw new MojoExecutionException(finalName.getFileName() + ": file not found. This happens if promagent-maven-plugin is called before the original artifact was created.");
        }
        Path defaultFinalName = makePath(project.getArtifactId() + "-" + project.getArtifact().getVersion(), project);
        if (!Files.exists(defaultFinalName)) {
            mv(finalName, defaultFinalName);
            return new JarFileNames(finalName, defaultFinalName, defaultFinalName);
        } else {
            Path defaultFinalNameWithSuffix = makePath(project.getArtifactId() + "-" + project.getArtifact().getVersion() + "-orig", project);
            mv(finalName, defaultFinalNameWithSuffix);
            return new JarFileNames(finalName, defaultFinalName, defaultFinalNameWithSuffix);
        }
    }

    private static Path makePath(String fileNameWithoutExtension, MavenProject project) {
        String extension = project.getArtifact().getArtifactHandler().getExtension(); // "jar"
        return Paths.get(project.getBuild().getDirectory(), fileNameWithoutExtension + "." + extension);
    }

    private static void mv(Path src, Path dest) throws MojoExecutionException {
        try {
            Files.move(src, dest);
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to rename " + src.getFileName() + ": " + e.getMessage(), e);
        }
    }
}
