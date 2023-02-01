import data.CoverageDataExport;
import data.CoverageDataStore;

import java.io.File;
import java.lang.instrument.Instrumentation;
import java.nio.file.Path;

public final class Agent {

    /**
     * Workaround: This field is used to avoid NoClassDefFoundException in shutdown hook
     */
    private static final Class<?> export = CoverageDataExport.class;

    public static void premain(final String options, final Instrumentation inst) {
        System.err.println("[DEBUG] premainOptions =  "+options);
        File dir = new File(options);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        Path baseDir = dir.toPath();
        System.err.println("[DEBUG] " + baseDir);
        String fileEnding = ".class";
        System.err.println("[DEBUG] c");
        CoverageDataStore.getInstance().addNodesFromDirRecursive(dir, CoverageDataStore.getInstance().getRoot(), baseDir, fileEnding);
        System.err.println("[DEBUG] abababab");
        ClassTransformer a = new ClassTransformer();
        System.err.println("[DEBUG] classTransformer =  " + a);
        inst.addTransformer(a);
        System.err.println("[DEBUG] e");
        Runtime.getRuntime().addShutdownHook(new Thread(() -> CoverageDataStore.getInstance().exportCoverageData()));
    }
}
