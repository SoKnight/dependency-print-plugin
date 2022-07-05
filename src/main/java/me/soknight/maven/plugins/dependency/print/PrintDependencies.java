package me.soknight.maven.plugins.dependency.print;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

import java.util.*;

@Mojo(
        name = "print-dependencies",
        defaultPhase = LifecyclePhase.PACKAGE,
        requiresDependencyCollection = ResolutionScope.COMPILE,
        requiresDependencyResolution = ResolutionScope.COMPILE
)
public final class PrintDependencies extends DependencyPrintMojo {

    @Parameter(defaultValue = "true")
    private boolean includeDependencies;

    @Parameter(defaultValue = "true")
    private boolean includeProjectArtifacts;

    @Parameter(defaultValue = "false")
    private boolean printType;

    @SuppressWarnings("unchecked")
    @Override
    public void execute() throws MojoExecutionException {
        Set<Artifact> dependencies = new TreeSet<>(Comparator.comparing(Artifact::getGroupId).thenComparing(Artifact::getArtifactId));
        dependencies.addAll(project.getArtifacts());
        dependencies.removeIf(a -> !a.getScope().equals("compile"));

        verbose("%d compile dependencies found:", dependencies.size());

        List<String> content = new ArrayList<>();
        for (Artifact dependency : dependencies) {
            String groupId = dependency.getGroupId();
            boolean isProjectArtifact = isProjectGroupId(groupId);

            if (isProjectArtifact && !includeProjectArtifacts)
                continue;

            if (!isProjectArtifact && !includeDependencies)
                continue;

            StringBuilder builder = new StringBuilder();
            if (printGroupId)
                builder.append(groupId);

            String artifactId = dependency.getArtifactId();
            if (printArtifactId) {
                if (builder.length() != 0)
                    builder.append(":");
                builder.append(artifactId);
            }

            String version = dependency.getVersion();
            if (printVersion) {
                if (builder.length() != 0)
                    builder.append(":");
                builder.append(version);
            }

            String type = dependency.getType();
            if (printType) {
                if (builder.length() != 0)
                    builder.append(":");
                builder.append(type);
            }

            ArtifactRepository repository = dependency.getRepository();
            String url = repository != null ? repository.getUrl() : null;

            if (printRepositoryUrl && url != null) {
                if (builder.length() != 0)
                    builder.append(" from ");
                builder.append(url);
            }

            verbose("- %s:%s:%s (%s) from repository '%s'", groupId, artifactId, version, type, url);

            content.add(builder.toString());
        }

        write(content);
    }

}
