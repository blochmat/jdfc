package com.jdfc.core.analysis.ifg;


import com.google.common.base.Preconditions;
import com.jdfc.core.analysis.data.CoverageDataExport;
import com.jdfc.core.analysis.data.CoverageDataStore;
import com.jdfc.core.analysis.data.ClassExecutionData;
import com.jdfc.core.analysis.ifg.data.*;

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
    private boolean isImpure;

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

    @Override
    public void setImpure() {
        this.isImpure = true;
    }

    ;

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
                workList.addAll(node.getPredecessors());
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
            ProgramVariable programVariable =
                    ProgramVariable.create(null, variable.getName(), variable.getDescriptor(),
                            pInsnIndex, pLineNumber, false);
            programVariable.setReference(isHolder(pData, programVariable));
            return programVariable;
        }
        return null;
    }

    static boolean isHolder(final ClassExecutionData pData,
                            final ProgramVariable pVariable) {
        for (InstanceVariable element : pData.getInstanceVariables()) {
            ProgramVariable holder = element.getHolder();
            if (holder.getOwner() == null && pVariable.getOwner() == null
                    && holder.getName().equals(pVariable.getName())
                    && holder.getDescriptor().equals(pVariable.getDescriptor())
                    && holder.getLineNumber() == pVariable.getLineNumber()
                    && holder.getInstructionIndex() == pVariable.getInstructionIndex()) {
                return true;
            }
        }
        return false;
    }

    static ProgramVariable prepareNewInstanceVarEntry(final ClassExecutionData pData,
                                                      final String pOwner,
                                                      final String pVarName,
                                                      final String pVarDesc,
                                                      final int pInstructionIndex,
                                                      final int pLineNumber) {
        if (pData != null) {
            Set<InstanceVariable> instanceVariables = pData.getInstanceVariables();
            InstanceVariable variable = findInstanceVariable(instanceVariables, pOwner, pVarName, pVarDesc);
            if (variable != null) {
                return ProgramVariable.create(variable.getOwner(), variable.getName(), variable.getDescriptor(), pInstructionIndex, pLineNumber, false);
            }
        }
        return null;
    }

    private static void addCoveredEntry(String methodNameDesc, ClassExecutionData classExecutionData, ProgramVariable programVariable) {
        if (programVariable != null) {
            Map<String, Set<ProgramVariable>> coveredList = classExecutionData.getVariablesCovered();
            coveredList.get(methodNameDesc).add(programVariable);
        }
    }

    static LocalVariable findLocalVariable(LocalVariableTable table, int index) {
        Optional<LocalVariable> o = table.getEntry(index);
        return o.orElse(null);
    }

    static InstanceVariable findInstanceVariable(final Set<InstanceVariable> pInstanceVariables,
                                                 final String pOwner,
                                                 final String pVarName,
                                                 final String pVarDesc) {
        for (InstanceVariable variable : pInstanceVariables) {
            if (variable.getOwner().equals(pOwner)
                    && variable.getName().equals(pVarName)
                    && variable.getDescriptor().equals(pVarDesc)) {
                return variable;
            }
        }
        return null;
    }
}
