package com.jdfc.core.analysis;

import com.jdfc.core.analysis.ifg.data.LocalVariable;
import com.jdfc.core.analysis.ifg.data.ProgramVariable;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;


import java.util.*;

import static org.objectweb.asm.Opcodes.*;
import static org.objectweb.asm.Opcodes.PUTFIELD;

public abstract class JDFCMethodVisitor extends MethodVisitor {

    public final JDFCClassVisitor classVisitor;
    public final MethodNode methodNode;
    public final Map<Integer, LocalVariable> localVariableTable;
    public String internalMethodName;

    public AbstractInsnNode currentNode = null;
    public int currentInstructionIndex = -1;
    public int currentLineNumber = -1;
    public int firstLine = -1;
    public String classDescriptor;

    public final String jacocoPrefix = "$jacoco";

    public JDFCMethodVisitor(final int pApi,
                             final JDFCClassVisitor pClassVisitor,
                             final MethodVisitor pMethodVisitor,
                             final MethodNode pMethodNode,
                             final String pInternalMethodName) {
        super(pApi, pMethodVisitor);
        classVisitor = pClassVisitor;
        methodNode = pMethodNode;
        internalMethodName = pInternalMethodName;
        localVariableTable = new HashMap<>();
        classDescriptor = String.format("L%s;", classVisitor.classExecutionData.getRelativePath());
    }

    public JDFCMethodVisitor(final int pApi,
                             final JDFCClassVisitor pClassVisitor,
                             final MethodVisitor pMethodVisitor,
                             final MethodNode pMethodNode,
                             final String pInternalMethodName,
                             final Map<Integer, LocalVariable> pLocalVariableTable) {
        super(pApi, pMethodVisitor);
        classVisitor = pClassVisitor;
        methodNode = pMethodNode;
        internalMethodName = pInternalMethodName;
        localVariableTable = pLocalVariableTable;
        classDescriptor = String.format("L%s;", classVisitor.classExecutionData.getRelativePath());
    }

    @Override
    public void visitCode() {
        //System.out.printf("[DEBUG] visitCode %s %s\n", classVisitor.classNode.name, methodNode.name);
        super.visitCode();
    }

    @Override
    public void visitLineNumber(int line, Label start) {
        if (firstLine == -1) {
            if (!methodNode.name.contains("<init>")) {
                firstLine += line;
            } else {
                firstLine = line;
            }

        }
        currentLineNumber = line;
        super.visitLineNumber(line, start);
    }

    @Override
    public void visitFrame(int type, int numLocal, Object[] local, int numStack, Object[] stack) {
        updateCurrentNode();
        //System.out.printf("[DEBUG] visitFrame %s %s\n", currentInstructionIndex, type);
        super.visitFrame(type, numLocal, local, numStack, stack);
    }

    @Override
    public void visitInsn(int opcode) {
        updateCurrentNode();
        visitFrameNew();
        //System.out.printf("[DEBUG] visitInsn %s %s\n", currentInstructionIndex, opcode);
        super.visitInsn(opcode);
    }

    @Override
    public void visitIntInsn(int opcode, int operand) {
        updateCurrentNode();
        visitFrameNew();
        //System.out.printf("[DEBUG] visitIntInsn %s %s\n", currentInstructionIndex, opcode);
        super.visitIntInsn(opcode, operand);
    }

    @Override
    public void visitVarInsn(int opcode, int var) {
        updateCurrentNode();
        visitFrameNew();
        //System.out.printf("[DEBUG] visitVarInsn %s %s\n", currentInstructionIndex, opcode);
        super.visitVarInsn(opcode, var);
    }

    @Override
    public void visitTypeInsn(int opcode, String type) {
        updateCurrentNode();
        visitFrameNew();
        //System.out.printf("[DEBUG] visitTypeInsn %s %s\n", currentInstructionIndex, opcode);
        super.visitTypeInsn(opcode, type);
    }

    @Override
    public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
        updateCurrentNode();
        //System.out.printf("[DEBUG] visitFieldInsn %s %s\n", currentInstructionIndex, opcode);
        super.visitFieldInsn(opcode, owner, name, descriptor);
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
        updateCurrentNode();
        visitFrameNew();
        //System.out.printf("[DEBUG] visitMethodInsn %s %s\n", currentInstructionIndex, opcode);
        super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
    }

    @Override
    public void visitInvokeDynamicInsn(String name, String descriptor, Handle bootstrapMethodHandle, Object... bootstrapMethodArguments) {
        updateCurrentNode();
        visitFrameNew();
        //System.out.printf("[DEBUG] visitInvokeDynamicInsn %s %s\n", currentInstructionIndex, INVOKEDYNAMIC);
        super.visitInvokeDynamicInsn(name, descriptor, bootstrapMethodHandle, bootstrapMethodArguments);
    }

    @Override
    public void visitJumpInsn(int opcode, Label label) {
        updateCurrentNode();
        visitFrameNew();
        //System.out.printf("[DEBUG] visitJumpInsn %s %s\n", currentInstructionIndex, opcode);
        super.visitJumpInsn(opcode, label);
    }

    @Override
    public void visitLdcInsn(Object value) {
        updateCurrentNode();
        visitFrameNew();
        //System.out.printf("[DEBUG] visitLdcInsn %s %s\n", currentInstructionIndex, LDC);
        super.visitLdcInsn(value);
    }

    @Override
    public void visitIincInsn(int var, int increment) {
        updateCurrentNode();
        visitFrameNew();
        //System.out.printf("[DEBUG] visitIincInsn %s %s \n", currentInstructionIndex, IINC);
        super.visitIincInsn(var, increment);
    }

    @Override
    public void visitTableSwitchInsn(int min, int max, Label dflt, Label... labels) {
        updateCurrentNode();
        visitFrameNew();
        //System.out.printf("[DEBUG] visitTableSwitchInsn %s %s\n", currentInstructionIndex, TABLESWITCH);
        super.visitTableSwitchInsn(min, max, dflt, labels);
    }

    @Override
    public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
        updateCurrentNode();
        visitFrameNew();
        //System.out.printf("[DEBUG] visitLookupSwitchInsn %s %s\n ", currentInstructionIndex, LOOKUPSWITCH);
        super.visitLookupSwitchInsn(dflt, keys, labels);
    }

    @Override
    public void visitMultiANewArrayInsn(String descriptor, int numDimensions) {
        updateCurrentNode();
        visitFrameNew();
        //System.out.printf("[DEBUG] visitMultiANewArrayInsn %s %s\n", currentInstructionIndex, MULTIANEWARRAY);
        super.visitMultiANewArrayInsn(descriptor, numDimensions);
    }

    @Override
    public void visitMaxs(int maxStack, int maxLocals) {
        super.visitMaxs(maxStack + 9, maxLocals);
    }

    @Override
    public void visitEnd() {
        super.visitEnd();
    }

    protected void updateCurrentNode() {
        if (currentNode == null) {
            currentNode = methodNode.instructions.getFirst();
        } else {
            if (currentNode.getNext() != null) {
                currentNode = currentNode.getNext();
            }
        }
        currentInstructionIndex = methodNode.instructions.indexOf(currentNode);
    }

    public void visitFrameNew() {
        if (currentNode.getOpcode() == F_NEW) {
            updateCurrentNode();
        }
    }

    protected ProgramVariable getProgramVariableFromLocalVar(final int varNumber,
                                                             final int pOpcode,
                                                             final int pIndex,
                                                             final int pLineNumber) {
        final String varName = getVariableNameFromLocalVariablesTable(varNumber);
        final String varType = getVariableTypeFromLocalVariablesTable(varNumber);
        final boolean isDefinition = isDefinition(pOpcode);
        return ProgramVariable.create(null, varName, varType, pIndex, pLineNumber, isDefinition);
    }

    private String getVariableNameFromLocalVariablesTable(final int pVarNumber) {
        final LocalVariable localVariable = localVariableTable.get(pVarNumber);
        if (localVariable != null) {
            return localVariable.getName();
        } else {
            return String.valueOf(pVarNumber);
        }
    }

    private String getVariableTypeFromLocalVariablesTable(final int pVarNumber) {
        final LocalVariable localVariable = localVariableTable.get(pVarNumber);
        if (localVariable != null) {
            return localVariable.getDescriptor();
        } else {
            return "UNKNOWN";
        }
    }

    protected boolean isInstrumentationRequired(String pString) {
        return !pString.contains(jacocoPrefix);
    }

    protected boolean isDefinition(final int pOpcode) {
        switch (pOpcode) {
            case ISTORE:
            case LSTORE:
            case FSTORE:
            case DSTORE:
            case ASTORE:
            case PUTFIELD:
                return true;
            default:
                return false;
        }
    }
}
