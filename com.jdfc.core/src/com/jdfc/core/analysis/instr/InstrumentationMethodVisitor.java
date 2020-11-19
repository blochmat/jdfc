package com.jdfc.core.analysis.instr;

import com.jdfc.core.analysis.JDFCMethodVisitor;
import com.jdfc.core.analysis.data.CoverageDataStore;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.MethodNode;

import java.util.Arrays;
import java.util.List;

import static org.objectweb.asm.Opcodes.*;

public class InstrumentationMethodVisitor extends JDFCMethodVisitor {

    public InstrumentationMethodVisitor(InstrumentationClassVisitor pClassVisitor,
                                        MethodVisitor pMethodVisitor,
                                        MethodNode pMethodNode,
                                        String internalMethodName) {
        super(ASM5, pClassVisitor, pMethodVisitor, pMethodNode, internalMethodName);
    }

    @Override
    public void visitVarInsn(int opcode, int var) {
        super.visitVarInsn(opcode, var);
        insertLocalVariableEntryCreation(opcode, var);
    }

    @Override
    public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
        super.visitFieldInsn(opcode, owner, name, descriptor);
        insertInstanceVariableEntryCreation(opcode, owner, name, descriptor);
    }

    @Override
    public void visitIincInsn(int var, int increment) {
        super.visitIincInsn(var, increment);
        insertLocalVariableEntryCreation(ISTORE, var);
    }



    private void insertLocalVariableEntryCreation(final int pOpcode,
                                                  final int pVar) {
        mv.visitLdcInsn(classVisitor.classNode.name);
        mv.visitLdcInsn(internalMethodName);
        mv.visitLdcInsn(pVar);
        mv.visitLdcInsn(currentInstructionIndex);
        mv.visitLdcInsn(currentLineNumber);
        mv.visitLdcInsn(pOpcode);
        mv.visitMethodInsn(
                Opcodes.INVOKESTATIC,
                Type.getInternalName(CoverageDataStore.class),
                "invokeCoverageTracker",
                "(Ljava/lang/String;" +
                        "Ljava/lang/String;" +
                        "IIII)V",
                false);
    }

    private void insertInstanceVariableEntryCreation(final int pOpcode,
                                                     final String pOwner,
                                                     final String pName,
                                                     final String pDescriptor) {
        if (isInstrumentationRequired(pName)) {
            mv.visitLdcInsn(classVisitor.classNode.name);
            mv.visitLdcInsn(pOwner);
            mv.visitLdcInsn(internalMethodName);
            mv.visitLdcInsn(pName);
            mv.visitLdcInsn(pDescriptor);
            mv.visitLdcInsn(currentInstructionIndex);
            mv.visitLdcInsn(currentLineNumber);
            mv.visitLdcInsn(pOpcode);
            mv.visitMethodInsn(
                    Opcodes.INVOKESTATIC,
                    Type.getInternalName(CoverageDataStore.class),
                    "invokeCoverageTracker",
                    "(Ljava/lang/String;" +
                            "Ljava/lang/String;" +
                            "Ljava/lang/String;" +
                            "Ljava/lang/String;" +
                            "Ljava/lang/String;" +
                            "III)V",
                    false);
        }
    }
}
