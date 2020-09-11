package com.jdfc.core.analysis.instr;

import com.jdfc.core.analysis.CoverageDataStore;
import com.jdfc.core.analysis.cfg.CFG;
import com.jdfc.core.analysis.cfg.CFGCreator;
import com.jdfc.core.analysis.internal.instr.tree.TreeInstrument;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.util.TraceClassVisitor;

import java.io.PrintWriter;
import java.util.Map;


public class ClassInstrument {

    public ClassInstrument() {
    }

    public byte[] instrument(final ClassReader classReader) {
        final ClassNode classNode = new ClassNode();
        classReader.accept(classNode, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        final Map<String, CFG> methodCFGs = CFGCreator.createCFGsForClass(classReader, classNode);
        // Create tree entry, insert method cfgs in correct class node
        CoverageDataStore.getInstance().setupClassDataNode(classReader.getClassName(), methodCFGs);

        final ClassWriter cw = new ClassWriter(classReader, ClassWriter.COMPUTE_FRAMES);
        final ClassVisitor cv = new MyClassVisitor(cw, classNode);
        classReader.accept(cv, 0);
        return cw.toByteArray();

//        return debug(classReader);
    }

    private byte[] debug(ClassReader classReader){
        final ClassWriter classWriter = new ClassWriter(classReader, ClassWriter.COMPUTE_FRAMES);
        final PrintWriter printWriter = new PrintWriter(System.out);
        final TraceClassVisitor tcv = new TraceClassVisitor(classWriter, printWriter);
        classReader.accept(tcv,0);
        return classWriter.toByteArray();
    }
}
