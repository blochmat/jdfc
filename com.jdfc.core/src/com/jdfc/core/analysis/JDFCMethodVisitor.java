package com.jdfc.core.analysis;

import com.jdfc.commons.utils.PrettyPrintMap;
import com.jdfc.core.analysis.ifg.CFGNode;
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

    protected VarInsnNode getOwnerNode(int pStartCounter) {
        return computeOwnerNode();

//        int counter = pStartCounter;
//        int as = 0;
//        for (AbstractInsnNode node : methodNode.instructions) {
//            System.out.println(as + " " + node.getOpcode());
//            as++;
//        }
//        System.out.println("ConstructionIndex:" + currentInstructionIndex);
////        System.out.println("Counter: " + counter);
//        counter = checkForConstants(counter);
//        AbstractInsnNode abstractInsnNode = null;
//        for (int i = 1; i <= counter; i++) {
//            abstractInsnNode = methodNode.instructions.get(currentInstructionIndex - i);
////            System.out.println("Add: " + recalculateCounter(abstractInsnNode));
//            counter += recalculateCounter(abstractInsnNode);
//        }
//        System.out.println("OPCODE: " + abstractInsnNode.getOpcode());
//        System.out.println("indexOf: " + methodNode.instructions.indexOf(abstractInsnNode));
//        System.out.println("getInstructionIndex: " + getInstructionIndex(pStartCounter));
//        return (VarInsnNode) abstractInsnNode;
    }

    private VarInsnNode computeOwnerNode() {
//        System.out.println("ComputeOwnerNode");
        boolean treeSaturated = false;
        int counter = 0;
        AbstractInsnNode startInsn = methodNode.instructions.get(currentInstructionIndex);
        InstructionNode root = new InstructionNode(new Instruction(counter, startInsn, getCapacity(startInsn)));

        // am ende root.get(counter) aufrufen um owner zu bekommen
        if (root.getInstruction().capacity != 0) {
            AbstractInsnNode currentInsn = startInsn;
            while (!treeSaturated) {
                currentInsn = currentInsn.getPrevious();
                counter++;
                int opcode = currentInsn.getOpcode();
                int index = methodNode.instructions.indexOf(currentInsn);
                InstructionNode newNode = new InstructionNode(new Instruction(counter, currentInsn, getCapacity(currentInsn)));
                if (InstructionNode.insertLeftRecursive(root, newNode)) {
//                    System.out.println("Opcode: " + opcode);
//                    System.out.println("Index: " + index);
//                    System.out.println("Counter: " + counter);
                    treeSaturated = InstructionNode.checkSaturationRecursive(root);
                } else {
                    throw new NullPointerException("Insertion failed.");
                }
            }
        }

        return (VarInsnNode) root.getChildren().get(counter).getInstruction().insnNode;
    }

    private static class Instruction {
        private final int index;
        private final AbstractInsnNode insnNode;
        private final int capacity;

        protected Instruction(final int pIndex,
                              final AbstractInsnNode pInsnNode,
                              final int pCapacity) {
            index = pIndex;
            insnNode = pInsnNode;
            capacity = pCapacity;
        }

        public String toString() {
            return String.format("Instruction { Idx: %s, Opcode: %s, Capacity: %s } \n", index, insnNode.getOpcode(), capacity);
        }
    }

    private static class InstructionNode {

        private final Map<Integer, InstructionNode> children = new HashMap<>();
        private final Instruction instruction;


        public InstructionNode(Instruction pInstruction) {
            instruction = pInstruction;
        }

        public Map<Integer, InstructionNode> getChildren() {
            return children;
        }

        public Instruction getInstruction() {
            return instruction;
        }

        public void addChild(Integer key, InstructionNode child) {
            this.children.put(key, child);
        }

        public String toString() {
            return String.format("InstructionNode { %s, %s } \n", instruction.toString(), new PrettyPrintMap<>(children));
        }

        public boolean isCapacityReached() {
            return instruction.capacity <= children.size();
        }

        public boolean isParent() {
            return instruction.capacity > 0;
        }

        public boolean isHavingChildren() {
            return !children.isEmpty();
        }

        public static boolean insertLeftRecursive(InstructionNode pNode, InstructionNode pNew) {
            boolean isInserted = false;

            if(!pNode.isParent()) {
                return false;
            }

            if(pNode.isParent() && !pNode.isHavingChildren()) {
                pNode.addChild(pNew.getInstruction().index, pNew);
                return true;
            }

            if(pNode.isParent() && pNode.isHavingChildren()) {
                for (InstructionNode child : pNode.getChildren().values()) {
                    isInserted = insertLeftRecursive(child, pNew);
                    if (isInserted) {
                        break;
                    }

                    if (!pNode.isCapacityReached()) {
                        pNode.addChild(pNew.getInstruction().index, pNew);
                        return true;
                    }

                }
            }
            return isInserted;
        }

        public static boolean checkSaturationRecursive(InstructionNode pNode) {
            boolean result;
            if (pNode.getChildren().size() == pNode.getInstruction().capacity) {
                result = true;
                for (Map.Entry<Integer, InstructionNode> entry : pNode.getChildren().entrySet()) {
                    result = result && checkSaturationRecursive(entry.getValue());
                }
            } else {
                return false;
            }
            return result;
        }
    }

    private int getCapacity(final AbstractInsnNode pInsnNode) {
        MethodInsnNode methodInsnNode;
        int paramsCount;
        switch (pInsnNode.getOpcode()) {
            case IADD:
            case ISUB:
            case IMUL:
            case IDIV:
            case DADD:
            case DSUB:
            case DMUL:
            case DDIV:
            case FADD:
            case FSUB:
            case FMUL:
            case FDIV:
            case LADD:
            case LSUB:
            case LMUL:
            case LDIV:
            case DCMPG:
            case DCMPL:
            case PUTFIELD:
            case DALOAD:
            case IALOAD:
            case FALOAD:
            case BALOAD:
            case AALOAD:
                return 2;
            case GETFIELD:
            case NEWARRAY:
            case ANEWARRAY:
            case CHECKCAST:
            case F_NEW:
            case I2D:
            case I2B:
            case I2C:
            case I2F:
            case I2S:
            case IFEQ:
            case IFGE:
            case IFGT:
            case IFLE:
            case IFLT:
            case IFNE:
            case IFNONNULL:
            case IFNULL:
            case GOTO:
                return 1;
            case INVOKESTATIC:
                methodInsnNode = (MethodInsnNode) pInsnNode;
                paramsCount = Type.getArgumentTypes(methodInsnNode.desc).length;
                return paramsCount;
            case INVOKEDYNAMIC:
            case INVOKEINTERFACE:
            case INVOKESPECIAL:
            case INVOKEVIRTUAL:
                methodInsnNode = (MethodInsnNode) pInsnNode;
                paramsCount = Type.getArgumentTypes(methodInsnNode.desc).length;
                if (methodInsnNode.name.contains("<init>")) {
                    paramsCount = Type.getArgumentTypes(methodInsnNode.desc).length;
                    return 2 + paramsCount;
                }
                return 1 + paramsCount;
            default:
                return 0;
        }
    }


//    protected CFGNode getOwnerNode(int pStartCounter, NavigableMap<Integer, CFGNode> pNodes) {
//        int counter = pStartCounter;
//        AbstractInsnNode abstractInsnNode;
//        for (int i = 1; i <= counter; i++) {
//            abstractInsnNode = methodNode.instructions.get(currentInstructionIndex - i);
//            counter += recalculateCounter(abstractInsnNode);
//        }
//
//        return pNodes.get(currentInstructionIndex - counter);
//    }
//
//    protected int getInstructionIndex(int pStartCounter) {
//        int counter = pStartCounter;
//        AbstractInsnNode abstractInsnNode;
//        for (int i = 1; i <= counter; i++) {
//            abstractInsnNode = methodNode.instructions.get(currentInstructionIndex - i);
//            counter += recalculateCounter(abstractInsnNode);
//        }
//        return currentInstructionIndex - counter;
//    }
//
//    private int recalculateCounter(AbstractInsnNode abstractInsnNode) {
////        System.out.println(abstractInsnNode.getOpcode());
//        MethodInsnNode methodInsnNode;
//        int paramsCount;
//        switch (abstractInsnNode.getOpcode()) {
//            case IADD:
//            case ISUB:
//            case IMUL:
//            case IDIV:
//            case DADD:
//            case DSUB:
//            case DMUL:
//            case DDIV:
//            case FADD:
//            case FSUB:
//            case FMUL:
//            case FDIV:
//            case LADD:
//            case LSUB:
//            case LMUL:
//            case LDIV:
//            case DCMPG:
//            case DCMPL:
//                if (isComputingFromBinaryOperatorResults()) {
//                    return 1;
//                } else {
//                    return 2;
//                }
//            case PUTFIELD:
//            case DALOAD:
//            case IALOAD:
//            case FALOAD:
//            case BALOAD:
//            case AALOAD:
//                return 2;
//            case GETFIELD:
//                System.out.println("asdf");
//                System.out.println(abstractInsnNode.getPrevious().getOpcode());
//                if (abstractInsnNode.getPrevious().getOpcode() == DUP) {
//                    return 0;
//                } else {
//                    return 1;
//                }
//            case NEWARRAY:
//            case ANEWARRAY:
//            case CHECKCAST:
//            case F_NEW:
//            case I2D:
//            case I2B:
//            case I2C:
//            case I2F:
//            case I2S:
//            case DUP:
//            case IFEQ:
//            case IFGE:
//            case IFGT:
//            case IFLE:
//            case IFLT:
//            case IFNE:
//            case IFNONNULL:
//            case IFNULL:
//            case GOTO:
//            case LDC:
//            case ICONST_0:
//            case ICONST_1:
//            case ICONST_2:
//            case ICONST_3:
//            case ICONST_4:
//            case ICONST_5:
//            case ICONST_M1:
//            case DCONST_0:
//            case DCONST_1:
//            case FCONST_0:
//            case FCONST_1:
//            case FCONST_2:
//            case ACONST_NULL:
//            case LCONST_0:
//            case LCONST_1:
//                return 1;
//            case INVOKESTATIC:
//                methodInsnNode = (MethodInsnNode) abstractInsnNode;
//                paramsCount = Type.getArgumentTypes(methodInsnNode.desc).length;
//                return paramsCount;
//            case INVOKEDYNAMIC:
//            case INVOKEINTERFACE:
//            case INVOKESPECIAL:
//            case INVOKEVIRTUAL:
//                methodInsnNode = (MethodInsnNode) abstractInsnNode;
//                paramsCount = Type.getArgumentTypes(methodInsnNode.desc).length;
//                return 1 + paramsCount;
//            default:
//                return 0;
//        }
//    }
//
//    protected boolean isComputingFromBinaryOperatorResults() {
//        boolean result = true;
//        for (int i = 1; i <= 2; i++) {
//            switch (methodNode.instructions.get(currentInstructionIndex - i).getOpcode()) {
//                case IADD:
//                case ISUB:
//                case IMUL:
//                case IDIV:
//                case DADD:
//                case DSUB:
//                case DMUL:
//                case DDIV:
//                case FADD:
//                case FSUB:
//                case FMUL:
//                case FDIV:
//                case LADD:
//                case LSUB:
//                case LMUL:
//                case LDIV:
//                    break;
//                default:
//                    result = false;
//            }
//        }
//        return result;
//    }
//
//    protected int checkForConstants(int pParamsCount) {
//        int newParamsCount = pParamsCount;
//        for (int i = 1; i <= pParamsCount; i++) {
//            switch (methodNode.instructions.get(currentInstructionIndex - i).getOpcode()) {
//                case LDC:
//                case ICONST_0:
//                case ICONST_1:
//                case ICONST_2:
//                case ICONST_3:
//                case ICONST_4:
//                case ICONST_5:
//                case ICONST_M1:
//                case DCONST_0:
//                case DCONST_1:
//                case FCONST_0:
//                case FCONST_1:
//                case FCONST_2:
//                case ACONST_NULL:
//                case LCONST_0:
//                case LCONST_1:
//                    newParamsCount--;
//            }
//        }
//        return newParamsCount;
//    }

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
                                                             final String internalMethodName,
                                                             final int pIndex,
                                                             final int pLineNumber) {
        final String varName = getVariableNameFromLocalVariablesTable(varNumber);
        final String varType = getVariableTypeFromLocalVariablesTable(varNumber);
        final boolean isDefinition = isDefinition(pOpcode);
        return ProgramVariable.create(null, varName, varType, internalMethodName,
                pIndex, pLineNumber, false, isDefinition);
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
