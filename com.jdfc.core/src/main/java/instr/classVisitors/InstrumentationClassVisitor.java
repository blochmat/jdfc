package instr.classVisitors;

import data.ClassExecutionData;
import instr.methodVisitors.InstrumentationMethodVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.ASMHelper;

import static org.objectweb.asm.Opcodes.ASM5;

public class InstrumentationClassVisitor extends JDFCClassVisitor {

    private final Logger logger = LoggerFactory.getLogger(InstrumentationClassVisitor.class);

    private final ASMHelper asmHelper = new ASMHelper();

    public String className;

    public InstrumentationClassVisitor(final ClassVisitor pClassVisitor,
                                       final ClassNode pClassNode,
                                       final ClassExecutionData pClassExecutionData) {
        super(ASM5, pClassVisitor, pClassNode, pClassExecutionData);
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        this.className = name;
        super.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public MethodVisitor visitMethod(final int pAccess,
                                     final String pName,
                                     final String pDescriptor,
                                     final String pSignature,
                                     final String[] pExceptions) {
        MethodVisitor mv = super.visitMethod(pAccess, pName, pDescriptor, pSignature, pExceptions);
        MethodNode methodNode = getMethodNode(pName, pDescriptor);
        final String internalMethodName = asmHelper.computeInternalMethodName(pName, pDescriptor, pSignature, pExceptions);
        if (methodNode != null && isInstrumentationRequired(methodNode, internalMethodName) ) {
            mv = new InstrumentationMethodVisitor(this, mv, methodNode, internalMethodName);
        }

        return mv;
    }

    @Override
    public void visitEnd() {
        super.visitEnd();
    }
}