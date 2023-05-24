import data.CoverageDataExport;
import data.CoverageDataStore;

import java.io.File;
import java.lang.instrument.Instrumentation;
import java.nio.file.Path;

public final class JDFCAgent {

    /**
     * Workaround: This field is used to avoid NoClassDefFoundException in shutdown hook
     */
    private static final Class<?> export = CoverageDataExport.class;

    public static void premain(final String options, final Instrumentation inst) {
        System.err.println("PREMAIN");
        File dir = new File(options);
        Path baseDir = dir.toPath();
        String fileEnding = ".class";
        CoverageDataStore.getInstance().addNodesFromDirRecursive(dir, CoverageDataStore.getInstance().getRoot(), baseDir, fileEnding);
        JDFCClassTransformer jdfcClassTransformer = new JDFCClassTransformer();
        inst.addTransformer(jdfcClassTransformer);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> CoverageDataStore.getInstance().exportCoverageData()));
    }
}
