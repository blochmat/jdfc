package com.jdfc.agent.rt.internal;

import com.jdfc.core.analysis.CoverageDataStore;
import com.jdfc.core.analysis.instr.ClassInstrument;
import org.objectweb.asm.ClassReader;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.Arrays;


public class ClassTransformer implements ClassFileTransformer {

    private final ClassInstrument classInstrument;

    public ClassTransformer() {
        this.classInstrument = new ClassInstrument();
    }

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
        if (!CoverageDataStore.getInstance().getClassList().contains(className)) {
            return classfileBuffer;
        }
        final ClassReader cr = new ClassReader(classfileBuffer);
        return classInstrument.instrument(cr);
    }
}
