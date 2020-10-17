package com.jdfc.core.analysis.instr;

import com.jdfc.core.analysis.ifg.CFGImpl;
import com.jdfc.core.analysis.ifg.ProgramVariable;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodNode;

public class InstrumentationMethodVisitor extends MethodVisitor {

    final String className;
    final String methodName;
    final String methodDesc;
    final MethodNode methodNode;
    AbstractInsnNode currentNode = null;
    int currentLineNumber = -1;
    int currentInstructionIndex = -1;
    // workaround not to collide with jacoco:
    final String jacocoMethodName = "$jacoco";

    public InstrumentationMethodVisitor(MethodVisitor pMethodVisitor, String pClassName, String pMethodName, String pMethodDesc, MethodNode pMethodNode) {
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
        updateCurrentNode();
        mv.visitFrame(type, numLocal, local, numStack, stack);
    }

    @Override
    public void visitIincInsn(int var, int increment) {
        updateCurrentNode();
        mv.visitIincInsn(var, increment);
        insertLocalVariableEntryCreation(var);
    }

    @Override
    public void visitInsn(int opcode) {
        updateCurrentNode();
        mv.visitInsn(opcode);
    }

    @Override
    public void visitIntInsn(int opcode, int operand) {
        updateCurrentNode();
        mv.visitIntInsn(opcode, operand);
    }

    @Override
    public void visitTypeInsn(int opcode, String type) {
        updateCurrentNode();
        mv.visitTypeInsn(opcode, type);
    }

    @Override
    public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
        updateCurrentNode();
        insertInstanceVariableEntryCreation(owner, name, descriptor);
        mv.visitFieldInsn(opcode, owner, name, descriptor);
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
        updateCurrentNode();
        mv.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
    }

    @Override
    public void visitInvokeDynamicInsn(String name, String descriptor, Handle bootstrapMethodHandle, Object... bootstrapMethodArguments) {
        updateCurrentNode();
        mv.visitInvokeDynamicInsn(name, descriptor, bootstrapMethodHandle, bootstrapMethodArguments);
    }

    @Override
    public void visitJumpInsn(int opcode, Label label) {
        updateCurrentNode();
        mv.visitJumpInsn(opcode, label);
    }

    @Override
    public void visitLdcInsn(Object value) {
        updateCurrentNode();
        mv.visitLdcInsn(value);
    }

    @Override
    public void visitTableSwitchInsn(int min, int max, Label dflt, Label... labels) {
        updateCurrentNode();
        mv.visitTableSwitchInsn(min, max, dflt, labels);
    }

    @Override
    public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
        updateCurrentNode();
        mv.visitLookupSwitchInsn(dflt, keys, labels);
    }

    @Override
    public void visitMultiANewArrayInsn(String descriptor, int numDimensions) {
        updateCurrentNode();
        mv.visitMultiANewArrayInsn(descriptor, numDimensions);
    }

    @Override
    public void visitVarInsn(int opcode, int var) {
        updateCurrentNode();
        mv.visitVarInsn(opcode, var);
        insertLocalVariableEntryCreation(var);
    }

    private void insertLocalVariableEntryCreation(final int var) {
        mv.visitLdcInsn(className);
        mv.visitLdcInsn(methodName);
        mv.visitLdcInsn(methodDesc);
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
        if (!isJacocoInstrumentation(pName)) {
            mv.visitLdcInsn(className);
            mv.visitLdcInsn(pOwner);
            mv.visitLdcInsn(methodName);
            mv.visitLdcInsn(methodDesc);
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

    // TODO: Twice (see CFGCreatorVisitor)
    private void updateCurrentNode() {
        if (currentNode == null) {
            currentNode = methodNode.instructions.getFirst();
        } else {
            currentNode = currentNode.getNext();
        }
        currentInstructionIndex = methodNode.instructions.indexOf(currentNode);
    }

    private boolean isJacocoInstrumentation(String pString) {
        return pString.contains(jacocoMethodName);
    }
}
