import data.singleton.CoverageDataStore;
import org.apache.commons.cli.*;
import utils.Instrumenter;

import java.io.File;
import java.util.List;

public class Main {


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
    }

}
