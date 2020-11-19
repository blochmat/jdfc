package com.jdfc.core.analysis;

import com.jdfc.core.analysis.data.CoverageDataStore;
import com.jdfc.core.analysis.data.ClassExecutionData;
import com.jdfc.core.analysis.ifg.CFGCreator;
import com.jdfc.core.analysis.instr.InstrumentationClassVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.util.TraceClassVisitor;

import java.io.PrintWriter;

public class JDFCInstrument {

    public byte[] instrument(final ClassReader classReader) {
        final ClassNode classNode = new ClassNode();
        final ClassWriter cw = new ClassWriter(classReader, 0);
        classReader.accept(classNode, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);

//        if(classNode.name.contains("Real")) {
//        final TraceClassVisitor tcv = new TraceClassVisitor(cw, new PrintWriter(System.out));
//        classReader.accept(tcv, 0);
//        return cw.toByteArray();
//        }

        ClassExecutionData classExecutionData =
                (ClassExecutionData) CoverageDataStore.getInstance().findClassDataNode(classNode.name).getData();

        if (classExecutionData != null) {
            CFGCreator.createCFGsForClass(classReader, classNode, classExecutionData);
            final ClassVisitor cv = new InstrumentationClassVisitor(cw, classNode, classExecutionData);
            classReader.accept(cv, 0);
        }
        return cw.toByteArray();
    }
}
