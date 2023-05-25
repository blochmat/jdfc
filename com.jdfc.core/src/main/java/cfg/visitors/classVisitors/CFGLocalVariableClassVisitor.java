package cfg.visitors.classVisitors;

import cfg.visitors.methodVisitors.CFGLocalVariableMethodVisitor;
import data.ClassExecutionData;
import cfg.CFGCreator;
import data.ProgramVariable;
import instr.classVisitors.JDFCClassVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.JDFCUtils;

import static org.objectweb.asm.Opcodes.ASM5;

/**
 * A class visitor that extracts the information of the local variable table for each method of a
 * class.
 *
 * @see ClassVisitor
 */
public class CFGLocalVariableClassVisitor extends JDFCClassVisitor {

    private final Logger logger = LoggerFactory.getLogger(CFGLocalVariableClassVisitor.class);

    public CFGLocalVariableClassVisitor(final ClassNode pClassNode, final ClassExecutionData pClassExecutionData) {
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
        this.getFields().add(
                ProgramVariable.create(this.getClassNode().name,
                        name, descriptor, Integer.MIN_VALUE, Integer.MIN_VALUE, false));
        logger.debug(String.format("Field: %s %s %s = %s%n", JDFCUtils.getAccess(access), descriptor, name, value));
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
        final String internalMethodName = CFGCreator.computeInternalMethodName(pName, pDescriptor, pSignature, pExceptions);
        if (methodNode != null && isInstrumentationRequired(pName)) {
            return new CFGLocalVariableMethodVisitor(
                    this, mv, methodNode, internalMethodName);
        }
        return mv;
    }
}
