package ant;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import utils.Deserializer;

public class ReportTask extends Task {

    @Override
    public void execute() throws BuildException {
        Deserializer.deserializeCoverageData();

    }
}
