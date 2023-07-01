package graphs.cfg;

import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import data.DomainVariable;
import data.ProgramVariable;
import graphs.cfg.nodes.CFGEntryNode;
import graphs.cfg.nodes.CFGNode;
import lombok.Data;
import lombok.NonNull;
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
public class CFG {

    private String owner;
    private String methodName;
    private NavigableMap<Integer, CFGNode> nodes;
    private Multimap<Integer, Integer> edges;
    private NavigableMap<Integer, DomainVariable> domain;

    public CFG(
            @NonNull final String owner,
            @NonNull final String methodName,
            @NonNull final NavigableMap<Integer, CFGNode> nodes,
            @NonNull final Multimap<Integer, Integer> edges,
            @NonNull final NavigableMap<Integer, DomainVariable> domain) {
        this.owner = owner;
        this.methodName = methodName;
        this.nodes = nodes;
        this.edges = edges;
        this.domain = domain;
    }

    public CFGEntryNode getEntryNode() {
        return (CFGEntryNode) this.nodes.get(0);
    }

    /**
     * {@inheritDoc}
     */
    public void calculateReachingDefinitions() {
        LinkedList<CFGNode> workList = new LinkedList<>();
        for (Map.Entry<Integer, CFGNode> node : nodes.entrySet()) {
            node.getValue().resetReachOut();
            workList.add(node.getValue());
        }

        while (!workList.isEmpty()) {
            CFGNode node = workList.poll();
            Set<ProgramVariable> oldValue = Sets.newLinkedHashSet(node.getReachOut());
            node.update();
            if (!node.getReachOut().equals(oldValue)) {
                workList.addAll(node.getSucc());
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return String.format("CFGImpl for method %s (containing %d nodes)%n %s", methodName, nodes.size(), JDFCUtils.prettyPrintMap(nodes));
    }
}
