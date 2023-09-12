package mojo;

import data.singleton.CoverageDataStore;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import report.ReportGenerator;
import utils.Deserializer;

import java.io.File;

@Mojo(name = "create-report", defaultPhase = LifecyclePhase.TEST, threadSafe = true)
public class ReportMojo extends AbstractMojo {

    @Parameter(property = "project", readonly = true)
    private MavenProject project;

    @Override
    public void execute() {
        CoverageDataStore.setInstance(Deserializer.deserializeCoverageData(project.getBasedir().toString()));
        final String outDirAbs = String.format("%s%sjdfc-report", CoverageDataStore.getInstance().getBuildDir().getAbsolutePath(), File.separator);
        final String sourceDirAbs = project.getBuild().getSourceDirectory();
        ReportGenerator reportGenerator = new ReportGenerator(outDirAbs, sourceDirAbs);
        reportGenerator.createHTMLReport();
    }
}
