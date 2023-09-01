import instr.JDFCInstrument;
import org.apache.commons.cli.*;
import org.objectweb.asm.ClassReader;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

public class Main {

    private static final String JDFC_INSTRUMENTED = ".jdfc_instrumented";

    public static void main(String[] args) {
        // create Options object
        Options options = new Options();

        Option workDirOption = Option.builder()
                .option("W")
                .longOpt("workDir")
                .required()
                .hasArg()
                .desc("absolute path to root of project to analyse")
                .build();
        options.addOption(workDirOption);

        Option buildDirOption = Option.builder()
                .option("B")
                .longOpt("buildDir")
                .required()
                .hasArg()
                .desc("relative path to build directory from workdir root")
                .build();
        options.addOption(buildDirOption);

        Option classesDirOption = Option.builder()
                .option("C")
                .longOpt("classesDir")
                .required()
                .hasArg()
                .desc("relative path to classes build directory from workdir root")
                .build();
        options.addOption(classesDirOption);

        Option sourceDirOption = Option.builder()
                .option("S")
                .longOpt("sourceDir")
                .required()
                .hasArg()
                .desc("relative path to source directory from workdir root e.g. src/main/java")
                .build();
        options.addOption(sourceDirOption);

        Option singleClassOption = Option.builder()
                .option("c")
                .longOpt("class")
                .argName("class")
                .hasArg()
                .desc("Relative path to class to instrument from source directory.")
                .build();
        options.addOption(singleClassOption);

        //Create a parser
        CommandLineParser parser = new DefaultParser();

        //parse the options passed as command line arguments
        CommandLine cmd;
        try {
            cmd = parser.parse( options, args);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }

        String workDirAbs = cmd.getOptionValue("W");
        String buildDirAbs = String.join(File.separator, workDirAbs, cmd.getOptionValue("B"));
        String classesDirAbs = String.join(File.separator, workDirAbs, cmd.getOptionValue("C"));
        String sourceDirAbs = String.join(File.separator, workDirAbs, cmd.getOptionValue("S"));

        System.out.println(workDirAbs);
        System.out.println(buildDirAbs);
        System.out.println(classesDirAbs);
        System.out.println(sourceDirAbs);

        if(cmd.hasOption("c")) {
            // Instrument single class
            String classFilePath = String.join(File.separator, workDirAbs, sourceDirAbs, cmd.getOptionValue("c"));
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
        } else {
            // load class files from build dir
            List<File> classFiles = new ArrayList<>();
            try {
                Files.walkFileTree(Paths.get(classesDirAbs), EnumSet.noneOf(FileVisitOption.class), Integer.MAX_VALUE, new FileVisitor<Path>() {

                    @Override
                    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        if (file.toString().endsWith(".class")) {
                            classFiles.add(file.toFile());
                        }
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                        return FileVisitResult.CONTINUE;
                    }
                });
            } catch (IOException e) {
                e.printStackTrace();
            }

            // Instrumenting and saving classes
            for (File classFile : classFiles) {
                String packagePath = classFile.getAbsolutePath().replace(classesDirAbs, "").replace(classFile.getName(), "");
                File out = new File(String.format("%s%s%s", JDFC_INSTRUMENTED, File.separator, packagePath));
                if(!out.exists()) {
                    out.mkdirs();
                }

                String outFilePath = String.format("%s%s%s", out.getAbsolutePath(), File.separator, classFile.getName());
                try (FileOutputStream fos = new FileOutputStream(outFilePath)){
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
}
