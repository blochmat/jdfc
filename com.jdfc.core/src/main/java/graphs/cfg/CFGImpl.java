package graphs.cfg;

import com.google.common.base.Preconditions;
import com.google.common.collect.Multimap;
import data.ProgramVariable;
import graphs.cfg.nodes.CFGNode;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import utils.JDFCUtils;

import java.util.LinkedList;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;

/**
 * A implementation of a {@link CFG}.
 */
@Slf4j
@Data
@NoArgsConstructor
public class CFGImpl implements CFG {

    private String methodName;
    private NavigableMap<Integer, CFGNode> nodes;
    private Multimap<Integer, Integer> edges;

    public CFGImpl(
            final String methodName,
            final NavigableMap<Integer, CFGNode> nodes,
            final Multimap<Integer, Integer> edges) {
        Preconditions.checkNotNull(methodName);
        Preconditions.checkNotNull(nodes);
        this.methodName = methodName;
        this.nodes = nodes;
        this.edges = edges;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NavigableMap<Integer, CFGNode> getNodes() {
        return nodes;
    }

    @Override
    public Multimap<Integer, Integer> getEdges() { return edges; }

    @Override
    public CFGNode getEntryNode() {
        throw new UnsupportedOperationException("Please implement CFGImpl.getEntryNode");
    }

    @Override
    public CFGNode getExitNode() {
        throw new UnsupportedOperationException("Please implement CFGImpl.getExitNode");
    }

    @Override
    public Set<LocalVariable> getDomain() {
        throw new UnsupportedOperationException("Please implement CFGImpl.getDomain()");
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
                workList.addAll(node.getPred());
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
