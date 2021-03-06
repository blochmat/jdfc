package com.jdfc.core.analysis.cfg;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A node in the {@link CFG}.
 */
public class CFGNode {

    private final Set<ProgramVariable> definitions;
    private final Set<ProgramVariable> uses;
    private final int index;
    private final Set<CFGNode> predecessors;
    private final Set<CFGNode> successors;
    private final Set<ProgramVariable> reachOut;
    private final Set<ProgramVariable> reach;

    CFGNode(final int pIndex) {
        this(
                Sets.newLinkedHashSet(),
                Sets.newLinkedHashSet(),
                pIndex,
                Sets.newLinkedHashSet(),
                Sets.newLinkedHashSet());
    }

    CFGNode(
            final Set<ProgramVariable> pDefinitions, final Set<ProgramVariable> pUses, final int pIndex) {
        this(pDefinitions, pUses, pIndex, Sets.newLinkedHashSet(), Sets.newLinkedHashSet());
    }

    CFGNode(
            final Set<ProgramVariable> pDefinitions,
            final Set<ProgramVariable> pUses,
            final int pIndex,
            final Set<CFGNode> pPredecessors,
            final Set<CFGNode> pSuccessors) {
        definitions = pDefinitions;
        uses = pUses;
        index = pIndex;
        predecessors = pPredecessors;
        successors = pSuccessors;

        reachOut = Sets.newLinkedHashSet();
        reach = Sets.newLinkedHashSet();
    }

    void resetReachOut() {
        reachOut.clear();
    }

    void update() {
        for (CFGNode node : predecessors) {
            reach.addAll(node.getReachOut());
        }

        reachOut.clear();
        reachOut.addAll(reach);
        reachOut.addAll(definitions);
        reachOut.removeAll(
                reachOut.stream().filter(this::isRedefinedVariable).collect(Collectors.toList()));
    }

    boolean isRedefinedVariable(ProgramVariable variable) {
        return definitions
                .stream()
                .anyMatch(
                        programVariable ->
                                programVariable.getName().equals(variable.getName())
                                        && programVariable.getInstructionIndex() != variable.getInstructionIndex());
    }

    void addSuccessor(final CFGNode pNode) {
        successors.add(pNode);
    }

    void addPredecessor(final CFGNode pNode) {
        predecessors.add(pNode);
    }

    /**
     * Returns the set of {@link ProgramVariable}s in the ReachOut set.
     *
     * @return The set of {@link ProgramVariable}s in the ReachOut set
     */
    public Set<ProgramVariable> getReachOut() {
        return ImmutableSet.copyOf(reachOut);
    }

    /**
     * Returns the set of {@link CFGNode}s that are successors of this node.
     *
     * @return The set of {@link CFGNode}s that are successors
     */
    public Set<CFGNode> getSuccessors() {
        return Collections.unmodifiableSet(successors);
    }

    public Set<ProgramVariable> getReach() {
        return Collections.unmodifiableSet(reach);
    }

    /**
     * Returns the index number of this node.
     *
     * @return The index number of this node
     */
    public int getIndex() {
        return index;
    }

    /**
     * Returns the set of {@link CFGNode}s that are predecessors of this node.
     *
     * @return The set of {@link CFGNode}s that are predecessors
     */
    public Set<CFGNode> getPredecessors() {
        return Collections.unmodifiableSet(predecessors);
    }

    /**
     * Returns the set of {@link ProgramVariable}s that get defined at this {@link CFGNode}.
     *
     * @return The set of {@link ProgramVariable}s that get defined
     */
    public Set<ProgramVariable> getDefinitions() {
        return Collections.unmodifiableSet(definitions);
    }

    /**
     * Returns the set of {@link ProgramVariable}s that get used at this {@link CFGNode}.
     *
     * @return The set of {@link ProgramVariable}s that get used
     */
    public Set<ProgramVariable> getUses() {
        return Collections.unmodifiableSet(uses);
    }

    @Override
    public String toString() {
        return String.format(
                "CFG Node: %d (%d predecessors, %d successors)",
                index, predecessors.size(), successors.size());
    }
}
