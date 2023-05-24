package instr.methodVisitors;

import icfg.data.LocalVariable;
import icfg.data.ProgramVariable;
import instr.classVisitors.JDFCClassVisitor;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.HashMap;
import java.util.Map;

import static org.objectweb.asm.Opcodes.*;

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
        super.visitFrame(type, numLocal, local, numStack, stack);
    }

    @Override
    public void visitInsn(int opcode) {
        updateCurrentNode();
        visitFrameNew();
        super.visitInsn(opcode);
    }

    @Override
    public void visitIntInsn(int opcode, int operand) {
        updateCurrentNode();
        visitFrameNew();
        super.visitIntInsn(opcode, operand);
    }

    @Override
    public void visitVarInsn(int opcode, int var) {
        updateCurrentNode();
        visitFrameNew();
        super.visitVarInsn(opcode, var);
    }

    @Override
    public void visitTypeInsn(int opcode, String type) {
        updateCurrentNode();
        visitFrameNew();
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
        visitFrameNew();
        super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
    }

    @Override
    public void visitInvokeDynamicInsn(String name, String descriptor, Handle bootstrapMethodHandle, Object... bootstrapMethodArguments) {
        updateCurrentNode();
        visitFrameNew();
        super.visitInvokeDynamicInsn(name, descriptor, bootstrapMethodHandle, bootstrapMethodArguments);
    }

    @Override
    public void visitJumpInsn(int opcode, Label label) {
        updateCurrentNode();
        visitFrameNew();
        super.visitJumpInsn(opcode, label);
    }

    @Override
    public void visitLdcInsn(Object value) {
        updateCurrentNode();
        visitFrameNew();
        super.visitLdcInsn(value);
    }

    @Override
    public void visitIincInsn(int var, int increment) {
        updateCurrentNode();
        visitFrameNew();
        super.visitIincInsn(var, increment);
    }

    @Override
    public void visitTableSwitchInsn(int min, int max, Label dflt, Label... labels) {
        updateCurrentNode();
        visitFrameNew();
        super.visitTableSwitchInsn(min, max, dflt, labels);
    }

    @Override
    public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
        updateCurrentNode();
        visitFrameNew();
        super.visitLookupSwitchInsn(dflt, keys, labels);
    }

    @Override
    public void visitMultiANewArrayInsn(String descriptor, int numDimensions) {
        updateCurrentNode();
        visitFrameNew();
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
                return true;
            default:
                return false;
        }
    }
}
