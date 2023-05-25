package icfg;

import com.google.common.base.Preconditions;
import icfg.data.LocalVariable;
import icfg.data.ProgramVariable;
import icfg.nodes.ICFGNode;
import utils.JDFCUtils;

import java.util.LinkedList;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;

/**
 * A implementation of a {@link ICFG}.
 */
public class ICFGImpl implements ICFG {

    private final String methodName;
    private final NavigableMap<Double, ICFGNode> nodes;
    private final Map<Integer, LocalVariable> localVariableTable;
    private boolean isImpure;

    public ICFGImpl(
            final String pMethodName,
            final NavigableMap<Double, ICFGNode> pNodes,
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
    public NavigableMap<Double, ICFGNode> getNodes() {
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
        LinkedList<ICFGNode> workList = new LinkedList<>();
        for (Map.Entry<Double, ICFGNode> node : nodes.entrySet()) {
            node.getValue().resetReachOut();
            workList.add(node.getValue());
        }

        while (!workList.isEmpty()) {
            ICFGNode node = workList.poll();
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
