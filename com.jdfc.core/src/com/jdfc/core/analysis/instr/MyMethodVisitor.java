package com.jdfc.core.analysis.instr;

import com.jdfc.core.analysis.cfg.CFGImpl;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodNode;

public class MyMethodVisitor extends MethodVisitor {

    final String className;
    final String methodName;
    final String methodDesc;
    final MethodNode methodNode;
    AbstractInsnNode currentNode = null;
    int currentLineNumber = -1;
    int currentInstructionIndex = Integer.MIN_VALUE;

    public MyMethodVisitor(MethodVisitor pMethodVisitor, String pClassName, String pMethodName, String pMethodDesc, MethodNode pMethodNode) {
        super(Opcodes.ASM6, pMethodVisitor);
        className = pClassName;
        methodName = pMethodName;
        methodDesc = pMethodDesc;
        methodNode = pMethodNode;
    }

    @Override
    public void visitLineNumber(int line, Label start) {
        currentLineNumber = line;
        mv.visitLineNumber(line, start);
    }

    @Override
    public void visitFrame(int type, int numLocal, Object[] local, int numStack, Object[] stack) {
        System.out.println("visitFrame");
        updateCurrentNode();
        mv.visitFrame(type, numLocal, local, numStack, stack);
    }

    @Override
    public void visitIincInsn(int var, int increment) {
        System.out.println("visitIincInsn");
        updateCurrentNode();
        mv.visitIincInsn(var, increment);
    }

    @Override
    public void visitInsn(int opcode) {
        System.out.println("visitInsn");
        updateCurrentNode();
        mv.visitInsn(opcode);
    }

    @Override
    public void visitIntInsn(int opcode, int operand) {
        System.out.println("visitIntInsn");
        updateCurrentNode();
        mv.visitIntInsn(opcode, operand);
    }

    @Override
    public void visitTypeInsn(int opcode, String type) {
        System.out.println("visitTypeInsn");
        updateCurrentNode();
        mv.visitTypeInsn(opcode, type);
    }

    @Override
    public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
        System.out.println("visitFieldInsn");
        updateCurrentNode();
        mv.visitFieldInsn(opcode, owner, name, descriptor);
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
        System.out.println("visitMethodInsn");
        updateCurrentNode();
        mv.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
    }

    @Override
    public void visitInvokeDynamicInsn(String name, String descriptor, Handle bootstrapMethodHandle, Object... bootstrapMethodArguments) {
        System.out.println("visitInvokeDynamicInsn");
        updateCurrentNode();
        mv.visitInvokeDynamicInsn(name, descriptor, bootstrapMethodHandle, bootstrapMethodArguments);
    }

    @Override
    public void visitJumpInsn(int opcode, Label label) {
        System.out.println("visitJumpInsn");
        updateCurrentNode();
        mv.visitJumpInsn(opcode, label);
    }

    @Override
    public void visitLdcInsn(Object value) {
        System.out.println("visitLdcInsn");
        updateCurrentNode();
        mv.visitLdcInsn(value);
    }

    @Override
    public void visitTableSwitchInsn(int min, int max, Label dflt, Label... labels) {
        System.out.println("visitTableSwitchInsn");
        updateCurrentNode();
        mv.visitTableSwitchInsn(min, max, dflt, labels);
    }

    @Override
    public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
        System.out.println("visitLookupSwitchInsn");
        updateCurrentNode();
        mv.visitLookupSwitchInsn(dflt, keys, labels);
    }

    @Override
    public void visitMultiANewArrayInsn(String descriptor, int numDimensions) {
        System.out.println("visitMultiANewArrayInsn");
        updateCurrentNode();
        mv.visitMultiANewArrayInsn(descriptor, numDimensions);
    }

    @Override
    public void visitVarInsn(int opcode, int var) {
        System.out.println("visitVarInsn");
        updateCurrentNode();
        mv.visitVarInsn(opcode, var);
        mv.visitLdcInsn(className);
        mv.visitLdcInsn(methodName);
        mv.visitLdcInsn(methodDesc);
        mv.visitLdcInsn(var);
        mv.visitLdcInsn(currentInstructionIndex);
        mv.visitLdcInsn(currentLineNumber);
        mv.visitMethodInsn(
                Opcodes.INVOKESTATIC,
                Type.getInternalName(CFGImpl.class),
                "addCoveredEntry",
                "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;III)V",
                false);
    }

    @Override
    public void visitMaxs(int maxStack, int maxLocals) {
        mv.visitMaxs(maxStack+5, maxLocals);
    }

    @Override
    public void visitEnd() {
        mv.visitEnd();
    }

    // TODO: Twice (see CFGCreatorVisitor)
    private void updateCurrentNode() {
        if (currentNode == null) {
            currentNode = methodNode.instructions.getFirst();
        } else {
            currentNode = currentNode.getNext();
        }
        currentInstructionIndex = methodNode.instructions.indexOf(currentNode);
    }
}
