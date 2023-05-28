package mojo;

import data.CoverageDataImport;
import data.CoverageDataStore;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.reporting.AbstractMavenReport;
import report.ReportGenerator;

import java.io.File;
import java.util.List;
import java.util.Locale;


@Mojo(name = "create-report", defaultPhase = LifecyclePhase.TEST, threadSafe = true)
public class ReportMojo extends AbstractMavenReport {

    @Override
    public void execute() {
        executeReport(Locale.getDefault());
    }

    @Override
    protected void executeReport(Locale locale) {
        getLog().info("Report creation started.");
        final String projectDirStr = getProject().getBasedir().toString(); // /home/path/to/project/root
        final String buildDirStr = getProject().getBuild().getDirectory(); // default: target
        final String classesBuildDirStr = getProject().getBuild().getOutputDirectory(); // default: target/classes
        final List<String> sourceDirStrList = getProject().getCompileSourceRoots(); // [/home/path/to/project/src,..]
        final String importDir = String.format("%s%sjdfc", buildDirStr, File.separator);

        CoverageDataStore.getInstance().saveProjectInfo(projectDirStr, buildDirStr, classesBuildDirStr, sourceDirStrList);
        CoverageDataImport.loadExecutionData(classesBuildDirStr, importDir);

        final String exportDir = String.format("%s%sjdfc-report", buildDirStr, File.separator);
        final String source = getProject().getBuild().getSourceDirectory();
        ReportGenerator reportGenerator = new ReportGenerator(exportDir, source);
        reportGenerator.createReport();
    }

    @Override
    public String getOutputName() {
        return null;
    }

    @Override
    public String getName(Locale locale) {
        return null;
    }

    @Override
    public String getDescription(Locale locale) {
        return null;
    }
}
