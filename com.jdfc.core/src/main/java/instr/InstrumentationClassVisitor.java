package instr;

import data.ClassExecutionData;
import ifg.CFGCreator;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import static org.objectweb.asm.Opcodes.ASM5;

public class InstrumentationClassVisitor extends JDFCClassVisitor {

    public InstrumentationClassVisitor(final ClassVisitor pClassVisitor,
                                       final ClassNode pClassNode,
                                       final ClassExecutionData pClassExecutionData) {
        super(ASM5, pClassVisitor, pClassNode, pClassExecutionData);
    }

    @Override
    public MethodVisitor visitMethod(final int pAccess,
                                     final String pName,
                                     final String pDescriptor,
                                     final String pSignature,
                                     final String[] pExceptions) {
        MethodVisitor mv = super.visitMethod(pAccess, pName, pDescriptor, pSignature, pExceptions);
        if (isInstrumentationRequired(pName)) {
            MethodNode methodNode = getMethodNode(pName, pDescriptor);
            final String internalMethodName = CFGCreator.computeInternalMethodName(pName, pDescriptor, pSignature, pExceptions);
            mv = new InstrumentationMethodVisitor(this, mv, methodNode, internalMethodName);
        }

        return mv;
    }
}