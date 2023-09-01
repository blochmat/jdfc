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

    private String workDir;

    private String classesDir;

    private String srcDir;

    public void addFileset(FileSet fileset) {
        filesets.add(fileset);
    }

    public void setWorkDir(String workDir) {
        this.workDir = workDir;
    }

    public void setClassesDir(String classesDir) {
        this.classesDir = classesDir;
    }

    public void setSrcDir(String srcDir) {
        this.srcDir = srcDir;
    }

    private static final String JDFC_INSTRUMENTED = ".jdfc_instrumented";

    @Override
    public void execute() {
        log("InstrumentTask is about to execute.");
        log("projectPath: " + getProject().getName());
        log("workDir: " + workDir);
        log("src: " + srcDir);
        String srcDirStr = String.format("%s%s%s", workDir, File.separator, srcDir);
        log("srcDirStr: " + srcDirStr);

        String buildDirStr = String.format("%s%starget", workDir, File.separator);
        log("classFilePath: " + classesDir);

        // TODO
        JDFCInstrument jdfcInstrument = new JDFCInstrument(workDir, buildDirStr, classesDir, srcDirStr);
        for (FileSet fs : filesets) {
            System.out.println(fs.getDir());
            DirectoryScanner ds = fs.getDirectoryScanner(getProject());
            for (String includedFile : ds.getIncludedFiles()) {
                System.out.println(includedFile);
                String classFilePath = String.format("%s%s%s%s%s", workDir, File.separator, classesDir, File.separator, includedFile);
                File classFile = new File(classFilePath);

                String packagePath = classFile.getAbsolutePath().replace(workDir, "").replace(classFile.getName(), "");
                File outDir = new File(String.format("%s%s%s", JDFC_INSTRUMENTED, File.separator, packagePath));
                if(!outDir.exists()) {
                    outDir.mkdirs();
                }
                String outPath = String.format("%s%s%s", outDir.getAbsolutePath(), File.separator, classFile.getName());
                try (FileOutputStream fos = new FileOutputStream(outPath)){
                    byte[] classFileBuffer = Files.readAllBytes(classFile.toPath());
                    ClassReader cr = new ClassReader(classFileBuffer);
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
