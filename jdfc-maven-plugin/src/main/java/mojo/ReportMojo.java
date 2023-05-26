package mojo;

import data.CoverageDataImport;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.reporting.AbstractMavenReport;
import report.ReportGenerator;

import java.io.File;
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
        final String targetDirStr = getProject().getBuild().getDirectory(); // target
        final String classesDirStr = getProject().getBuild().getOutputDirectory(); // target/classes
        final String importDir = String.format("%s%sjdfc", targetDirStr, File.separator);

        // Load stored data into CoverageDataStore
        CoverageDataImport.loadExecutionData(classesDirStr, importDir);

        final String exportDir = String.format("%s%sjdfc-report", targetDirStr, File.separator);
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
