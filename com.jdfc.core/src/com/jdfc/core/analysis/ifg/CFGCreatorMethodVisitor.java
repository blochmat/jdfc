package com.jdfc.core.analysis.ifg;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.jdfc.core.analysis.JDFCMethodVisitor;
import com.jdfc.core.analysis.ifg.data.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.MethodNode;

import java.util.*;

import static org.objectweb.asm.Opcodes.*;

class CFGCreatorMethodVisitor extends JDFCMethodVisitor {

    private final Map<String, CFG> methodCFGs;
    private final Type[] parameterTypes;
    private final Multimap<Integer, Integer> edges;
    private final NavigableMap<Integer, CFGNode> nodes;
    private boolean isImpure = false;

    public CFGCreatorMethodVisitor(final CFGCreatorClassVisitor pClassVisitor,
                                   final MethodVisitor pMethodVisitor,
                                   final MethodNode pMethodNode,
                                   final String pInternalMethodName,
                                   final Map<String, CFG> pMethodCFGs,
                                   final Map<Integer, LocalVariable> pLocalVariableTable,
                                   final Type[] pParameterTypes) {
        super(ASM5, pClassVisitor, pMethodVisitor, pMethodNode, pInternalMethodName, pLocalVariableTable);
        methodCFGs = pMethodCFGs;
        parameterTypes = pParameterTypes;
        edges = ArrayListMultimap.create();
        nodes = Maps.newTreeMap();
    }

    @Override
    public void visitFrame(int type, int numLocal, Object[] local, int numStack, Object[] stack) {
        super.visitFrame(type, numLocal, local, numStack, stack);
        final CFGNode node = new CFGNode(currentInstructionIndex, getFrameOpcode(type));
        nodes.put(currentInstructionIndex, node);
    }

    private int getFrameOpcode(int type) {
        switch (type) {
            case -1:
                return F_NEW;
            case 0:
                return F_FULL;
            case 1:
                return F_APPEND;
            case 2:
                return F_CHOP;
            case 3:
                return F_SAME;
            case 4:
                return F_SAME1;
            default:
                return Integer.MIN_VALUE;
        }
    }

    @Override
    public void visitInsn(int opcode) {
        super.visitInsn(opcode);
        visitFrameNew();
        final CFGNode node = new CFGNode(currentInstructionIndex, opcode);
        nodes.put(currentInstructionIndex, node);
    }

    @Override
    public void visitFrameNew() {
        if (currentNode.getOpcode() == F_NEW) {
            final CFGNode node = new CFGNode(currentInstructionIndex, F_NEW);
            nodes.put(currentInstructionIndex, node);
            updateCurrentNode();
        }
    }

    @Override
    public void visitIntInsn(int opcode, int operand) {
        super.visitIntInsn(opcode, operand);
        visitFrameNew();
        final CFGNode node = new CFGNode(currentInstructionIndex, opcode);
        nodes.put(currentInstructionIndex, node);
    }

    @Override
    public void visitVarInsn(int opcode, int var) {
        super.visitVarInsn(opcode, var);
        visitFrameNew();
        createCFGNodeForVarInsnNode(opcode, var, currentInstructionIndex, currentLineNumber);
    }

    @Override
    public void visitTypeInsn(int opcode, String type) {
        super.visitTypeInsn(opcode, type);
        visitFrameNew();
        final CFGNode node = new CFGNode(currentInstructionIndex, opcode);
        nodes.put(currentInstructionIndex, node);
    }

    @Override
    public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
        super.visitFieldInsn(opcode, owner, name, descriptor);
        visitFrameNew();
        createCFGNodeForFieldInsnNode(opcode, owner, name, descriptor);
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
        super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
        visitFrameNew();
//        System.out.printf("DEBUG visitMethodInsn %s %s %s %s %s\n", classVisitor.classNode.name, methodNode.name, name, currentInstructionIndex, currentLineNumber);
        if (owner.equals(classVisitor.classNode.name) && isInstrumentationRequired(internalMethodName)) {
            String callSiteMethodName = computeInternalMethodName(name, descriptor);
            int paramsCount = (int) Arrays.stream(Type.getArgumentTypes(descriptor)).filter(x -> !x.toString().equals("[")).count();
            final IFGNode node = new IFGNode(currentInstructionIndex, currentLineNumber, opcode, owner, null, callSiteMethodName, paramsCount);
            nodes.put(currentInstructionIndex, node);

        } else {
            final CFGNode node = new CFGNode(currentInstructionIndex, opcode);
            nodes.put(currentInstructionIndex, node);
        }
    }

    private String computeInternalMethodName(String name, String descriptor) {
        for (MethodNode node : classVisitor.classNode.methods) {
            if (node.name.equals(name) && node.desc.equals(descriptor)) {
                return CFGCreator.computeInternalMethodName(node.name, node.desc, node.signature, node.exceptions.toArray(new String[0]));
            }
        }
        return null;
    }

    @Override
    public void visitInvokeDynamicInsn(String name, String descriptor, Handle bootstrapMethodHandle, Object... bootstrapMethodArguments) {
        super.visitInvokeDynamicInsn(name, descriptor, bootstrapMethodHandle, bootstrapMethodArguments);
        visitFrameNew();
        final CFGNode node = new CFGNode(currentInstructionIndex, INVOKEDYNAMIC);
        nodes.put(currentInstructionIndex, node);
    }

    @Override
    public void visitJumpInsn(int opcode, Label label) {
        super.visitJumpInsn(opcode, label);
        visitFrameNew();
        final CFGNode node = new CFGNode(currentInstructionIndex, opcode);
        nodes.put(currentInstructionIndex, node);
    }

    @Override
    public void visitLdcInsn(Object value) {
        super.visitLdcInsn(value);
        visitFrameNew();
        final CFGNode node = new CFGNode(currentInstructionIndex, LDC);
        nodes.put(currentInstructionIndex, node);
    }

    @Override
    public void visitIincInsn(int var, int increment) {
        super.visitIincInsn(var, increment);
        visitFrameNew();
        createCFGNodeForIincInsnNode(var, currentInstructionIndex, currentLineNumber);
    }

    @Override
    public void visitTableSwitchInsn(int min, int max, Label dflt, Label... labels) {
        super.visitTableSwitchInsn(min, max, dflt, labels);
        visitFrameNew();
        final CFGNode node = new CFGNode(currentInstructionIndex, TABLESWITCH);
        nodes.put(currentInstructionIndex, node);
    }

    @Override
    public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
        super.visitLookupSwitchInsn(dflt, keys, labels);
        visitFrameNew();
        final CFGNode node = new CFGNode(currentInstructionIndex, LOOKUPSWITCH);
        nodes.put(currentInstructionIndex, node);
    }

    @Override
    public void visitMultiANewArrayInsn(String descriptor, int numDimensions) {
        super.visitMultiANewArrayInsn(descriptor, numDimensions);
        visitFrameNew();
        final CFGNode node = new CFGNode(currentInstructionIndex, MULTIANEWARRAY);
        nodes.put(currentInstructionIndex, node);
    }

    @Override
    public void visitEnd() {
        super.visitEnd();
        edges.putAll(createEdges());
        setPredecessorSuccessorRelation();

        addEntryNode();

        CFG cfg = new CFGImpl(internalMethodName, nodes, localVariableTable, isImpure);
        methodCFGs.put(internalMethodName, cfg);
        classVisitor.classExecutionData.getMethodFirstLine().put(internalMethodName, firstLine);
        classVisitor.classExecutionData.getMethodLastLine().put(internalMethodName, currentLineNumber);
    }

    private void createCFGNodeForFieldInsnNode(final int pOpcode, String pOwner, String pName, String pDescriptor) {
        final ProgramVariable programVariable;
        final CFGNode node;
        switch (pOpcode) {
            case GETFIELD:
                programVariable = ProgramVariable.create(pOwner, pName, pDescriptor,
                        currentInstructionIndex, currentLineNumber, false);
                node = new CFGNode(Sets.newLinkedHashSet(), Sets.newHashSet(programVariable), currentInstructionIndex, pOpcode);
                break;
            case PUTFIELD:
                if (pOwner.equals(classVisitor.classNode.name)) {
                    isImpure = true;
                }
                programVariable = ProgramVariable.create(pOwner, pName, pDescriptor,
                        currentInstructionIndex, currentLineNumber, true);
                node = new CFGNode(Sets.newHashSet(programVariable), Sets.newLinkedHashSet(), currentInstructionIndex, pOpcode);
                break;
            default:
                node = new CFGNode(currentInstructionIndex, pOpcode);
                break;
        }
        nodes.put(currentInstructionIndex, node);
    }

    private void createCFGNodeForVarInsnNode(final int opcode, final int varNumber, final int pIndex, final int lineNumber) {
        final CFGNode node;
        final ProgramVariable programVariable;
        switch (opcode) {
            case ISTORE:
            case LSTORE:
            case FSTORE:
            case DSTORE:
            case ASTORE:
                programVariable = getProgramVariableFromLocalVar(varNumber, opcode, pIndex, lineNumber);
                node = new CFGNode(Sets.newHashSet(programVariable), Sets.newLinkedHashSet(), pIndex, opcode);
                break;
            case ILOAD:
            case LLOAD:
            case FLOAD:
            case DLOAD:
            case ALOAD:
                programVariable = getProgramVariableFromLocalVar(varNumber, opcode, pIndex, lineNumber);
                node = new CFGNode(Sets.newLinkedHashSet(), Sets.newHashSet(programVariable), pIndex, opcode);
                break;
            default:
                node = new CFGNode(pIndex, opcode);
                break;
        }
        nodes.put(pIndex, node);
    }

    private void createCFGNodeForIincInsnNode(final int varNumber, final int pIndex, final int pLineNumber) {
        final ProgramVariable programVariable = getProgramVariableFromLocalVar(varNumber, ISTORE, pIndex, pLineNumber);
        final CFGNode node =
                new CFGNode(Sets.newHashSet(programVariable), Sets.newHashSet(programVariable), pIndex, IINC);
        nodes.put(pIndex, node);
    }

    private Multimap<Integer, Integer> createEdges() {
        CFGEdgeAnalyzationVisitor cfgEdgeAnalysationVisitor =
                new CFGEdgeAnalyzationVisitor(classVisitor.classNode.name, methodNode);
        methodNode.accept(cfgEdgeAnalysationVisitor);
        return cfgEdgeAnalysationVisitor.getEdges();
    }

    private void setPredecessorSuccessorRelation() {
        for (Map.Entry<Integer, Integer> edge : edges.entries()) {
            final CFGNode first = nodes.get(edge.getKey());
            final CFGNode second = nodes.get(edge.getValue());
            first.addSuccessor(second);
            second.addPredecessor(first);
        }
    }

    private void addEntryNode() {
        final Set<ProgramVariable> parameters = Sets.newLinkedHashSet();

        for (LocalVariable localVariable : localVariableTable.values()) {
            final ProgramVariable variable =
                    ProgramVariable.create(null,
                            localVariable.getName(),
                            localVariable.getDescriptor(),
                            Integer.MIN_VALUE,
                            firstLine,
                            true);
            parameters.add(variable);
        }

        final CFGNode firstNode = nodes.get(0);
        if (firstNode != null) {
            final CFGNode entryNode =
                    new CFGNode(
                            parameters,
                            Sets.newLinkedHashSet(),
                            Integer.MIN_VALUE,
                            Integer.MIN_VALUE,
                            Sets.newLinkedHashSet(),
                            Sets.newHashSet(firstNode));
            firstNode.addPredecessor(entryNode);
            nodes.put(Integer.MIN_VALUE, entryNode);
        }

    }
}
