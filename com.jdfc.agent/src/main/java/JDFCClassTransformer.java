import instr.Instrumenter;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import utils.JDFCUtils;

import java.io.File;
import java.lang.instrument.ClassFileTransformer;
import java.net.URL;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.Objects;

@Slf4j
@AllArgsConstructor
public class JDFCClassTransformer implements ClassFileTransformer {

    private final String workDirAbs;
    private final String classesDirRel;
    private final String sourceDirRel;
    private final boolean isInterProcedural;

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) {
        String classesDirAbs = String.format("%s%s%s", workDirAbs, File.separator, classesDirRel);
        String sourceDirAbs = String.format("%s%s%s", workDirAbs, File.separator, sourceDirRel);
        boolean isInstrumentationRequired = this.getIsInstrumentationRequired(protectionDomain, classesDirAbs);
        if (isInstrumentationRequired) {
            String classFileAbs = String.format("%s%s%s%s", classesDirAbs, File.separator, className, ".class");
            JDFCUtils.logThis(classFileAbs, "transform");
            Instrumenter instrumenter = new Instrumenter(workDirAbs, classesDirAbs, sourceDirAbs, isInterProcedural);
            return instrumenter.instrumentClass(classfileBuffer, classFileAbs);
        }
        return classfileBuffer;

    }

    private boolean getIsInstrumentationRequired(ProtectionDomain protectionDomain, String classesDirAbs) {
        String location = this.getCodeSourceLocation(protectionDomain);
        String compare = classesDirAbs + File.separator;
        return location != null && Objects.equals(location, compare);
    }

    private String getCodeSourceLocation(ProtectionDomain protectionDomain) {
        if (protectionDomain != null) {
            CodeSource codeSource = protectionDomain.getCodeSource();
            if (codeSource != null) {
                URL location = codeSource.getLocation();
                if (location != null) {
                    return location.getPath();
                }
            }
        }
        return null;
    }
}
