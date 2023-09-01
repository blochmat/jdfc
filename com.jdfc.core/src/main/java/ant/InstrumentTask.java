package ant;

import instr.JDFCInstrument;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.FileSet;
import org.objectweb.asm.ClassReader;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
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

    private static final String JDFC_INSTRUMENTED = ".jdfc_instrumented";

    @Override
    public void execute() {
        String workDirAbs = work;
        String buildDirAbs = String.format("%s%starget", workDirAbs, File.separator);
        String classesDirAbs = String.join(File.separator, workDirAbs, classes);
        String sourceDirAbs = String.join(File.separator, workDirAbs, src);

        log(workDirAbs);
        log(buildDirAbs);
        log(classesDirAbs);
        log(sourceDirAbs);

        // TODO
        for (FileSet fs : filesets) {
            log("fs dir: " + fs.getDir());
            DirectoryScanner ds = fs.getDirectoryScanner(getProject());
            for (String includedFile : ds.getIncludedFiles()) {
                log("include file: " + includedFile);
                String classFilePath = String.join(File.separator, classesDirAbs, includedFile);
                File classFile = new File(classFilePath);

                String packagePath = classFile.getAbsolutePath().replace(classesDirAbs, "").replace(classFile.getName(), "");
                File outDir = new File(String.join(File.separator, JDFC_INSTRUMENTED, packagePath));
                if(!outDir.exists()) {
                    outDir.mkdirs();
                }
                String outPath = String.join(File.separator, outDir.getAbsolutePath(), classFile.getName());
                try (FileOutputStream fos = new FileOutputStream(outPath)){
                    byte[] classFileBuffer = Files.readAllBytes(classFile.toPath());
                    ClassReader cr = new ClassReader(classFileBuffer);
                    JDFCInstrument jdfcInstrument = new JDFCInstrument(workDirAbs, buildDirAbs, classesDirAbs, sourceDirAbs);
                    byte[] instrumented = jdfcInstrument.instrument(cr);
                    fos.write(instrumented);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
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
