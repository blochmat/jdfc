import data.CoverageDataStore;
import instr.JDFCInstrument;
import org.objectweb.asm.ClassReader;

import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;


public class ClassTransformer implements ClassFileTransformer {

    private final instr.JDFCInstrument JDFCInstrument;

    public ClassTransformer() {
        this.JDFCInstrument = new JDFCInstrument();
    }

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) {
        if (!CoverageDataStore.getInstance().getClassList().contains(className)) {
            return classfileBuffer;
        }
        final ClassReader cr = new ClassReader(classfileBuffer);
        return JDFCInstrument.instrument(cr);
    }
}
