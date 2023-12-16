import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import instr.Instrumenter;

import java.lang.instrument.ClassFileTransformer;
import java.net.URL;
import java.security.CodeSource;
import java.security.ProtectionDomain;

@Slf4j
@AllArgsConstructor
public class JDFCClassTransformer implements ClassFileTransformer {

    private final String workDirAbs;
    private final String classesDirAbs;
    private final String sourceDirAbs;
    private final boolean isInterProcedural;

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) {
        String classFileAbs = this.getAbsolutePath(classBeingRedefined);
        if(classFileAbs == null) {
            throw new RuntimeException("ERROR: Absolute path of " + className + " could not be retrieved. Code source is null.");
        }

        if(classFileAbs.startsWith(classesDirAbs)) {
            Instrumenter instrumenter = new Instrumenter(workDirAbs, classesDirAbs, sourceDirAbs, isInterProcedural);
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
