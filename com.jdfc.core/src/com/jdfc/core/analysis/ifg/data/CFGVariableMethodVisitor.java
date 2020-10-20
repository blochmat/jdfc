package com.jdfc.core.analysis.ifg.data;

import com.jdfc.core.analysis.JDFCMethodVisitor;
import com.jdfc.core.analysis.ifg.CFGCreator;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

import java.util.Set;

import static org.objectweb.asm.Opcodes.ASM6;
import static org.objectweb.asm.Opcodes.PUTFIELD;

class CFGVariableMethodVisitor extends JDFCMethodVisitor {

    private final String descriptor;
    private final String signature;
    private final String[] exceptions;

    CFGVariableMethodVisitor(
            final CFGVariableClassVisitor pClassVisitor,
            final MethodVisitor pMethodVisitor,
            final String pMethodName,
            final String pDescriptor,
            final String pSignature,
            final String[] pExceptions) {
        super(ASM6, pClassVisitor, pMethodVisitor, pMethodName);
        descriptor = pDescriptor;
        signature = pSignature;
        exceptions = pExceptions;
    }

    @Override
    public void visitFieldInsn(int pOpcode, String pOwner, String pName, String pDescription) {
        if (pOpcode == PUTFIELD) {
            Set<InstanceVariable> instanceVariables = classVisitor.classExecutionData.getInstanceVariables();
            for (InstanceVariable instanceVariable : instanceVariables) {
                if (instanceVariable.getOwner().equals(pOwner)
                        && instanceVariable.getName().equals(pName)
                        && instanceVariable.getDescriptor().equals(pDescription)) {
                    instanceVariable.setLineNumber(currentLineNumber);
                }
            }
        }
        super.visitFieldInsn(pOpcode, pOwner, pName, pDescription);
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
        final LocalVariable variable = new LocalVariable(pName, pDescriptor, pSignature, pIndex);
        localVariableTable.addEntry(pIndex, variable);
        super.visitLocalVariable(pName, pDescriptor, pSignature, pStart, pEnd, pIndex);
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

        final String methodName =
                CFGCreator.computeInternalMethodName(this.methodName, descriptor, signature, exceptions);
        classVisitor.getLocalVariableTables().put(methodName, localVariableTable);//        }
    }
}
