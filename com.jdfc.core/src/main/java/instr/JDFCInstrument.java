package instr;

import data.ClassExecutionData;
import data.CoverageDataStore;
import ifg.CFGCreator;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

public class JDFCInstrument {

    public byte[] instrument(final ClassReader classReader) {

        final ClassNode classNode = new ClassNode();
        final ClassWriter cw = new ClassWriter(classReader, 0);
        classReader.accept(classNode, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        System.err.println("[DEBUG] instrument " + classNode.name);
        ClassExecutionData classExecutionData =
                (ClassExecutionData) CoverageDataStore.getInstance().findClassDataNode(classNode.name).getData();

        System.err.println("[DEBUG] instrument " + classNode.name);
        if (classExecutionData != null) {
            CFGCreator.createCFGsForClass(classReader, classNode, classExecutionData);
            System.err.println("[DEBUG] cfg " + classNode.name);
            final ClassVisitor cv = new InstrumentationClassVisitor(cw, classNode, classExecutionData);
            classReader.accept(cv, 0);
        }
//
//        final TraceClassVisitor tcv = new TraceClassVisitor(cw, new PrintWriter(System.out));
//        classReader.accept(tcv, 0);
        return cw.toByteArray();
    }
}
