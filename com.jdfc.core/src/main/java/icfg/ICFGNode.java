package icfg;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import icfg.data.ProgramVariable;
import utils.JDFCUtils;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A node in the {@link CFG}.
 */
public class ICFGNode {
    private final Set<ProgramVariable> definitions;
    private final Set<ProgramVariable> uses;
    private final int index;
    private final int opcode;
    private final Set<ICFGNode> predecessors;
    private final Set<ICFGNode> successors;
    private final Set<ProgramVariable> reachOut;
    private final Set<ProgramVariable> reach;

    ICFGNode(final int pIndex, final int pOpcode) {
        this(
                Sets.newLinkedHashSet(),
                Sets.newLinkedHashSet(),
                pIndex,
                pOpcode,
                Sets.newLinkedHashSet(),
                Sets.newLinkedHashSet());
    }

    ICFGNode(
            final Set<ProgramVariable> pDefinitions, final Set<ProgramVariable> pUses, final int pIndex, final int pOpcode) {
        this(pDefinitions, pUses, pIndex, pOpcode, Sets.newLinkedHashSet(), Sets.newLinkedHashSet());
    }

    ICFGNode(
            final Set<ProgramVariable> pDefinitions,
            final Set<ProgramVariable> pUses,
            final int pIndex,
            final int pOpcode,
            final Set<ICFGNode> pPredecessors,
            final Set<ICFGNode> pSuccessors) {
        definitions = pDefinitions;
        uses = pUses;
        index = pIndex;
        opcode = pOpcode;
        predecessors = pPredecessors;
        successors = pSuccessors;

        reachOut = Sets.newLinkedHashSet();
        reach = Sets.newLinkedHashSet();
    }

    void resetReachOut() {
        reachOut.clear();
    }

    void update() {
        for (ICFGNode node : predecessors) {
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

    void addSuccessor(final ICFGNode pNode) {
        successors.add(pNode);
    }

    void addPredecessor(final ICFGNode pNode) {
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
     * Returns the set of {@link ICFGNode}s that are successors of this node.
     *
     * @return The set of {@link ICFGNode}s that are successors
     */
    public Set<ICFGNode> getSuccessors() {
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
     * Returns the set of {@link ICFGNode}s that are predecessors of this node.
     *
     * @return The set of {@link ICFGNode}s that are predecessors
     */
    public Set<ICFGNode> getPredecessors() {
        return Collections.unmodifiableSet(predecessors);
    }

    public void addDefinition(ProgramVariable pDefinition) {
        definitions.add(pDefinition);
    }

    /**
     * Returns the set of {@link ProgramVariable}s that get defined at this {@link ICFGNode}.
     *
     * @return The set of {@link ProgramVariable}s that get defined
     */
    public Set<ProgramVariable> getDefinitions() {
        return Collections.unmodifiableSet(definitions);
    }

    /**
     * Returns the set of {@link ProgramVariable}s that get used at this {@link ICFGNode}.
     *
     * @return The set of {@link ProgramVariable}s that get used
     */
    public Set<ProgramVariable> getUses() {
        return Collections.unmodifiableSet(uses);
    }

    public int getOpcode() {
        return opcode;
    }

    @Override
    public String toString() {
        return String.format(
                "CFG Node: %d %s (%d predecessors, %d successors)",
                index, JDFCUtils.getOpcode(opcode), predecessors.size(), successors.size());
    }
}
