import data.ProjectData;
import org.apache.commons.cli.*;
import org.junit.Test;
import report.ReportGenerator;
import utils.Deserializer;
import instr.Instrumenter;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;

import static utils.Constants.JDFC_SERIALIZATION_FILE;

public class Main {

    private static Options options;
    private static String workDirAbs;
    private static String buildDirAbs;
    private static String classesDirAbs;
    private static String sourceDirAbs;
    private static String outputDirAbs;

    public static void main(String[] args) {
        createOptions();
        CommandLineParser parser = new DefaultParser();

        // Parse cmd arguments
        CommandLine cmd;
        try {
            cmd = parser.parse( options, args);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
        if(cmd.hasOption("i")) {
            // Instrument
            parsePathOptions(cmd, false);
            ProjectData.getInstance().saveProjectInfo(workDirAbs, buildDirAbs, classesDirAbs, sourceDirAbs);
            Instrumenter instrumenter = new Instrumenter(workDirAbs, classesDirAbs, sourceDirAbs);
            String classFqn = cmd.getOptionValue("i");
            if (classFqn != null) {
                // Instrument single class
                String classFileRel = classFqn.replace(".", File.separator) + ".class";
                String classFileAbs = String.join(File.separator, classesDirAbs, classFileRel);
                instrumenter.instrumentClass(classFileAbs);
            } else {
                // Instrument all classes from project
                List<File> classFiles = instrumenter.loadClassFiles();
                for (File classFile : classFiles) {
                    instrumenter.instrumentClass(classFile.getAbsolutePath());
                }
            }
        }

        if (cmd.hasOption("t")
                && cmd.getOptionValue("i") != null
                && cmd.getOptionValue("t") != null) {
        }

        if (cmd.hasOption("r")) {
            // Report
            String outputDirStr = cmd.getOptionValue("r");
            if(outputDirStr == null) {
                outputDirStr = outputDirAbs;
            }
            System.out.println(outputDirStr);
//            parsePathOptions(cmd, true);
//            String jdfcDir = String.format("%s/%s", buildDirAbs, "jdfc/");
//            String fileInAbs = String.join(File.separator, jdfcDir, JDFC_SERIALIZATION_FILE);
//            ProjectData deserialized = Deserializer.deserializeCoverageData(fileInAbs);
//            if(deserialized == null) {
//                throw new IllegalArgumentException("Unable do deserialize coverage data.");
//            }
//            ProjectData.setInstance(deserialized);
//
//            ReportGenerator reportGenerator = new ReportGenerator(outputDirAbs, sourceDirAbs);
//            reportGenerator.create();
        }
    }

    private static void createOptions() {
        options = new Options();

        Option workDirOption = Option.builder()
                .option("W")
                .longOpt("workDir")
                .hasArg()
                .desc("absolute path to root of project to analyse")
                .build();
        options.addOption(workDirOption);

        Option buildDirOption = Option.builder()
                .option("B")
                .longOpt("buildDir")
                .hasArg()
                .desc("relative path to build directory from workdir root")
                .build();
        options.addOption(buildDirOption);

        Option classesDirOption = Option.builder()
                .option("C")
                .longOpt("classesDir")
                .hasArg()
                .desc("relative path to classes build directory from workdir root")
                .build();
        options.addOption(classesDirOption);

        Option sourceDirOption = Option.builder()
                .option("S")
                .longOpt("sourceDir")
                .hasArg()
                .desc("relative path to source directory from workdir root e.g. src/main/java")
                .build();
        options.addOption(sourceDirOption);

        Option instrument = Option.builder()
                .option("i")
                .longOpt("instrument")
                .argName("instrument")
                .hasArg()
                .optionalArg(true)
                .desc("Instrument class/es.")
                .build();
        options.addOption(instrument);

        Option test = Option.builder()
                .option("t")
                .longOpt("test")
                .argName("test")
                .optionalArg(true)
                .hasArg()
                .desc("Test instrumented class/es.")
                .build();
        options.addOption(test);

        Option report = Option.builder()
                .option("r")
                .longOpt("report")
                .desc("When flag is set the coverage report is created from gathered coverage data.")
                .build();
        options.addOption(report);
    }

    private static void parsePathOptions(CommandLine cmd, boolean parseOutputDir) {
        boolean systemExit = false;
        if(cmd.hasOption("W")) {
            workDirAbs = cmd.getOptionValue("W");
        } else {
            System.out.println("Please pass missing option -W.");
            systemExit = true;
        }

        if(cmd.hasOption("B")) {
            buildDirAbs = String.join(File.separator, workDirAbs, cmd.getOptionValue("B"));
        } else {
            System.out.println("Please pass missing option -B.");
            systemExit = true;
        }

        if(cmd.hasOption("C")) {
            classesDirAbs = String.join(File.separator, workDirAbs, cmd.getOptionValue("C"));
        } else {
            System.out.println("Please pass missing option -C.");
            systemExit = true;
        }

        if(cmd.hasOption("S")) {
            sourceDirAbs = String.join(File.separator, workDirAbs, cmd.getOptionValue("S"));
        } else {
            System.out.println("Please pass missing option -S.");
            systemExit = true;
        }

        if(parseOutputDir) {
            if(cmd.hasOption("O")) {
                outputDirAbs = String.join(File.separator, workDirAbs, cmd.getOptionValue("O"));
            } else {
                outputDirAbs = "";
            }
        }

        if(systemExit) {
            System.exit(1);
        }
    }
}
