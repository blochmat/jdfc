package mojo;

import data.singleton.CoverageDataStore;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import utils.Instrumenter;

import java.io.File;
import java.util.List;

@Mojo(name = "prepare-agent", defaultPhase = LifecyclePhase.PROCESS_TEST_CLASSES, requiresDependencyResolution = ResolutionScope.RUNTIME, threadSafe = true)
public class AgentMojo extends AbstractMojo {

    @Parameter(property = "project", readonly = true)
    private MavenProject project;

    @Override
    public void execute()  {
        final String workDirAbs = project.getBasedir().toString();
        final String buildDirAbs = project.getBuild().getDirectory();
        final String classesDirAbs = project.getBuild().getOutputDirectory();
        final String sourceDirAbs = project.getBuild().getSourceDirectory();
        CoverageDataStore.getInstance().saveProjectInfo(workDirAbs, buildDirAbs, classesDirAbs, sourceDirAbs);
        Instrumenter instrumenter = new Instrumenter(workDirAbs, classesDirAbs);
        List<File> classFiles = instrumenter.loadClassFiles();
        for (File classFile : classFiles) {
            instrumenter.instrumentClass(classFile.getAbsolutePath());
        }
    }
}
