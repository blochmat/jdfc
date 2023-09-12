package ant;

import data.singleton.CoverageDataStore;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import report.ReportGenerator;
import utils.Deserializer;

import java.io.File;

import static utils.Constants.JDFC_SERIALIZATION_FILE;

public class ReportTask extends Task {

    private String work;

    private String out;

    public void setOut(String out) {
        this.out = out;
    }

    public void setWork(String work) {
        this.work = work;
    }

    @Override
    public void execute() throws BuildException {
        String fileInAbs = String.join(File.separator, work, JDFC_SERIALIZATION_FILE);
        CoverageDataStore deserialized = Deserializer.deserializeCoverageData(fileInAbs);
        if(deserialized == null) {
            throw new IllegalArgumentException("Unable do deserialize coverage data.");
        }
        CoverageDataStore.setInstance(deserialized);
        String outAbs = String.join(File.separator, CoverageDataStore.getInstance().getWorkDir().getAbsolutePath(), out);
        ReportGenerator reportGenerator = new ReportGenerator(outAbs, CoverageDataStore.getInstance().getSourceDirAbs());
        reportGenerator.createHTMLReport();
    }
}
