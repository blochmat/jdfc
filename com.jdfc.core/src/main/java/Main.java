import data.singleton.CoverageDataStore;
import org.apache.commons.cli.*;
import report.ReportGenerator;
import utils.Deserializer;
import utils.Instrumenter;

import java.io.File;
import java.util.List;

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

        if(cmd.hasOption("i") ^ cmd.hasOption("r")) {
            if(cmd.hasOption("i")) {
                // Instrument
                parsePathOptions(cmd, false);
                CoverageDataStore.getInstance().saveProjectInfo(workDirAbs, buildDirAbs, classesDirAbs, sourceDirAbs);
                Instrumenter instrumenter = new Instrumenter(workDirAbs, classesDirAbs);
                if(cmd.hasOption("c")) {
                    // Instrument single class
                    String classFileAbs = String.join(File.separator, classesDirAbs, cmd.getOptionValue("c"));
                    instrumenter.instrumentClass(classFileAbs);
                } else {
                    // Instrument all classes from project
                    List<File> classFiles = instrumenter.loadClassFiles();
                    for (File classFile : classFiles) {
                        instrumenter.instrumentClass(classFile.getAbsolutePath());
                    }
                }
            } else {
                // Report
                if(cmd.hasOption("O")) {
                    parsePathOptions(cmd, true);
                    Deserializer.deserializeCoverageData(workDirAbs);
                    ReportGenerator reportGenerator = new ReportGenerator(outputDirAbs, sourceDirAbs);
                    reportGenerator.createHTMLReport();
                } else {
                    System.out.println("Please provide the desired output directory path of the report relative to the project's root e.g. /path/to/report.");
                }
            }
        } else {
            System.out.println("To analyse your project please follow these steps:\n    1. Run jdfc with -i to instrument the source code.\n    2. Execute the tests of your project.\n    3. Run jdfc with -r to create the coverage report.");
            System.exit(1);
        }
    }

    private static void createOptions() {
        options = new Options();

        Option instrument = Option.builder()
                .option("i")
                .longOpt("instrument")
                .desc("When flag is set the classes under test are instrumented.")
                .build();
        options.addOption(instrument);

        Option report = Option.builder()
                .option("r")
                .longOpt("report")
                .desc("When flag is set the coverage report is created from gathered coverage data.")
                .build();
        options.addOption(report);

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

        Option outputDirOption = Option.builder()
                .option("O")
                .longOpt("outputDir")
                .hasArg()
                .desc("relative path to report output directory from workdir root")
                .build();
        options.addOption(outputDirOption);

        Option singleClassOption = Option.builder()
                .option("c")
                .longOpt("class")
                .argName("class")
                .hasArg()
                .desc("Relative path to class to instrument from source directory.")
                .build();
        options.addOption(singleClassOption);
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
