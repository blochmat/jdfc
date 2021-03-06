package com.jdfc.core.analysis.instr;

import com.jdfc.core.analysis.CoverageDataStore;
import com.jdfc.core.analysis.cfg.CFG;
import com.jdfc.core.analysis.cfg.CFGCreator;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

import java.util.Map;


public class ClassInstrument {

    public byte[] instrument(final ClassReader classReader) {
        final ClassNode classNode = new ClassNode();
        classReader.accept(classNode, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);

        final Map<String, CFG> methodCFGs = CFGCreator.createCFGsForClass(classReader, classNode);
        CoverageDataStore.getInstance().setupClassDataNode(classReader.getClassName(), methodCFGs);
        final ClassWriter cw = new ClassWriter(classReader, ClassWriter.COMPUTE_FRAMES);
        final ClassVisitor cv = new MyClassVisitor(cw, classNode);
        classReader.accept(cv, 0);
        return cw.toByteArray();
    }
}
