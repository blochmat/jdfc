package mojo;

import data.singleton.CoverageDataStore;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;
import utils.Instrumenter;

import java.io.File;
import java.util.List;

@Mojo(name = "prepare-agent", defaultPhase = LifecyclePhase.PROCESS_TEST_CLASSES, requiresDependencyResolution = ResolutionScope.RUNTIME, threadSafe = true)
public class AgentMojo extends AbstractJdfcMojo {

    private static final String JDFC_INSTRUMENTED = ".jdfc_instrumented";

    /**
     * When executing the mojo we add the agent argument to the command line.
     * If ths seems unnecessarily complicated to you then you might be right.
     *
     */
    @Override
    protected void executeMojo()  {
        final String workDirAbs = getProject().getBasedir().toString();
        final String buildDirAbs = getProject().getBuild().getDirectory();
        final String classesDirAbs = getProject().getBuild().getOutputDirectory();
        final String sourceDirAbs = getProject().getCompileSourceRoots().get(0);
        CoverageDataStore.getInstance().saveProjectInfo(workDirAbs, buildDirAbs, classesDirAbs, sourceDirAbs);
        Instrumenter instrumenter = new Instrumenter(workDirAbs, classesDirAbs);
        List<File> classFiles = instrumenter.loadClassFiles();
        for (File classFile : classFiles) {
            instrumenter.instrumentClass(classFile.getAbsolutePath());
        }
    }
}
