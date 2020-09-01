package com.jdfc.agent.rt.internal;

import com.jdfc.core.analysis.CoverageDataStore;
import com.jdfc.core.analysis.instr.Instrument;
import org.objectweb.asm.ClassReader;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;


public class ClassTransformer implements ClassFileTransformer {

    private final Instrument instrument;

    public ClassTransformer() {
        this.instrument = new Instrument();
    }

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
        if (!CoverageDataStore.INSTANCE.getClassList().contains(className)) {
            return classfileBuffer;
        }
        final ClassReader cr = new ClassReader(classfileBuffer);
        return instrument.instrument(cr);
    }
}
