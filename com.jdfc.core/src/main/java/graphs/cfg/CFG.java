package graphs.cfg;

import com.google.common.collect.Multimap;
import data.DomainVariable;
import graphs.cfg.nodes.CFGEntryNode;
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
     * Returns the name of the owner class.
     *
     * @return name of the owner class.
     */
    String getOwner();

    /**
     * Returns the name of the corresponding method.
     *
     * @return name of the corresponding method.
     */
    String getMethodName();

    /**
     * Returns a map between node ID and {@link CFGNode}.
     *
     * @return A map between node ID and {@link CFGNode}
     */
    NavigableMap<Integer, CFGNode> getNodes();

    /**
     * Returns a map between node ID and connected node IDs.
     *
     * @return A map between node ID and connected node IDs
     */
    Multimap<Integer, Integer> getEdges();

    /**
     * Returns a set of all {@link DomainVariable}s present in the scope of the CFG.
     *
     * @return A set of all {@link DomainVariable}s present in the scope of the CFG
     */
    Set<DomainVariable> getDomain();

    /**
     * Returns a {@link CFGEntryNode} representing the entry node of the CFG.
     *
     * @return A {@link CFGEntryNode} representing the entry node of the CFG
     */
    CFGEntryNode getEntryNode();

    /**
     * Calculates the reaching definitions for each variable using the classic reaching-definitions
     * worklist algorithm.
     */
    void calculateReachingDefinitions();

    /** {@inheritDoc} */
    @Override
    String toString();
}
