package icfg.visitors.classVisitors;

import data.ClassExecutionData;
import icfg.ICFGCreator;
import icfg.visitors.methodVisitors.LocalVariableMethodVisitor;
import instr.classVisitors.JDFCClassVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import utils.JDFCUtils;

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
     *
     * Used to grab all fields of a class.
     * Value is assigned in the constructor.
     *
     */
    @Override
    public FieldVisitor visitField(
            final int access,
            final String name,
            final String descriptor,
            final String signature,
            final Object value) {
        final FieldVisitor fv = super.visitField(access, name, descriptor, signature, value);
        System.out.printf("Field: %s %s %s = %s%n", JDFCUtils.getAccess(access), descriptor, name, value);
        return fv;
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
        final String internalMethodName = ICFGCreator.computeInternalMethodName(pName, pDescriptor, pSignature, pExceptions);
        if (methodNode != null && isInstrumentationRequired(pName)) {
            return new LocalVariableMethodVisitor(
                    this, mv, methodNode, internalMethodName);
        }
        return mv;
    }
}
