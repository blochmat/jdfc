package ant;

import data.ProjectData;
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
        ProjectData deserialized = Deserializer.deserializeCoverageData(fileInAbs);
        if(deserialized == null) {
            throw new IllegalArgumentException("Unable do deserialize coverage data.");
        }
        ProjectData.setInstance(deserialized);
        String outAbs = String.join(File.separator, ProjectData.getInstance().getWorkDir().getAbsolutePath(), out);

        ReportGenerator reportGenerator = new ReportGenerator(outAbs, ProjectData.getInstance().getSourceDirAbs());
        reportGenerator.create();
    }
}
