package instr;

import data.ClassExecutionData;
import data.CoverageDataStore;
import cfg.CFGCreator;
import instr.classVisitors.InstrumentationClassVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

public class JDFCInstrument {

    public byte[] instrument(final ClassReader classReader) {

        final ClassNode classNode = new ClassNode();
        final ClassWriter cw = new ClassWriter(classReader, 0);
        classReader.accept(classNode, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        ClassExecutionData classExecutionData =
                (ClassExecutionData) CoverageDataStore.getInstance().findClassDataNode(classNode.name).getData();

        if (classExecutionData != null) {
            CFGCreator.createICFGsForClass(classReader, classNode, classExecutionData);
            final ClassVisitor cv = new InstrumentationClassVisitor(cw, classNode, classExecutionData);
            classReader.accept(cv, 0);
        }
//
//        final TraceClassVisitor tcv = new TraceClassVisitor(cw, new PrintWriter(System.out));
//        classReader.accept(tcv, 0);
        return cw.toByteArray();
    }
}
