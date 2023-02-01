package ifg.data;

import data.ClassExecutionData;
import ifg.CFGCreator;
import instr.JDFCClassVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import static org.objectweb.asm.Opcodes.ASM5;

/**
 * A class visitor that extracts the information of the local variable table for each method of a
 * class.
 *
 * @see ClassVisitor
 */
public class LocalVariableClassVisitor extends JDFCClassVisitor {

    public LocalVariableClassVisitor(final ClassNode pClassNode, final ClassExecutionData pClassExecutionData) {
        super(ASM5, pClassNode, pClassExecutionData);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MethodVisitor visitMethod(final int pAccess,
                                     final String pName,
                                     final String pDescriptor,
                                     final String pSignature,
                                     final String[] pExceptions) {
        final MethodVisitor mv = super.visitMethod(pAccess, pName, pDescriptor, pSignature, pExceptions);
        final MethodNode methodNode = getMethodNode(pName, pDescriptor);
        final String internalMethodName = CFGCreator.computeInternalMethodName(pName, pDescriptor, pSignature, pExceptions);
        if (methodNode != null && isInstrumentationRequired(pName)) {
            return new LocalVariableMethodVisitor(
                    this, mv, methodNode, internalMethodName);
        }
        return mv;
    }
}
