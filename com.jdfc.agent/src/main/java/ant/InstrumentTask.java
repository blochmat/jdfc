package ant;

import org.apache.tools.ant.Task;

import java.io.File;

public class InstrumentTask extends Task {

    private String workDir;

    private String classesDir;

    private String instrument;

    public void setWorkDir(String workDir) {
        this.workDir = workDir;
    }

    public void setClassesDir(String classesDir) {
        this.classesDir = classesDir;
    }

    public void setInstrument(String instrument) {
        this.instrument = instrument;
    }

    @Override
    public void execute() {
        log("InstrumentTask is about to execute.");
        log("projectPath: " + getProject().getName());
        log("workDir: " + workDir);
        log("classFilePath: " + classesDir);
        log("instrument: " + instrument);
        String buildDirStr = String.format("%s%starget", workDir, File.separator);

        // We need to instrument a single class without breaking stuff

//        String[] classes = instrument.split(",");
//
//        for(String c : classes) {
//            if (c.contains("$")) {
//                continue;
//            }
//
//            String path = String.format("%s%s%s%s%s", workDir, File.separator, classesDir, File.separator, c);
//            log(path);
//
//            byte[] classFileBuffer;
//            try {
//                classFileBuffer = Files.readAllBytes(Paths.get(path));
//                ClassReader cr = new ClassReader(classFileBuffer);
//                String dirPath = String.format("%s%sjdfc_instrumented", workDir, File.separator);
//                File dir =  new File(dirPath);
//                if (!dir.exists()) {
//                    dir.mkdirs();
//                }
//
//                String filePath = dirPath + File.separator + c;
//
//                try (FileOutputStream fos = new FileOutputStream(filePath)) {
//                    fos.write(instr);
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            } catch (IOException e) {
//                throw new RuntimeException(e);
//            }
//        }
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
