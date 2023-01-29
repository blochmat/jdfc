package com.jdfc.core.analysis.ifg;

import com.google.common.base.Preconditions;
import com.jdfc.core.analysis.ifg.data.*;
import java.util.*;

/**
 * A implementation of a {@link CFG}.
 */
public class CFGImpl implements CFG {

    private final String methodName;
    private final NavigableMap<Integer, CFGNode> nodes;
    private final Map<Integer, LocalVariable> localVariableTable;
    private boolean isImpure;

    CFGImpl(
            final String pMethodName,
            final NavigableMap<Integer, CFGNode> pNodes,
            final Map<Integer, LocalVariable> pLocalVariableTable,
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
    public Map<Integer, LocalVariable> getLocalVariableTable() {
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
}
