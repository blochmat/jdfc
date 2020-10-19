package com.jdfc.core.analysis;

import com.jdfc.core.analysis.data.CoverageDataStore;
import com.jdfc.core.analysis.data.ClassExecutionData;
import com.jdfc.core.analysis.ifg.CFGCreator;
import com.jdfc.core.analysis.instr.InstrumentationClassVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

import java.io.PrintWriter;

public class JDFCInstrument {

    public byte[] instrument(final ClassReader classReader) {
        final ClassNode classNode = new ClassNode();
        classReader.accept(classNode, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);

        ClassExecutionData classExecutionData =
                (ClassExecutionData) CoverageDataStore.getInstance().findClassDataNode(classNode.name).getData();

        CFGCreator.createCFGsForClass(classReader, classNode, classExecutionData);

        final ClassWriter cw = new ClassWriter(classReader, ClassWriter.COMPUTE_FRAMES);

        //TODO: Remove Debug
//        if(classNode.name.equals("BranchingInteger")) {
//            final TraceClassVisitor tcv = new TraceClassVisitor(cw, new PrintWriter(System.out));
//            final ClassVisitor cv = new InstrumentationClassVisitor(tcv, classNode);
//            classReader.accept(cv, 0);
//        } else {
        final ClassVisitor cv = new InstrumentationClassVisitor(cw, classNode);
        classReader.accept(cv, 0);
//        }
        return cw.toByteArray();
    }
}
