package com.jdfc.core.analysis.ifg.data;

import com.jdfc.core.analysis.JDFCMethodVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.tree.MethodNode;

import static org.objectweb.asm.Opcodes.ASM6;

class CFGLocalVariableMethodVisitor extends JDFCMethodVisitor {

    CFGLocalVariableMethodVisitor(
            final CFGLocalVariableClassVisitor pClassVisitor,
            final MethodVisitor pMethodVisitor,
            final MethodNode pMethodNode,
            final String pInternalMethodName) {
        super(ASM6, pClassVisitor, pMethodVisitor, pMethodNode, pInternalMethodName);
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
        boolean isReferenceToField = isLocalVariableReferenceToField(pIndex, pDescriptor);
        final LocalVariable variable = new LocalVariable(pName, pDescriptor, pSignature, pIndex, isReferenceToField);
        localVariableTable.addEntry(pIndex, variable);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void visitEnd() {
        for (InstanceVariable instanceVariable : classVisitor.classExecutionData.getInstanceVariables()) {
            if (localVariableTable.containsEntry(instanceVariable.getName(), instanceVariable.getDescriptor())) {
                instanceVariable.getOutOfScope().put(firstLine, currentLineNumber);
            }
        }
        classVisitor.getLocalVariableTables().put(internalMethodName, localVariableTable);
    }
}
