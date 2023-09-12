package graphs.cfg.visitors.classVisitors;

import data.ClassData;
import graphs.cfg.visitors.methodVisitors.CFGLocalVariableMethodVisitor;
import instr.classVisitors.JDFCClassVisitor;
import lombok.extern.slf4j.Slf4j;
import org.objectweb.asm.ClassVisitor;
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

    public CFGLocalVariableClassVisitor(final ClassNode pClassNode, final ClassData pClassData) {
        super(ASM5, pClassNode, pClassData);
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

        if (methodNode != null && isInstrumentationRequired(methodNode, internalMethodName)) {
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
    }
}
