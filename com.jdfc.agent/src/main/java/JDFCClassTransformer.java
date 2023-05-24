import data.CoverageDataStore;
import instr.JDFCInstrument;
import org.objectweb.asm.ClassReader;

import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;


public class JDFCClassTransformer implements ClassFileTransformer {

    private final JDFCInstrument JDFCInstrument;

    public JDFCClassTransformer() {
        this.JDFCInstrument = new JDFCInstrument();
    }

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) {
        if (!CoverageDataStore.getInstance().getClassList().contains(className)) {
            return classfileBuffer;
        }
        System.err.println("TRANSFORM");
        final ClassReader cr = new ClassReader(classfileBuffer);
        return JDFCInstrument.instrument(cr);
    }
}
