package mojo;

import data.ProjectData;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import report.ReportGenerator;
import utils.Deserializer;
import utils.JDFCUtils;

import java.io.File;

import static utils.Constants.JDFC_SERIALIZATION_FILE;

@Mojo(name = "create-report", defaultPhase = LifecyclePhase.TEST, threadSafe = true)
public class ReportMojo extends AbstractMojo {

    @Parameter(property = "project", readonly = true)
    private MavenProject project;

    @Override
    public void execute() {
        ProjectData deserialized = Deserializer.deserializeCoverageData(JDFCUtils.getJDFCSerFileAbs());
        if(deserialized == null) {
            throw new IllegalArgumentException("Unable do deserialize coverage data.");
        }
        ProjectData.setInstance(deserialized);
        final String outDirAbs = String.format("%s%sjdfc-report", ProjectData.getInstance().getBuildDir().getAbsolutePath(), File.separator);
        final String sourceDirAbs = project.getBuild().getSourceDirectory();

        ReportGenerator reportGenerator = new ReportGenerator(outDirAbs, sourceDirAbs);
        reportGenerator.create();
    }
}
