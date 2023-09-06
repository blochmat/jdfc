import lombok.extern.slf4j.Slf4j;
import utils.Instrumenter;

import java.lang.instrument.ClassFileTransformer;
import java.net.URL;
import java.security.CodeSource;
import java.security.ProtectionDomain;

@Slf4j
public class JDFCClassTransformer implements ClassFileTransformer {

    private final String workDirAbs;
    private final String classesDirAbs;

    public JDFCClassTransformer(String workDirAbs, String classesDirAbs) {
        this.workDirAbs = workDirAbs;
        this.classesDirAbs = classesDirAbs;
    }

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) {
        String classFileAbs = this.getAbsolutePath(classBeingRedefined);
        if(classFileAbs == null) {
            throw new RuntimeException("ERROR: Absolute path of " + className + " could not be retrieved. Code source is null.");
        }

        if(classFileAbs.startsWith(classesDirAbs)) {
            Instrumenter instrumenter = new Instrumenter(workDirAbs, classesDirAbs);
            return instrumenter.instrumentClass(classfileBuffer, classFileAbs);
        } else {
            return classfileBuffer;
        }

    }

    private String getAbsolutePath(Class<?> classBeingRedefined) {
        CodeSource codeSource = classBeingRedefined.getProtectionDomain().getCodeSource();
        if(codeSource != null) {
            URL resource = codeSource.getLocation();
            return resource.getPath();
        } else {
            return null;
        }
    }
}
