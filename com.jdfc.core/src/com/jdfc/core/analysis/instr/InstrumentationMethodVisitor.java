package com.jdfc.core.analysis.instr;

import com.jdfc.core.analysis.JDFCMethodVisitor;
import com.jdfc.core.analysis.ifg.CFGImpl;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.MethodNode;

import java.util.Arrays;
import java.util.List;

import static org.objectweb.asm.Opcodes.*;

public class InstrumentationMethodVisitor extends JDFCMethodVisitor {

    List<Integer> returnOpcodes = Arrays.asList(RETURN, IRETURN, DRETURN, ARETURN, FRETURN, LRETURN);

    public InstrumentationMethodVisitor(InstrumentationClassVisitor pClassVisitor,
                                        MethodVisitor pMethodVisitor,
                                        MethodNode pMethodNode,
                                        String internalMethodName) {
        super(ASM6, pClassVisitor, pMethodVisitor, pMethodNode, internalMethodName);
    }

    @Override
    public void visitInsn(int opcode) {
        if(returnOpcodes.contains(opcode) && !methodNode.name.contains("<init>")) {
            mv.visitLdcInsn(classVisitor.classNode.name);
            mv.visitMethodInsn(
                    Opcodes.INVOKESTATIC,
                    Type.getInternalName(CFGImpl.class),
                    "dumpClassExecutionDataToFile",
                    "(Ljava/lang/String;)V",
                    false);
        }
        super.visitInsn(opcode);
    }

    @Override
    public void visitVarInsn(int opcode, int var) {
        super.visitVarInsn(opcode, var);
        insertLocalVariableEntryCreation(var);
    }

    @Override
    public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
        super.visitFieldInsn(opcode, owner, name, descriptor);
        insertInstanceVariableEntryCreation(owner, name, descriptor);
    }

    @Override
    public void visitIincInsn(int var, int increment) {
        super.visitIincInsn(var, increment);
        insertLocalVariableEntryCreation(var);
    }

    private void insertLocalVariableEntryCreation(final int var) {
        mv.visitLdcInsn(classVisitor.classNode.name);
        mv.visitLdcInsn(methodNode.name);
        mv.visitLdcInsn(methodNode.desc);
        mv.visitLdcInsn(var);
        mv.visitLdcInsn(currentInstructionIndex);
        mv.visitLdcInsn(currentLineNumber);
        mv.visitMethodInsn(
                Opcodes.INVOKESTATIC,
                Type.getInternalName(CFGImpl.class),
                "addLocalVarCoveredEntry",
                "(Ljava/lang/String;" +
                        "Ljava/lang/String;" +
                        "Ljava/lang/String;" +
                        "III)V",
                false);
    }

    private void insertInstanceVariableEntryCreation(final String pOwner, final String pName, final String pDescriptor) {
        if (isInstrumentationRequired(pName)) {
            mv.visitLdcInsn(classVisitor.classNode.name);
            mv.visitLdcInsn(pOwner);
            mv.visitLdcInsn(methodNode.name);
            mv.visitLdcInsn(methodNode.desc);
            mv.visitLdcInsn(pName);
            mv.visitLdcInsn(pDescriptor);
            mv.visitLdcInsn(currentInstructionIndex);
            mv.visitLdcInsn(currentLineNumber);
            mv.visitMethodInsn(
                    Opcodes.INVOKESTATIC,
                    Type.getInternalName(CFGImpl.class),
                    "addInstanceVarCoveredEntry",
                    "(Ljava/lang/String;" +
                            "Ljava/lang/String;" +
                            "Ljava/lang/String;" +
                            "Ljava/lang/String;" +
                            "Ljava/lang/String;" +
                            "Ljava/lang/String;" +
                            "II)V",
                    false);
        }
    }
}