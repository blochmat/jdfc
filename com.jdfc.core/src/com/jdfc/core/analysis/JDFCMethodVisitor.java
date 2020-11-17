package com.jdfc.core.analysis;

import com.jdfc.core.analysis.ifg.CFGNode;
import com.jdfc.core.analysis.ifg.data.LocalVariable;
import com.jdfc.core.analysis.ifg.data.LocalVariableTable;
import com.jdfc.core.analysis.ifg.data.ProgramVariable;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;


import java.util.NavigableMap;
import java.util.Optional;

import static org.objectweb.asm.Opcodes.*;
import static org.objectweb.asm.Opcodes.PUTFIELD;

public abstract class JDFCMethodVisitor extends MethodVisitor {

    public final JDFCClassVisitor classVisitor;
    public final MethodNode methodNode;
    public final LocalVariableTable localVariableTable;
    public String internalMethodName;

    public AbstractInsnNode currentNode = null;
    public int currentInstructionIndex = -1;
    public int currentLineNumber = -1;
    public int firstLine = -1;
    public String classDescriptor;

    public final int PUTFIELD_STANDARD = 2;
    public final int GETFIELD_STANDARD = 1;
    public final int INVOKE_STANDARD = 1;

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
        localVariableTable = new LocalVariableTable();
        classDescriptor = String.format("L%s;", classVisitor.classExecutionData.getRelativePath());
    }

    public JDFCMethodVisitor(final int pApi,
                             final JDFCClassVisitor pClassVisitor,
                             final MethodVisitor pMethodVisitor,
                             final MethodNode pMethodNode,
                             final String pInternalMethodName,
                             final LocalVariableTable pLocalVariableTable) {
        super(pApi, pMethodVisitor);
        classVisitor = pClassVisitor;
        methodNode = pMethodNode;
        internalMethodName = pInternalMethodName;
        localVariableTable = pLocalVariableTable;
        classDescriptor = String.format("L%s;", classVisitor.classExecutionData.getRelativePath());
    }

    @Override
    public void visitLineNumber(int line, Label start) {
        if (firstLine == -1) {
            if (methodNode.name.equals("<init>")) {
                firstLine = line;
            } else {
                firstLine += line;
            }
        }
        currentLineNumber = line;
        super.visitLineNumber(line, start);
    }

    @Override
    public void visitFrame(int type, int numLocal, Object[] local, int numStack, Object[] stack) {
        updateCurrentNode();
        super.visitFrame(type, numLocal, local, numStack, stack);
    }

    @Override
    public void visitInsn(int opcode) {
        updateCurrentNode();
        super.visitInsn(opcode);
    }

    @Override
    public void visitIntInsn(int opcode, int operand) {
        updateCurrentNode();
        super.visitIntInsn(opcode, operand);
    }

    @Override
    public void visitVarInsn(int opcode, int var) {
        updateCurrentNode();
        super.visitVarInsn(opcode, var);
    }

    @Override
    public void visitTypeInsn(int opcode, String type) {
        updateCurrentNode();
        super.visitTypeInsn(opcode, type);
    }

    @Override
    public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
        updateCurrentNode();
        super.visitFieldInsn(opcode, owner, name, descriptor);
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
        updateCurrentNode();
        super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
    }

    @Override
    public void visitInvokeDynamicInsn(String name, String descriptor, Handle bootstrapMethodHandle, Object... bootstrapMethodArguments) {
        updateCurrentNode();
        super.visitInvokeDynamicInsn(name, descriptor, bootstrapMethodHandle, bootstrapMethodArguments);
    }

    @Override
    public void visitJumpInsn(int opcode, Label label) {
        updateCurrentNode();
        super.visitJumpInsn(opcode, label);
    }

    @Override
    public void visitLdcInsn(Object value) {
        updateCurrentNode();
        super.visitLdcInsn(value);
    }

    @Override
    public void visitIincInsn(int var, int increment) {
        updateCurrentNode();
        super.visitIincInsn(var, increment);
    }

    @Override
    public void visitTableSwitchInsn(int min, int max, Label dflt, Label... labels) {
        updateCurrentNode();
        super.visitTableSwitchInsn(min, max, dflt, labels);
    }

    @Override
    public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
        updateCurrentNode();
        super.visitLookupSwitchInsn(dflt, keys, labels);
    }

    @Override
    public void visitMultiANewArrayInsn(String descriptor, int numDimensions) {
        updateCurrentNode();
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
            currentNode = currentNode.getNext();
        }
        currentInstructionIndex = methodNode.instructions.indexOf(currentNode);
    }

    protected VarInsnNode getOwnerNode(int pStartCounter) {
        int counter = pStartCounter;
        AbstractInsnNode abstractInsnNode = null;
        for (int i = 1; i <= counter; i++) {
            abstractInsnNode = methodNode.instructions.get(currentInstructionIndex - i);
            counter += recalculateCounter(abstractInsnNode);
        }
        return (VarInsnNode) abstractInsnNode;
    }

    protected CFGNode getOwnerNode(int pStartCounter, NavigableMap<Integer, CFGNode> pNodes) {
        int counter = pStartCounter;
        AbstractInsnNode abstractInsnNode;
        for (int i = 1; i <= counter; i++) {
            abstractInsnNode = methodNode.instructions.get(currentInstructionIndex - i);
            counter += recalculateCounter(abstractInsnNode);
        }

        return pNodes.get(currentInstructionIndex - counter);
    }

    protected int getInstructionIndex(int pStartCounter) {
        int counter = pStartCounter;
        AbstractInsnNode abstractInsnNode;
        for (int i = 1; i <= counter; i++) {
            abstractInsnNode = methodNode.instructions.get(currentInstructionIndex - i);
            counter += recalculateCounter(abstractInsnNode);
        }
        return currentInstructionIndex - counter;
    }

    private int recalculateCounter(AbstractInsnNode abstractInsnNode) {
        switch (abstractInsnNode.getOpcode()) {
            case PUTFIELD:
            case IADD:
            case ISUB:
            case IMUL:
            case IDIV:
                return 2;
            case GETFIELD:
                return 1;
            case INVOKEDYNAMIC:
            case INVOKEINTERFACE:
            case INVOKESPECIAL:
            case INVOKESTATIC:
            case INVOKEVIRTUAL:
                MethodInsnNode methodInsnNode = (MethodInsnNode) abstractInsnNode;
                int paramsCount = Type.getArgumentTypes(methodInsnNode.desc).length;
                if(methodInsnNode.name.contains("<init>")) {
                    return 2 + paramsCount;
                } else {
                    return 1 + paramsCount;
                }
            default:
                return 0;
        }
    }

    protected boolean isLocalVariableReferenceToField(final int pIndex, final String pDescriptor) {
        if (!isSimpleType(pDescriptor)) {
            for (AbstractInsnNode abstractInsnNode : methodNode.instructions) {
                if (abstractInsnNode instanceof VarInsnNode) {
                    VarInsnNode varInsnNode = (VarInsnNode) abstractInsnNode;
                    if (varInsnNode.getOpcode() == Opcodes.ASTORE && varInsnNode.var == pIndex) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private boolean isSimpleType(final String pDescriptor) {
        return pDescriptor.equals("I")
                || pDescriptor.equals("D")
                || pDescriptor.equals("F")
                || pDescriptor.equals("L")
                || pDescriptor.equals("Ljava/lang/String;");
    }

    protected ProgramVariable getProgramVariableFromLocalVar(final int varNumber,
                                                             final int pOpcode,
                                                             final int pIndex,
                                                             final int pLineNumber) {
        final String varName = getVariableNameFromLocalVariablesTable(varNumber);
        final String varType = getVariableTypeFromLocalVariablesTable(varNumber);
        final boolean isDefinition = isDefinition(pOpcode);
        return ProgramVariable.create(null, varName, varType, pIndex, pLineNumber, false, isDefinition);
    }

    private String getVariableNameFromLocalVariablesTable(final int pVarNumber) {
        final Optional<LocalVariable> localVariable = localVariableTable.getEntry(pVarNumber);
        if (localVariable.isPresent()) {
            return localVariable.get().getName();
        } else {
            return String.valueOf(pVarNumber);
        }
    }

    private String getVariableTypeFromLocalVariablesTable(final int pVarNumber) {
        final Optional<LocalVariable> localVariable = localVariableTable.getEntry(pVarNumber);
        if (localVariable.isPresent()) {
            return localVariable.get().getDescriptor();
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
