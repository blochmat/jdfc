package com.jdfc.core.analysis.cfg;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.util.ListIterator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.objectweb.asm.Opcodes.*;

public class MethodCFGCreator {

    private final String methodName;
    private final MethodNode methodNode;
    private final LocalVariableTable localVariableTable;
    private final Multimap<Integer, Integer> edges;
    private final Map<Integer, CFGNode> nodes;
    private final Type[] parameterTypes;

    MethodCFGCreator(
            final MethodNode pMethodNode,
            final LocalVariableTable pLocalVariableTable,
            final Type[] pParameterTypes) {
        methodName = pMethodNode.name;
        methodNode = pMethodNode;
        localVariableTable = pLocalVariableTable;
        edges = ArrayListMultimap.create();
        nodes = Maps.newLinkedHashMap();
        parameterTypes = pParameterTypes;
    }

    CFG create() {
        final InsnList instructions = methodNode.instructions;
        for (AbstractInsnNode node : instructions) {
            createCFGNode(node, instructions.indexOf(node));
        }
        edges.putAll(createEdges());
        setPredecessorSuccessorRelation();
        addDummyStartNode();
        return new CFGImpl(methodName, nodes, localVariableTable);
    }

    private void createCFGNode(final AbstractInsnNode pNode, final int pIndex) {
        if (pNode instanceof VarInsnNode) {
            createCFGNodeForVarInsnNode((VarInsnNode) pNode, pIndex);
        } else if (pNode instanceof IincInsnNode) {
            createCFGNodeForIincInsnNode((IincInsnNode) pNode, pIndex);
        } else {
            final CFGNode node = new CFGNode(pIndex);
            nodes.put(pIndex, node);
        }
    }

    private void createCFGNodeForVarInsnNode(final VarInsnNode pNode, final int pIndex) {
        final int varNumber = pNode.var;
        final ProgramVariable programVariable = getProgramVariable(varNumber, pIndex);
        final CFGNode node;
        switch (pNode.getOpcode()) {
            case ISTORE:
            case LSTORE:
            case FSTORE:
            case DSTORE:
                node = new CFGNode(Sets.newHashSet(programVariable), Sets.newLinkedHashSet(), pIndex);
                break;
            case ILOAD:
            case LLOAD:
            case FLOAD:
            case DLOAD:
                node = new CFGNode(Sets.newLinkedHashSet(), Sets.newHashSet(programVariable), pIndex);
                break;
            default:
                node = new CFGNode(pIndex);
                break;
        }
        nodes.put(pIndex, node);
    }

    private ProgramVariable getProgramVariable(int varNumber, final int pInstructionIndex) {
        final String varName = getVariableNameFromLocalVariablesTable(varNumber);
        final String varType = getVariableTypeFromLocalVariablesTable(varNumber);
        return ProgramVariable.create(varName, varType, pInstructionIndex);
    }

    private void createCFGNodeForIincInsnNode(final IincInsnNode pNode, final int pIndex) {
        final int varNumber = pNode.var;
        final ProgramVariable programVariable = getProgramVariable(varNumber, pIndex);
        final CFGNode node =
                new CFGNode(Sets.newHashSet(programVariable), Sets.newHashSet(programVariable), pIndex);
        nodes.put(pIndex, node);
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

    private void setPredecessorSuccessorRelation() {
        for (Map.Entry<Integer, Integer> edge : edges.entries()) {
            final CFGNode first = nodes.get(edge.getKey());
            final CFGNode second = nodes.get(edge.getValue());

            first.addSuccessor(second);
            second.addPredecessor(first);
        }
    }

    private Multimap<Integer, Integer> createEdges() {
        CFGEdgeAnalyzationVisitor cfgEdgeAnalysationVisitor =
                new CFGEdgeAnalyzationVisitor(methodName, methodNode);
        methodNode.accept(cfgEdgeAnalysationVisitor);
        return cfgEdgeAnalysationVisitor.getEdges();
    }

    private void addDummyStartNode() {
        final Set<ProgramVariable> parameters = Sets.newLinkedHashSet();
        for (int i = 0; i < parameterTypes.length; i++) {
            final Optional<LocalVariable> parameterVariable = localVariableTable.getEntry(i + 1);
            if (parameterVariable.isPresent()) {
                final LocalVariable localVariable = parameterVariable.get();
                final ProgramVariable variable =
                        ProgramVariable.create(
                                localVariable.getName(), localVariable.getDescriptor(), Integer.MIN_VALUE);
                parameters.add(variable);
            }
        }

        final CFGNode firstNode = nodes.get(0);
        final CFGNode dummyStartNode =
                new CFGNode(
                        parameters,
                        Sets.newLinkedHashSet(),
                        Integer.MIN_VALUE,
                        Sets.newLinkedHashSet(),
                        Sets.newHashSet(firstNode));
        firstNode.addPredecessor(dummyStartNode);
        nodes.put(Integer.MIN_VALUE, dummyStartNode);
    }
}
