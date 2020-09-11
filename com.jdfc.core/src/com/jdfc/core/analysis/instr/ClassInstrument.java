package com.jdfc.core.analysis.instr;

import com.jdfc.core.analysis.CoverageDataStore;
import com.jdfc.core.analysis.cfg.CFG;
import com.jdfc.core.analysis.cfg.CFGCreator;
import com.jdfc.core.analysis.internal.instr.tree.TreeInstrument;
import org.objectweb.asm.ClassReader;
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
        final Map<String, CFG> methodCFGs = CFGCreator.createCFGsForClass(classReader, classNode);

        // Create tree entry, insert method cfgs in correct class node
        CoverageDataStore.getInstance().setupClassDataNode(classReader.getClassName(), methodCFGs);

        // Instrument the definitions and uses of variables
        final TreeInstrument instrument = new TreeInstrument(classNode);
        instrument.instrument();
        final ClassWriter classWriter = new ClassWriter(classReader, ClassWriter.COMPUTE_FRAMES);
        final PrintWriter printWriter = new PrintWriter(System.out);
        final TraceClassVisitor tcv = new TraceClassVisitor(classWriter, printWriter);
        classNode.accept(tcv);
        return classWriter.toByteArray();
    }
}
