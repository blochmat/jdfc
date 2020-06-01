package main.java.com.jdfc.core.cfg;


import com.google.common.base.Preconditions;
import main.java.com.jdfc.core.CFGStorage;

import java.util.*;

/** A implementation of a {@link CFG}. */
public class CFGImpl implements CFG {

    private final String methodName;
    private final Map<Integer, CFGNode> nodes;
    private final LocalVariableTable localVariableTable;

    CFGImpl(
            final String pMethodName,
            final Map<Integer, CFGNode> pNodes,
            final LocalVariableTable pLocalVariableTable) {
        Preconditions.checkNotNull(pMethodName);
        Preconditions.checkNotNull(pNodes);
        methodName = pMethodName;
        nodes = pNodes;
        localVariableTable = pLocalVariableTable;
    }

    /** {@inheritDoc} */
    @Override
    public Map<Integer, CFGNode> getNodes() {
        return nodes;
    }

    /** {@inheritDoc} */
    @Override
    public LocalVariableTable getLocalVariableTable() {
        return localVariableTable;
    }

    /** {@inheritDoc} */
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

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return String.format("CFGImpl for method %s (containing %d nodes)", methodName, nodes.size());
    }

    public static void addCoveredEntry(
            String methodName, String methodDesc, int varIndex, int instructionIndex) {
        String methodNameDesc = methodName.concat(": " + methodDesc);
        ProgramVariable programVariable = prepareNewEntry(methodNameDesc, varIndex, instructionIndex);
        Map<String, Set<ProgramVariable>> coveredList = CFGStorage.INSTANCE.getDefUseCovered();
        coveredList.get(methodNameDesc).add(programVariable);
    }

    static ProgramVariable prepareNewEntry(String methodName, int varIndex, int instructionIndex) {
        CFG cfg = CFGStorage.INSTANCE.getMethodCFGs().get(methodName);
        LocalVariableTable table = cfg.getLocalVariableTable();
        LocalVariable variable = findLocalVariable(table, varIndex);
        return ProgramVariable.create(variable.getName(), variable.getDescriptor(), instructionIndex);
    }

    static LocalVariable findLocalVariable(LocalVariableTable table, int index) {
        Optional<LocalVariable> o = table.getEntry(index);
        return o.orElse(null);
    }
}

