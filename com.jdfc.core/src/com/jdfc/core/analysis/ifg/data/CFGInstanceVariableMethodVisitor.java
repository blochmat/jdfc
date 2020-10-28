package com.jdfc.core.analysis.ifg.data;

import com.jdfc.core.analysis.JDFCClassVisitor;
import com.jdfc.core.analysis.JDFCMethodVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

import java.util.Set;

import static org.objectweb.asm.Opcodes.ASM6;
import static org.objectweb.asm.Opcodes.PUTFIELD;

public class CFGInstanceVariableMethodVisitor extends JDFCMethodVisitor {

    public CFGInstanceVariableMethodVisitor(final JDFCClassVisitor pClassVisitor,
                                            final MethodVisitor pMethodVisitor,
                                            final MethodNode pMethodNode,
                                            final String pInternalMethodName,
                                            LocalVariableTable pLocalVariableTable) {
        super(ASM6, pClassVisitor, pMethodVisitor, pMethodNode, pInternalMethodName, pLocalVariableTable);
    }

    /**
     * For every PUTFIELD Insn we create a new {@code InstanceVariableOccurrence}. The {@code OwnerVariable} is a local
     * variable that grants the access to the {@code InstanceVariable} and is used to associate the object holding
     * the field with the field itself.
     * @param pOpcode
     * @param pOwner
     * @param pName
     * @param pDescription
     */
    @Override
    public void visitFieldInsn(int pOpcode, String pOwner, String pName, String pDescription) {
        if(mv != null) {
            mv.visitFieldInsn(pOpcode, pOwner, pName, pDescription);
        } else {
            super.visitFieldInsn(pOpcode, pOwner, pName, pDescription);
        }

        if (pOpcode == PUTFIELD) {
            VarInsnNode ownerNode = getOwnerNode(PUTFIELD_STANDARD);
            ProgramVariable ownerVariable =
                    getProgramVariableFromLocalVar(ownerNode.var, currentInstructionIndex, currentLineNumber);
            Set<InstanceVariable> instanceVariables = classVisitor.classExecutionData.getInstanceVariables();
            for (InstanceVariable instanceVariable : instanceVariables) {
                if (instanceVariable.getOwner().equals(pOwner)
                        && instanceVariable.getName().equals(pName)
                        && instanceVariable.getDescriptor().equals(pDescription)) {
                    classVisitor.classExecutionData.getInstanceVariablesOccurrences().add(
                            InstanceVariable.create(instanceVariable.getOwner(),
                                    ownerVariable,
                                    instanceVariable.getAccess(),
                                    pName,
                                    pDescription,
                                    instanceVariable.getSignature(),
                                    currentLineNumber)
                    );
                }
            }
        }
    }
}
