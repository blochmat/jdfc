package icfg;

import icfg.data.LocalVariable;
import icfg.nodes.ICFGNode;

import java.util.Map;
import java.util.NavigableMap;

/**
 * The Control-Flow Graph (CFG) for one method of the original program.
 *
 * <p>The CFG itself consists of a number of {@link ICFGNode}s and offers the possibility to
 * calculate the reaching definitions of variables.
 */
public interface ICFG {

    /**
     * Calculates the reaching definitions for each variable using the classic reaching-definitions
     * worklist algorithm.
     */
    void calculateReachingDefinitions();

    /** {@inheritDoc} */
    @Override
    String toString();

    /**
     * Returns a map between node ID and {@link ICFGNode}.
     *
     * @return A map between node ID and {@link ICFGNode}
     */
    NavigableMap<Double, ICFGNode> getNodes();

    /**
     * Returns a local variable table representation of this method.
     *
     * @return Map containing all local variables of a method
     */
    Map<Integer, LocalVariable> getLocalVariableTable();

    /**
     * Returns if a method is impure.
     *
     * @return isImpure
     */
    boolean isImpure();

    void setImpure();
}
