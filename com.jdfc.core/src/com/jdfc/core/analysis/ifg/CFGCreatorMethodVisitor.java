package com.jdfc.core.analysis.ifg;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.jdfc.core.analysis.JDFCMethodVisitor;
import com.jdfc.core.analysis.ifg.data.InstanceVariable;
import com.jdfc.core.analysis.ifg.data.LocalVariable;
import com.jdfc.core.analysis.ifg.data.LocalVariableTable;
import com.jdfc.core.analysis.ifg.data.ProgramVariable;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

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
                                   final LocalVariableTable pLocalVariableTable,
                                   final Type[] pParameterTypes) {
        super(ASM6, pClassVisitor, pMethodVisitor, pMethodNode, pInternalMethodName, pLocalVariableTable);
        methodCFGs = pMethodCFGs;
        parameterTypes = pParameterTypes;
        edges = ArrayListMultimap.create();
        nodes = Maps.newTreeMap();
    }

    @Override
    public void visitFrame(int type, int numLocal, Object[] local, int numStack, Object[] stack) {
        super.visitFrame(type, numLocal, local, numStack, stack);
        final CFGNode node = new CFGNode(currentInstructionIndex);
        nodes.put(currentInstructionIndex, node);
    }

    @Override
    public void visitInsn(int opcode) {
        super.visitInsn(opcode);
        final CFGNode node = new CFGNode(currentInstructionIndex);
        nodes.put(currentInstructionIndex, node);
    }

    @Override
    public void visitIntInsn(int opcode, int operand) {
        super.visitIntInsn(opcode, operand);
        final CFGNode node = new CFGNode(currentInstructionIndex);
        nodes.put(currentInstructionIndex, node);
    }

    @Override
    public void visitVarInsn(int opcode, int var) {
        super.visitVarInsn(opcode, var);
        createCFGNodeForVarInsnNode(opcode, var, currentInstructionIndex, currentLineNumber);
    }

    @Override
    public void visitTypeInsn(int opcode, String type) {
        super.visitTypeInsn(opcode, type);
        final CFGNode node = new CFGNode(currentInstructionIndex);
        nodes.put(currentInstructionIndex, node);
    }

    @Override
    public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
        super.visitFieldInsn(opcode, owner, name, descriptor);
        createCFGNodeForFieldInsnNode(opcode, owner, name, descriptor);
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
        super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
        String methodNameDesc = name.concat(": " + descriptor);
        if (owner.equals(classVisitor.classNode.name) && isInstrumentationRequired(methodNameDesc)) {
            int paramsCount = Type.getArgumentTypes(descriptor).length;
            VarInsnNode ownerNode = getOwnerNode(INVOKE_STANDARD + paramsCount);
            ProgramVariable caller =
                    getProgramVariableFromLocalVar(ownerNode.var, currentInstructionIndex, currentLineNumber);
            final IFGNode node = new IFGNode(currentInstructionIndex, currentLineNumber, caller, methodNameDesc, paramsCount);
            nodes.put(currentInstructionIndex, node);
        } else {
            final CFGNode node = new CFGNode(currentInstructionIndex);
            nodes.put(currentInstructionIndex, node);
        }
    }

    @Override
    public void visitInvokeDynamicInsn(String name, String descriptor, Handle bootstrapMethodHandle, Object... bootstrapMethodArguments) {
        super.visitInvokeDynamicInsn(name, descriptor, bootstrapMethodHandle, bootstrapMethodArguments);
        final CFGNode node = new CFGNode(currentInstructionIndex);
        nodes.put(currentInstructionIndex, node);
    }

    @Override
    public void visitJumpInsn(int opcode, Label label) {
        super.visitJumpInsn(opcode, label);
        final CFGNode node = new CFGNode(currentInstructionIndex);
        nodes.put(currentInstructionIndex, node);
    }

    @Override
    public void visitLdcInsn(Object value) {
        super.visitLdcInsn(value);
        final CFGNode node = new CFGNode(currentInstructionIndex);
        nodes.put(currentInstructionIndex, node);
    }

    @Override
    public void visitIincInsn(int var, int increment) {
        super.visitIincInsn(var, increment);
        createCFGNodeForIincInsnNode(var, currentInstructionIndex, currentLineNumber);
    }

    @Override
    public void visitTableSwitchInsn(int min, int max, Label dflt, Label... labels) {
        super.visitTableSwitchInsn(min, max, dflt, labels);
        final CFGNode node = new CFGNode(currentInstructionIndex);
        nodes.put(currentInstructionIndex, node);
    }

    @Override
    public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
        super.visitLookupSwitchInsn(dflt, keys, labels);
        final CFGNode node = new CFGNode(currentInstructionIndex);
        nodes.put(currentInstructionIndex, node);
    }

    @Override
    public void visitMultiANewArrayInsn(String descriptor, int numDimensions) {
        super.visitMultiANewArrayInsn(descriptor, numDimensions);
        final CFGNode node = new CFGNode(currentInstructionIndex);
        nodes.put(currentInstructionIndex, node);
    }

    @Override
    public void visitEnd() {
        super.visitEnd();
        edges.putAll(createEdges());
        setPredecessorSuccessorRelation();

        // Add all instance variables to dummy start node as they are defined for all methods
        addEntryNode(classVisitor.classExecutionData.getInstanceVariables());
        addExitNode();

        CFG cfg = new CFGImpl(methodNode.name, nodes, localVariableTable, isImpure);
        cfg.calculateReachingDefinitions();
        methodCFGs.put(internalMethodName, cfg);
        classVisitor.classExecutionData.getMethodFirstLine().put(internalMethodName, firstLine);
    }

    private void createCFGNodeForFieldInsnNode(final int pOpcode, String pOwner, String pName, String pDescriptor) {
        // TODO: getProgramVariableFromInstanceVariableOcc due to object owner information
        final ProgramVariable programVariable =
                ProgramVariable.create(pOwner, pName, pDescriptor, currentInstructionIndex, currentLineNumber);
//        System.out.printf("DEBUG TRACKING FIELD: \n%s\n", programVariable);
        final CFGNode node;
        switch (pOpcode) {
            case GETFIELD:
                node = new CFGNode(Sets.newLinkedHashSet(), Sets.newHashSet(programVariable), currentInstructionIndex);
                break;
            case PUTFIELD:
                if (pOwner.equals(classVisitor.classNode.name)) {
                    isImpure = true;
                }

                node = new CFGNode(Sets.newHashSet(programVariable), Sets.newLinkedHashSet(), currentInstructionIndex);
                break;
            default:
                node = new CFGNode(currentInstructionIndex);
                break;
        }
        nodes.put(currentInstructionIndex, node);
    }

    private void createCFGNodeForVarInsnNode(final int opcode, final int varNumber, final int pIndex, final int lineNumber) {
        final CFGNode node;
        final ProgramVariable programVariable = getProgramVariableFromLocalVar(varNumber, pIndex, lineNumber);
        switch (opcode) {
            case ISTORE:
            case LSTORE:
            case FSTORE:
            case DSTORE:
            case ASTORE:
                node = new CFGNode(Sets.newHashSet(programVariable), Sets.newLinkedHashSet(), pIndex);
                break;
            case ILOAD:
            case LLOAD:
            case FLOAD:
            case DLOAD:
            case ALOAD:
                node = new CFGNode(Sets.newLinkedHashSet(), Sets.newHashSet(programVariable), pIndex);
                break;
            default:
                node = new CFGNode(pIndex);
                break;
        }
        nodes.put(pIndex, node);
    }

    private void createCFGNodeForIincInsnNode(final int varNumber, final int pIndex, final int pLineNumber) {
        final ProgramVariable programVariable = getProgramVariableFromLocalVar(varNumber, pIndex, pLineNumber);
        final CFGNode node =
                new CFGNode(Sets.newHashSet(programVariable), Sets.newHashSet(programVariable), pIndex);
        nodes.put(pIndex, node);
    }

    private Multimap<Integer, Integer> createEdges() {
        CFGEdgeAnalyzationVisitor cfgEdgeAnalysationVisitor =
                new CFGEdgeAnalyzationVisitor(methodNode.name, methodNode);
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

    private void addEntryNode(Set<InstanceVariable> pInstanceVariables) {
        final Set<ProgramVariable> parameters = Sets.newLinkedHashSet();
        for (int i = 0; i <= parameterTypes.length; i++) {
            final Optional<LocalVariable> parameterVariable = localVariableTable.getEntry(i);
            if (parameterVariable.isPresent()) {
                final LocalVariable localVariable = parameterVariable.get();
                final ProgramVariable variable =
                        ProgramVariable.create(null,
                                localVariable.getName(),
                                localVariable.getDescriptor(),
                                Integer.MIN_VALUE,
                                firstLine);
                parameters.add(variable);
            }
        }

        for (InstanceVariable instanceVariable : pInstanceVariables) {
            final ProgramVariable variable =
                    ProgramVariable.create(
                            instanceVariable.getOwner(),
                            instanceVariable.getName(),
                            instanceVariable.getDescriptor(),
                            Integer.MIN_VALUE,
                            instanceVariable.getLineNumber());
            parameters.add(variable);
        }

        final CFGNode firstNode = nodes.get(0);
        final CFGNode entryNode =
                new CFGNode(
                        parameters,
                        Sets.newLinkedHashSet(),
                        Integer.MIN_VALUE,
                        Sets.newLinkedHashSet(),
                        Sets.newHashSet(firstNode));
        firstNode.addPredecessor(entryNode);
        nodes.put(Integer.MIN_VALUE, entryNode);
    }

    private void addExitNode() {
        final CFGNode lastNode = nodes.get(currentInstructionIndex);
        final CFGNode exitNode = new CFGNode(Integer.MAX_VALUE);
        lastNode.addSuccessor(exitNode);
        nodes.put(Integer.MAX_VALUE, exitNode);
    }
}
