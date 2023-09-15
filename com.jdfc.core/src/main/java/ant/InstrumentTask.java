package ant;

import data.singleton.CoverageDataStore;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.FileSet;
import instr.Instrumenter;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class InstrumentTask extends Task {

    private List<FileSet> filesets = new ArrayList<>();

    private String work;

    private String classes;

    private String src;

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

    @Override
    public void execute() {
        String workDirAbs = work;
        String buildDirAbs = String.format("%s%starget", workDirAbs, File.separator);
        String classesDirAbs = String.join(File.separator, workDirAbs, classes);
        String sourceDirAbs = String.join(File.separator, workDirAbs, src);
        CoverageDataStore.getInstance().saveProjectInfo(workDirAbs, buildDirAbs, classesDirAbs, sourceDirAbs);
        Instrumenter instrumenter = new Instrumenter(workDirAbs, classesDirAbs, sourceDirAbs);
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
