import data.io.CoverageDataExport;
import data.singleton.CoverageDataStore;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.JDFCUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public final class JDFCAgent {

    /**
     * Workaround: This field is used to avoid NoClassDefFoundException in shutdown hook
     */
    private static final Class<?> export = CoverageDataExport.class;

    public static void premain(final String agentArgs, final Instrumentation inst) {
        // print uncaught exceptions
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
            System.err.println("Exception occurred in thread: " + t.getName());
            e.printStackTrace();
        });

        long start = System.currentTimeMillis();
        log.info("Instrumentation started.");

        // handle arguments
        List<String> args = Arrays.asList(agentArgs.split(","));
        CoverageDataStore.getInstance().saveProjectInfo(args.get(0), args.get(1), args.get(2), args.subList(3, args.size()));

        // setup package/class tree data structure
        File dir = CoverageDataStore.getInstance().getClassesBuildDir();
        Path classesBuildDir = dir.toPath();
        String fileEnding = ".class";
        CoverageDataStore.getInstance().addNodesFromDirRecursive(dir, CoverageDataStore.getInstance().getRoot(), classesBuildDir, fileEnding);

        // add transformer for classes with tests
        JDFCClassTransformer jdfcClassTransformer = new JDFCClassTransformer();
        inst.addTransformer(jdfcClassTransformer);

        // add shutdown hook to compute and write results
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            Logger logger = LoggerFactory.getLogger("Shutdown Hook");
            try {
                long end = System.currentTimeMillis();
                Duration duration = Duration.ofMillis(end - start);
                long hours = duration.toHours();
                duration = duration.minusHours(hours);
                long minutes = duration.toMinutes();
                duration = duration.minusMinutes(minutes);
                long seconds = duration.getSeconds();
                duration = duration.minusSeconds(seconds);
                long millis = duration.toMillis();

                String time = String.format("%02d:%02d:%02d.%03d", hours, minutes, seconds, millis);
                logger.info(String.format("Instrumentation and Testing finished. Time: %s", time));
                CoverageDataStore.getInstance().exportCoverageData();
            } catch (Exception e) {
                File jdfcDir = CoverageDataStore.getInstance().getJdfcDir();
                if (jdfcDir.exists() || jdfcDir.mkdirs()) {
                    try (FileWriter writer = new FileWriter(JDFCUtils.createFileInJDFCDir("shutdown_error.txt"), false)) {
                        String stackTrace = Arrays.stream(e.getStackTrace()).map(StackTraceElement::toString).collect(Collectors.joining("\n"));
                        writer.write("Exception during shutdown: " + e.getMessage() + "\n");
                        writer.write(stackTrace);
                        writer.write("\n");
                    } catch (IOException ioException) {
                        ioException.printStackTrace();  // print to console as a last resort
                    }
                }
            }
        }));
    }
}
