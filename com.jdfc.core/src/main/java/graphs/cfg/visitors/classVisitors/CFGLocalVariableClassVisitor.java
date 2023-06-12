package graphs.cfg.visitors.classVisitors;

import data.ClassExecutionData;
import graphs.cfg.visitors.methodVisitors.CFGLocalVariableMethodVisitor;
import instr.classVisitors.JDFCClassVisitor;
import lombok.extern.slf4j.Slf4j;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import utils.ASMHelper;

import static org.objectweb.asm.Opcodes.ASM5;

/**
 * A class visitor that extracts the information of the local variable table for each method of a
 * class.
 *
 * @see ClassVisitor
 */
@Slf4j
public class CFGLocalVariableClassVisitor extends JDFCClassVisitor {

    private final ASMHelper asmHelper = new ASMHelper();

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
//        this.fields.add(
//                new ProgramVariable(this.getClassNode().name,
//                        name, descriptor, Integer.MIN_VALUE, Integer.MIN_VALUE, false, false));
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
        final MethodNode methodNode = this.getMethodNode(pName, pDescriptor);
        final String internalMethodName = asmHelper.computeInternalMethodName(pName, pDescriptor, pSignature, pExceptions);
        // TODO

        if (methodNode != null
                && isInstrumentationRequired(methodNode)
                && !internalMethodName.contains("<init>")
                && !internalMethodName.contains("<clinit>")) {
            return new CFGLocalVariableMethodVisitor(
                    this, mv, methodNode, internalMethodName);
        }
        return mv;
    }

    @Override
    public void visitEnd() {
        if (cv != null) {
            cv.visitEnd();
        } else {
            super.visitEnd();
        }
        // TODO: Add fields to classExecutionData
    }
}
