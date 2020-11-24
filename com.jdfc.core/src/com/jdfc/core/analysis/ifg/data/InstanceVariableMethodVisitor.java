package com.jdfc.core.analysis.ifg.data;

import com.jdfc.core.analysis.JDFCClassVisitor;
import com.jdfc.core.analysis.JDFCMethodVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.tree.MethodNode;

import java.util.Map;

import static org.objectweb.asm.Opcodes.*;

public class InstanceVariableMethodVisitor extends JDFCMethodVisitor {

    public InstanceVariableMethodVisitor(final JDFCClassVisitor pClassVisitor,
                                         final MethodVisitor pMethodVisitor,
                                         final MethodNode pMethodNode,
                                         final String pInternalMethodName,
                                         Map<Integer, LocalVariable> pLocalVariableTable) {
        super(ASM5, pClassVisitor, pMethodVisitor, pMethodNode, pInternalMethodName, pLocalVariableTable);
    }

    /**
     * For every PUTFIELD Insn we create a new {@code InstanceVariableOccurrence}. The {@code OwnerVariable} is a local
     * variable that grants the access to the {@code InstanceVariable} and is used to associate the object holding
     * the field with the field itself.
     *
     * @param pOpcode
     * @param pOwner
     * @param pName
     * @param pDescription
     */
    @Override
    public void visitFieldInsn(int pOpcode, String pOwner, String pName, String pDescription) {
        if (mv != null) {
            mv.visitFieldInsn(pOpcode, pOwner, pName, pDescription);
        } else {
            super.visitFieldInsn(pOpcode, pOwner, pName, pDescription);
        }

        boolean isDefinition = isDefinition(pOpcode);
        InstanceVariable instanceVariable = InstanceVariable.create(pOwner,
                pName, pDescription, currentInstructionIndex, currentLineNumber,
                isDefinition);

        classVisitor.classExecutionData.getInstanceVariables().add(instanceVariable);
    }
}

