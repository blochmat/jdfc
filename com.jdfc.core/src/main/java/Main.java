import org.apache.commons.cli.*;

public class Main {

    private static final String JDFC_INSTRUMENTED = ".jdfc_instrumented";

    public static void main(String[] args) {
        // create Options object
        Options options = new Options();

        // add a option
        options.addOption("a", false, "add two numbers");
        options.addOption("m", false, "multiply two numbers");

        //Create a parser
        CommandLineParser parser = new DefaultParser();

        //parse the options passed as command line arguments
        CommandLine cmd = null;
        try {
            cmd = parser.parse( options, args);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }

        //hasOptions checks if option is present or not
        if(cmd.hasOption("a")) {
            // add the two numbers
            System.out.println();
        } else if(cmd.hasOption("m")) {
            System.out.println("MULTIPLY");
        }

//        JDFCInstrument jdfcInstrument = new JDFCInstrument(workDir, buildDirStr, classesDir, srcDirStr);
    }
}
