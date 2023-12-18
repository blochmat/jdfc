package ant;

import data.ProjectData;
import lombok.extern.slf4j.Slf4j;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import report.ReportGenerator;
import utils.Deserializer;
import utils.JDFCUtils;

import java.io.File;
import java.util.Objects;

@Slf4j
public class ReportTask extends Task {

    private String work;
    private String out;
    private boolean xml;
    private boolean html;

    public void setOut(String out) {
        this.out = out;
    }

    public void setWork(String work) {
        this.work = work;
    }

    public void setXml(String xml) {
        this.xml = Objects.equals(xml, "true");
    }

    public void setHtml(String html) {
        this.html = Objects.equals(html, "true");
    }

    @Override
    public void execute() throws BuildException {
        log(JDFCUtils.getJDFCSerFileAbs());
        ProjectData deserialized = Deserializer.deserializeCoverageData(JDFCUtils.getJDFCSerFileAbs());
        if(deserialized == null) {
            String msg = String.format("Unable do deserialize coverage data from %s", JDFCUtils.getJDFCSerFileAbs());
            throw new IllegalArgumentException(msg);
        }
        ProjectData.getInstance().fetchDataFrom(deserialized);
        String outAbs = String.join(File.separator, ProjectData.getInstance().getWorkDir().getAbsolutePath(), out);

        ReportGenerator reportGenerator = new ReportGenerator(outAbs, ProjectData.getInstance().getSourceDirRel(), xml, html);
        reportGenerator.create();
    }
}
