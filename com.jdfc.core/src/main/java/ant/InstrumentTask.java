package ant;

import data.ProjectData;
import lombok.extern.slf4j.Slf4j;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.FileSet;
import instr.Instrumenter;
import utils.JDFCUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Slf4j
public class InstrumentTask extends Task {

    private final List<FileSet> filesets = new ArrayList<>();

    private String work;

    private String classes;

    private String src;

    private String scope;

    public void addFileset(FileSet fileset) {
        filesets.add(fileset);
    }

    public void setWork(String work) {
        this.work = work;
    }

    public void setClasses(String classes) {
        this.classes = classes;
    }

    public void setSrc(String src) {
        this.src = src;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }


    @Override
    public void execute() {
        log.info(this.work);
        log.info(this.classes);
        log.info(this.src);
        log.info(this.scope);
        boolean isInterProcedural = Objects.equals(this.scope, "inter");
        String workDirAbs = work;
        String buildDirAbs = String.format("%s%starget", workDirAbs, File.separator);
        String classesDirAbs = String.join(File.separator, workDirAbs, classes);
        String sourceDirAbs = String.join(File.separator, workDirAbs, src);
        ProjectData.getInstance().saveProjectInfo(workDirAbs, buildDirAbs, classesDirAbs, sourceDirAbs);
        Instrumenter instrumenter = new Instrumenter(workDirAbs, classesDirAbs, sourceDirAbs, isInterProcedural);
        // Print class path
        System.err.println(System.getProperty("java.class.path").replace(":", "\n"));
        for (FileSet fs : filesets) {
            DirectoryScanner ds = fs.getDirectoryScanner(getProject());
            for (String includedFile : ds.getIncludedFiles()) {
                String classFileAbs = String.join(File.separator, classesDirAbs, includedFile);
                instrumenter.instrumentClass(classFileAbs);
            }
        }
    }

    private void logToFile(String text) {
        try {
            java.io.FileWriter writer = new java.io.FileWriter("instrument_task.log", true);
            writer.write(text + "\n");
            writer.close();
        } catch (java.io.IOException e) {
            log("Error writing to log: " + e.getMessage());
        }
    }
}
