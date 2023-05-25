package cfg;

import cfg.nodes.CFGNode;
import com.google.common.base.Preconditions;
import com.google.common.collect.Multimap;
import cfg.data.LocalVariable;
import data.ProgramVariable;
import utils.JDFCUtils;

import java.util.LinkedList;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;

/**
 * A implementation of a {@link CFG}.
 */
public class CFGImpl implements CFG {

    private final String methodName;
    private final NavigableMap<Double, CFGNode> nodes;
    private final Multimap<Double, Double> edges;
    private final Map<Integer, LocalVariable> localVariableTable;
    private boolean isImpure;

    public CFGImpl(
            final String pMethodName,
            final NavigableMap<Double, CFGNode> pNodes,
            final Multimap<Double, Double> edges,
            final Map<Integer, LocalVariable> pLocalVariableTable,
            final boolean pIsImpure) {
        Preconditions.checkNotNull(pMethodName);
        Preconditions.checkNotNull(pNodes);
        methodName = pMethodName;
        nodes = pNodes;
        this.edges = edges;
        localVariableTable = pLocalVariableTable;
        isImpure = pIsImpure;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NavigableMap<Double, CFGNode> getNodes() {
        return nodes;
    }

    @Override
    public Multimap<Double, Double> getEdges() { return edges; }

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
        for (Map.Entry<Double, CFGNode> node : nodes.entrySet()) {
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
        return String.format("ICFGImpl for method %s (containing %d nodes)%n %s", methodName, nodes.size(), JDFCUtils.prettyPrintMap(nodes));
    }
}
