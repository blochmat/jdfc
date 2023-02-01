import data.CoverageDataStore;
import instr.JDFCInstrument;
import org.objectweb.asm.ClassReader;

import java.io.File;
import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;


public class ClassTransformer implements ClassFileTransformer {

    private final instr.JDFCInstrument JDFCInstrument;

    public ClassTransformer() {
        System.err.println("[DEBUG] classTransformer created");
        this.JDFCInstrument = new JDFCInstrument();
    }

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) {
        if (className.contains("BranchingInteger")) {
            System.err.println("[DEBUG] original: " + className);
            for(String n : CoverageDataStore.getInstance().getClassList()) {
                if (n.contains("BranchingInteger")) {
                    System.err.println("[DEBUG] CovStore class list: " + n);
                }
            }
        }
        if (className.contains("BranchingInteger")) {
            className = className.replace(File.separator, "/");
            System.err.println("[DEBUG] replaced: " + className);
        }
        if (!CoverageDataStore.getInstance().getClassList().contains(className)) {
            return classfileBuffer;
        }
        System.err.println("[DEBUG] transform 2 " + className);
        final ClassReader cr = new ClassReader(classfileBuffer);
        return JDFCInstrument.instrument(cr);
    }
}
