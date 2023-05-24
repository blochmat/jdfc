import data.CoverageDataStore;
import instr.JDFCInstrument;
import org.objectweb.asm.ClassReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;


public class JDFCClassTransformer implements ClassFileTransformer {

    private final Logger logger = LoggerFactory.getLogger(JDFCClassTransformer.class);
    private final JDFCInstrument JDFCInstrument;

    public JDFCClassTransformer() {
        this.JDFCInstrument = new JDFCInstrument();
    }

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) {
        if (!CoverageDataStore.getInstance().getClassList().contains(className)) {
            return classfileBuffer;
        }
        logger.debug("transform");
        final ClassReader cr = new ClassReader(classfileBuffer);
        return JDFCInstrument.instrument(cr);
    }
}
