package me.soknight.maven.plugins.dependency.print;

import lombok.Getter;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

@Mojo(
        name = "print-modules",
        defaultPhase = LifecyclePhase.PACKAGE,
        requiresDependencyCollection = ResolutionScope.COMPILE,
        requiresDependencyResolution = ResolutionScope.COMPILE
)
public final class PrintModules extends DependencyPrintMojo {

    @Parameter(defaultValue = "false")
    private boolean printPackaging;

    @SuppressWarnings("unchecked")
    @Override
    public void execute() throws MojoExecutionException {
        MavenProject project = this.project;
        while (project.hasParent())
            project = project.getParent();

        Set<Module> modules = new TreeSet<>(Comparator.comparing(Module::getGroupId).thenComparing(Module::getArtifactId));
        Module parentModule = Module.createParent(project);
        List<String> moduleKeys = project.getModules();

        try {
            handleModules(modules, parentModule, moduleKeys);
        } catch (IOException | XmlPullParserException ex) {
            throw new MojoExecutionException(ex);
        }

        verbose("%d project modules found:", modules.size());

        List<String> content = new ArrayList<>();
        for (Module module : modules) {
            StringBuilder builder = new StringBuilder();

            String groupId = module.getGroupId();
            if (printGroupId)
                builder.append(groupId);

            String artifactId = module.getArtifactId();
            if (printArtifactId) {
                if (builder.length() != 0)
                    builder.append(":");
                builder.append(artifactId);
            }

            String version = module.getVersion();
            if (printVersion) {
                if (builder.length() != 0)
                    builder.append(":");
                builder.append(version);
            }

            String packaging = module.getPackaging();
            if (printPackaging) {
                if (builder.length() != 0)
                    builder.append(":");
                builder.append(packaging);
            }

            content.add(builder.toString());

            verbose("- %s:%s:%s (%s)", groupId, artifactId, version, packaging);
        }

        write(content);
    }

    @SuppressWarnings("unchecked")
    private void handleModules(Set<Module> out, Module parent, List<String> modules) throws IOException, XmlPullParserException {
        if (modules.isEmpty())
            return;

        for (String module : modules) {
            Path moduleRoot = parent.getProjectRoot().resolve(module);
            Path modulePom = moduleRoot.resolve("pom.xml");

            if (!Files.isReadable(modulePom))
                return;

            MavenXpp3Reader mavenReader = new MavenXpp3Reader();
            Model moduleModel = mavenReader.read(Files.newInputStream(modulePom));
            MavenProject moduleProject = new MavenProject(moduleModel);

            Module child = parent.createChild(moduleRoot, moduleProject);
            List<String> childModules = moduleProject.getModules();

            if (childModules.isEmpty()) {
                out.add(child);
            } else
                handleModules(out, child, childModules);
        }
    }

    @Getter
    private static final class Module {

        private final Module parent;
        private final Path projectRoot;

        private final String groupId;
        private final String artifactId;
        private final String version;
        private final String packaging;

        private Module(Path projectRoot, MavenProject project) {
            this(null, projectRoot, project);
        }

        private Module(Module parent, Path projectRoot, MavenProject project) {
            this.parent = parent;
            this.projectRoot = projectRoot;

            this.groupId = project.getGroupId() != null ? project.getGroupId() : parent.getGroupId();
            this.artifactId = project.getArtifactId();
            this.version = project.getVersion() != null ? project.getVersion() : parent.getVersion();
            this.packaging = project.getPackaging();
        }

        public static Module createParent(MavenProject project) {
            return new Module(project.getBasedir().toPath(), project);
        }

        public Module createChild(Path moduleRoot, MavenProject project) {
            return new Module(this, moduleRoot, project);
        }

        public boolean hasParent() {
            return parent != null;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Module module = (Module) o;
            return Objects.equals(groupId, module.groupId) &&
                    Objects.equals(artifactId, module.artifactId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(groupId, artifactId);
        }

    }

}
