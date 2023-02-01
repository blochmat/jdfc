package mojo;

import data.CoverageDataImport;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.project.MavenProject;
import org.apache.maven.reporting.AbstractMavenReport;
import report.ReportGenerator;

import java.io.File;
import java.util.Locale;


@Mojo(name = "create-report", defaultPhase = LifecyclePhase.PREPARE_PACKAGE, threadSafe = true)
public class ReportMojo extends AbstractMavenReport {

    @Override
    protected String getOutputDirectory() {
        return null;
    }

    @Override
    protected MavenProject getProject() {
        return this.project;
    }

    @Override
    public void execute() {
        executeReport(Locale.getDefault());
    }

    @Override
    protected void executeReport(Locale locale) {
        final String target = getProject().getBuild().getDirectory();
        final String classesDir = getProject().getBuild().getOutputDirectory();
        final String importDir = String.format("%s%sjdfc", target, File.separator);

        // Load stored data into CoverageDataStore
        CoverageDataImport.loadExecutionData(classesDir, importDir);

        final String exportDir = String.format("%s%sjdfc-report", target, File.separator);
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
