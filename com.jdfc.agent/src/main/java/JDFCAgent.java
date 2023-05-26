import data.CoverageDataExport;
import data.CoverageDataStore;

import java.io.File;
import java.lang.instrument.Instrumentation;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

public final class JDFCAgent {

    /**
     * Workaround: This field is used to avoid NoClassDefFoundException in shutdown hook
     */
    private static final Class<?> export = CoverageDataExport.class;

    public static void premain(final String agentArgs, final Instrumentation inst) {
        // handle arguments
        List<String> args = Arrays.asList(agentArgs.split(","));
        CoverageDataStore.getInstance().saveProjectInfo(args.get(0), args.get(1), args.get(2), args.subList(3, args.size()));

        // setup package/class tree data structure
        File dir = new File(CoverageDataStore.getInstance().getClassesBuildDirStr());
        Path classesBuildDir = dir.toPath();
        String fileEnding = ".class";
        CoverageDataStore.getInstance().addNodesFromDirRecursive(dir, CoverageDataStore.getInstance().getRoot(), classesBuildDir, fileEnding);

        // add transformer for classes with tests
        JDFCClassTransformer jdfcClassTransformer = new JDFCClassTransformer();
        inst.addTransformer(jdfcClassTransformer);

        // add shutdown hook to compute and write results
        Runtime.getRuntime().addShutdownHook(new Thread(() -> CoverageDataStore.getInstance().exportCoverageData()));
    }
}
