package com.jdfc.agent.rt.internal;

import com.jdfc.core.analysis.CFGStorage;
import com.jdfc.core.analysis.cfg.CFG;
import com.jdfc.core.analysis.cfg.CFGCreator;
import com.jdfc.core.analysis.instr.Instrument;
import com.jdfc.core.analysis.internal.instr.ClassInstrument;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;


public class ClassTransformer implements ClassFileTransformer {

    private final Instrument instrument;

    public ClassTransformer() {
        this.instrument = new Instrument();
        // Class names will be reported in VM notation:
//        includes = new WildcardMatcher(toVMName(options.getIncludes()));
//        excludes = new WildcardMatcher(toVMName(options.getExcludes()));
//        exclClassloader = new WildcardMatcher(options.getExclClassloader());
//        classFileDumper = new ClassFileDumper(options.getClassDumpDir());
//        inclBootstrapClasses = options.getInclBootstrapClasses();
//        inclNoLocationClasses = options.getInclNoLocationClasses();
    }

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
        if (!className.equals("BranchInteger")) {
            return classfileBuffer;
        }

        final ClassReader cr = new ClassReader(classfileBuffer);
        return instrument.instrument(cr);
    }
}
