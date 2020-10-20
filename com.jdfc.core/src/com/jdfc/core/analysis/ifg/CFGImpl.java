package com.jdfc.core.analysis.ifg;


import com.google.common.base.Preconditions;
import com.jdfc.core.analysis.data.CoverageDataExport;
import com.jdfc.core.analysis.data.CoverageDataStore;
import com.jdfc.core.analysis.data.ClassExecutionData;
import com.jdfc.core.analysis.ifg.data.InstanceVariable;
import com.jdfc.core.analysis.ifg.data.LocalVariable;
import com.jdfc.core.analysis.ifg.data.LocalVariableTable;
import com.jdfc.core.analysis.ifg.data.ProgramVariable;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import java.util.*;

/**
 * A implementation of a {@link CFG}.
 */
public class CFGImpl implements CFG {

    private final String methodName;
    private final NavigableMap<Integer, CFGNode> nodes;
    private final LocalVariableTable localVariableTable;
    private final boolean isImpure;

    CFGImpl(
            final String pMethodName,
            final NavigableMap<Integer, CFGNode> pNodes,
            final LocalVariableTable pLocalVariableTable,
            final boolean pIsImpure) {
        Preconditions.checkNotNull(pMethodName);
        Preconditions.checkNotNull(pNodes);
        methodName = pMethodName;
        nodes = pNodes;
        localVariableTable = pLocalVariableTable;
        isImpure = pIsImpure;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NavigableMap<Integer, CFGNode> getNodes() {
        return nodes;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public LocalVariableTable getLocalVariableTable() {
        return localVariableTable;
    }

    @Override
    public boolean isImpure() {
        return isImpure;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void calculateReachingDefinitions() {
        LinkedList<CFGNode> workList = new LinkedList<>();
        for (Map.Entry<Integer, CFGNode> node : nodes.entrySet()) {
            node.getValue().resetReachOut();
            workList.add(node.getValue());
        }

        while (!workList.isEmpty()) {
            CFGNode node = workList.poll();
            Set<ProgramVariable> oldValue = node.getReachOut();
            node.update();
            if (!node.getReachOut().equals(oldValue)) {
                node.getSuccessors().forEach(workList::addLast);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return String.format("CFGImpl for method %s (containing %d nodes)", methodName, nodes.size());
    }

    public static void addLocalVarCoveredEntry(final String pClassName,
                                               final String pMethodName,
                                               final String pMethodDesc,
                                               final int pVarIndex,
                                               final int pInsnIndex,
                                               final int pLineNumber) {
        String methodNameDesc = pMethodName.concat(": " + pMethodDesc);
        ClassExecutionData classExecutionData = (ClassExecutionData) CoverageDataStore.getInstance().findClassDataNode(pClassName).getData();
        ProgramVariable programVariable = prepareNewLocalVarEntry(classExecutionData, methodNameDesc, pVarIndex, pInsnIndex, pLineNumber);
        addCoveredEntry(methodNameDesc, classExecutionData, programVariable);
    }

    public static void addInstanceVarCoveredEntry(final String pClassName,
                                                  final String pOwner,
                                                  final String pMethodName,
                                                  final String pMethodDesc,
                                                  final String pVarName,
                                                  final String pVarDesc,
                                                  final int pIndex,
                                                  final int pLineNumber) {
        String methodNameDesc = pMethodName.concat(": " + pMethodDesc);
        ClassExecutionData classExecutionData = (ClassExecutionData) CoverageDataStore.getInstance().findClassDataNode(pClassName).getData();
        ProgramVariable programVariable = prepareNewInstanceVarEntry(classExecutionData, pOwner, pVarName, pVarDesc, pIndex, pLineNumber);
        addCoveredEntry(methodNameDesc, classExecutionData, programVariable);
    }

    public static void dumpClassExecutionDataToFile(final String pClassName) {
        ClassExecutionData classExecutionData = (ClassExecutionData) CoverageDataStore.getInstance().findClassDataNode(pClassName).getData();
        try {
            CoverageDataExport.dumpClassExecutionDataToFile(pClassName, classExecutionData);
        } catch (ParserConfigurationException | TransformerException e) {
            e.printStackTrace();
        }
    }

    static ProgramVariable prepareNewLocalVarEntry(final ClassExecutionData pData,
                                                   final String pMethodName,
                                                   final int pVarIndex,
                                                   final int pInsnIndex,
                                                   final int pLineNumber) {
        CFG cfg = pData.getMethodCFGs().get(pMethodName);
        LocalVariableTable table = cfg.getLocalVariableTable();
        LocalVariable variable = findLocalVariable(table, pVarIndex);
        if (variable != null) {
            return ProgramVariable.create(null, variable.getName(), variable.getDescriptor(), pInsnIndex, pLineNumber);
        }
        return null;
    }

    static ProgramVariable prepareNewInstanceVarEntry(final ClassExecutionData pClassExecutionData,
                                                      final String pOwner,
                                                      final String pVarName,
                                                      final String pVarDesc,
                                                      final int pInstructionIndex,
                                                      final int pLineNumber) {
        if(pClassExecutionData != null) {
            Set<InstanceVariable> set = pClassExecutionData.getInstanceVariables();
            InstanceVariable variable = findInstanceVariable(set, pOwner, pVarName, pVarDesc);
            if (variable != null) {
                return ProgramVariable.create(variable.getOwner(), variable.getName(), variable.getDescriptor(), pInstructionIndex, pLineNumber);
            }
        }
        return null;
    }

    private static void addCoveredEntry(String methodNameDesc, ClassExecutionData classExecutionData, ProgramVariable programVariable) {
        if (programVariable != null) {
            Map<String, Set<ProgramVariable>> coveredList = classExecutionData.getDefUseCovered();
            coveredList.get(methodNameDesc).add(programVariable);
        }
    }

    static LocalVariable findLocalVariable(LocalVariableTable table, int index) {
        Optional<LocalVariable> o = table.getEntry(index);
        return o.orElse(null);
    }

    static InstanceVariable findInstanceVariable(Set<InstanceVariable> pSet, String pOwner, String pVarName, String pVarDesc) {
        for (InstanceVariable variable : pSet) {
            if (variable.getOwner().equals(pOwner)
                    && variable.getName().equals(pVarName)
                    && variable.getDescriptor().equals(pVarDesc)) {
                return variable;
            }
        }
        return null;
    }
}
