package ant;

import data.singleton.CoverageDataStore;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import report.ReportGenerator;
import utils.Deserializer;

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
        Deserializer.deserializeCoverageData(work);
        String outAbs = CoverageDataStore.getInstance().getWorkDir().getAbsolutePath().concat(out);
        ReportGenerator reportGenerator = new ReportGenerator(outAbs, CoverageDataStore.getInstance().getSourceDirAbs());
        reportGenerator.createHTMLReport();
    }
}
