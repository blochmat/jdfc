package cfg;

import cfg.nodes.CFGNode;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.base.Preconditions;
import com.google.common.collect.Multimap;
import data.ProgramVariable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.JDFCUtils;

import java.util.LinkedList;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;

/**
 * A implementation of a {@link CFG}.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CFGImpl implements CFG {

    @JsonIgnore
    private Logger logger = LoggerFactory.getLogger(CFGImpl.class);
    private String methodName;
    private NavigableMap<Double, CFGNode> nodes;
    private Multimap<Double, Double> edges;

    public CFGImpl(
            final String pMethodName,
            final NavigableMap<Double, CFGNode> pNodes,
            final Multimap<Double, Double> edges) {
        Preconditions.checkNotNull(pMethodName);
        Preconditions.checkNotNull(pNodes);
        methodName = pMethodName;
        nodes = pNodes;
        this.edges = edges;
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
    public void calculateReachingDefinitions() {
        logger.debug("calculateReachingDefinitions");
        logger.debug(JDFCUtils.prettyPrintMap(nodes));
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

        logger.debug(JDFCUtils.prettyPrintMap(nodes));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return String.format("ICFGImpl for method %s (containing %d nodes)%n %s", methodName, nodes.size(), JDFCUtils.prettyPrintMap(nodes));
    }
}
