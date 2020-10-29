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

        Set<Field> fields = classVisitor.classExecutionData.getFields();
        for (Field field : fields) {
            if (field.getOwner().equals(pOwner)
                    && field.getName().equals(pName)
                    && field.getDescriptor().equals(pDescription)) {
                VarInsnNode ownerNode;
                if (pOpcode == PUTFIELD) {
                    ownerNode = getOwnerNode(PUTFIELD_STANDARD);
                } else {
                    ownerNode = getOwnerNode(GETFIELD_STANDARD);
                }
                ProgramVariable ownerVariable =
                        getProgramVariableFromLocalVar(ownerNode.var, currentInstructionIndex-1, currentLineNumber);

                classVisitor.classExecutionData.getInstanceVariables().add(
                        InstanceVariable.create(pOwner,
                                ownerVariable,
                                field.getAccess(),
                                pName,
                                pDescription,
                                field.getSignature(),
                                currentInstructionIndex,
                                currentLineNumber)
                );
                break;
            }
        }

    }
}
