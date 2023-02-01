package ifg.data;

import instr.JDFCMethodVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.tree.MethodNode;

import static org.objectweb.asm.Opcodes.ASM5;

class LocalVariableMethodVisitor extends JDFCMethodVisitor {

    LocalVariableMethodVisitor(
            final LocalVariableClassVisitor pClassVisitor,
            final MethodVisitor pMethodVisitor,
            final MethodNode pMethodNode,
            final String pInternalMethodName) {
        super(ASM5, pClassVisitor, pMethodVisitor, pMethodNode, pInternalMethodName);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void visitLocalVariable(
            final String pName,
            final String pDescriptor,
            final String pSignature,
            final Label pStart,
            final Label pEnd,
            final int pIndex) {
        super.visitLocalVariable(pName, pDescriptor, pSignature, pStart, pEnd, pIndex);
        final LocalVariable variable = new LocalVariable(pName, pDescriptor, pSignature, pIndex);
        localVariableTable.put(pIndex, variable);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void visitEnd() {
        classVisitor.getLocalVariableTables().put(internalMethodName, localVariableTable);
    }
}
