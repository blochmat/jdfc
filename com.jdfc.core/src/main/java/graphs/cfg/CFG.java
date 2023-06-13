package graphs.cfg;

import com.google.common.collect.Multimap;
import graphs.cfg.nodes.CFGNode;

import java.util.NavigableMap;
import java.util.Set;

/**
 * The Control-Flow Graph (CFG) for one method of the original program.
 *
 * <p>The CFG itself consists of a number of {@link CFGNode}s and offers the possibility to
 * calculate the reaching definitions of variables.
 */
public interface CFG {

    /**
     * Calculates the reaching definitions for each variable using the classic reaching-definitions
     * worklist algorithm.
     */
    void calculateReachingDefinitions();

    /** {@inheritDoc} */
    @Override
    String toString();

    /**
     * Returns a map between node ID and {@link CFGNode}.
     *
     * @return A map between node ID and {@link CFGNode}
     */
    NavigableMap<Integer, CFGNode> getNodes();

    Multimap<Integer, Integer> getEdges();

    CFGNode getEntryNode();

    CFGNode getExitNode();

    Set<LocalVariable> getDomain();
}
