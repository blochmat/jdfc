import data.singleton.CoverageDataStore;
import instr.JDFCInstrument;
import lombok.extern.slf4j.Slf4j;
import org.objectweb.asm.ClassReader;
import utils.JDFCUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;

@Slf4j
public class JDFCClassTransformer implements ClassFileTransformer {

    private final JDFCInstrument JDFCInstrument;

    public JDFCClassTransformer() {
        this.JDFCInstrument = new JDFCInstrument();
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
                writer.write(className);
                writer.write("\n");
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        }

        final ClassReader cr = new ClassReader(classfileBuffer);
        return JDFCInstrument.instrument(cr);
    }
}
