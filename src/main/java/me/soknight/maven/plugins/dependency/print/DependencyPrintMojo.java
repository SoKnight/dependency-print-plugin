package me.soknight.maven.plugins.dependency.print;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;

public abstract class DependencyPrintMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", readonly = true)
    protected MavenProject project;

    @Parameter(defaultValue = "${project.build.outputDirectory}", required = true)
    protected File outputDirectory;

    @Parameter(defaultValue = "dependency-print-output.txt", required = true)
    protected String outputFileName;

    @Parameter(defaultValue = "false")
    private boolean verbose;

    @Parameter
    protected List<String> projectGroupIds;

    @Parameter(defaultValue = "true")
    protected boolean printGroupId;

    @Parameter(defaultValue = "true")
    protected boolean printArtifactId;

    @Parameter(defaultValue = "true")
    protected boolean printVersion;

    @Parameter(defaultValue = "true")
    protected boolean printRepositoryUrl;

    protected void write(List<String> content) throws MojoExecutionException {
        try {
            Path outputFile = outputDirectory.toPath().resolve(outputFileName);
            if (!Files.isDirectory(outputFile.getParent()))
                Files.createDirectories(outputFile.getParent());

            Files.write(outputFile, content, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            verbose("Dependencies has been printed into: %s", outputFile.toAbsolutePath());
        } catch (IOException ex) {
            throw new MojoExecutionException(ex);
        }
    }

    protected boolean isProjectGroupId(String groupId) {
        return projectGroupIds != null && !projectGroupIds.isEmpty() && projectGroupIds.contains(groupId);
    }

    protected void verbose(String message, Object... args) {
        if (verbose)
            getLog().info(String.format(message, args));
    }

}
