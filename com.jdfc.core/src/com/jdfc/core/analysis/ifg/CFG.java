package com.jdfc.core.analysis.ifg;

import com.jdfc.core.analysis.ifg.data.LocalVariable;

import java.util.Map;
import java.util.NavigableMap;

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
