import data.singleton.CoverageDataStore;
import lombok.extern.slf4j.Slf4j;
import utils.JDFCUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.net.URL;
import java.security.CodeSource;
import java.security.ProtectionDomain;

@Slf4j
public class JDFCClassTransformer implements ClassFileTransformer {

//    private final JDFCInstrument JDFCInstrument;

    public JDFCClassTransformer(String workDirAbs,
                                String buildDirAbs,
                                String classesDirAbs,
                                String sourceDirAbs) {
//        CoverageDataStore.getInstance().saveProjectInfo(workDirAbs, buildDirAbs, classesDirAbs, sourceDirAbs);
//        this.JDFCInstrument = new JDFCInstrument(classesDirAbs, "");
    }

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) {
        if (!CoverageDataStore.getInstance().getUntestedClassList().contains(className)) {
            return classfileBuffer;
        }

        if(log.isDebugEnabled()) {
            // Log all relative paths of files in the classpath
            File transformFile = JDFCUtils.createFileInDebugDir("2_transform.txt", false);
            try (FileWriter writer = new FileWriter(transformFile, true)) {
                CodeSource codeSource = classBeingRedefined.getProtectionDomain().getCodeSource();
                if(codeSource != null) {
                    URL resource = codeSource.getLocation();
                    writer.write(resource.getPath());
                }
                writer.write(JDFCUtils.prettyPrintArray(CoverageDataStore.getInstance().getUntestedClassList().toArray(new String[0])));
                writer.write(className);
                writer.write("\n");
                writer.write("\n");
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        }
        return classfileBuffer;

//        final ClassReader cr = new ClassReader(classfileBuffer);
//        return JDFCInstrument.instrument(cr);
    }
}
