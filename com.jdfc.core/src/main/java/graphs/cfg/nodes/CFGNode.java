package graphs.cfg.nodes;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import data.singleton.CoverageDataStore;
import graphs.cfg.CFG;
import utils.JDFCUtils;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * A node in the {@link CFG}.
 */
public class CFGNode {
    private final Set<UUID> definitions;
    private final Set<UUID> uses;
    private final int insnIdx;
    private final int opcode;
    private final Set<CFGNode> predecessors;
    private final Set<CFGNode> successors;
    private final Set<UUID> reachOut;
    private final Set<UUID> reach;

    public CFGNode(final int insnIdx, final int opcode) {
        this(
                Sets.newLinkedHashSet(),
                Sets.newLinkedHashSet(),
                insnIdx,
                opcode,
                Sets.newLinkedHashSet(),
                Sets.newLinkedHashSet());
    }

    public CFGNode(
            final Set<UUID> definitions, final Set<UUID> uses, final int insnIdx, final int opcode) {
        this(definitions, uses, insnIdx, opcode, Sets.newLinkedHashSet(), Sets.newLinkedHashSet());
    }

    public CFGNode(
            final Set<UUID> pDefinitions,
            final Set<UUID> pUses,
            final int pIndex,
            final int pOpcode,
            final Set<CFGNode> pPredecessors,
            final Set<CFGNode> pSuccessors) {
        definitions = pDefinitions;
        uses = pUses;
        insnIdx = pIndex;
        opcode = pOpcode;
        predecessors = pPredecessors;
        successors = pSuccessors;

        reachOut = Sets.newLinkedHashSet();
        reach = Sets.newLinkedHashSet();
    }

    public void resetReachOut() {
        reachOut.clear();
    }

    public void update() {
        for (CFGNode node : predecessors) {
            reach.addAll(node.getReachOut());
        }
        reachOut.clear();
        reachOut.addAll(reach);
        reachOut.addAll(definitions);
        reachOut.removeAll(
                reachOut.stream().filter(this::isRedefinedVariable).collect(Collectors.toList()));
    }

    public boolean isRedefinedVariable(UUID varId) {
        CoverageDataStore store = CoverageDataStore.getInstance();
        return definitions
                .stream()
                .map(id -> store.getUuidProgramVariableMap().get(id))
                .anyMatch(
                        programVariable ->
                                programVariable.getName().equals(store.getUuidProgramVariableMap().get(varId).getName())
                                        && programVariable.getInsnIdx() != store.getUuidProgramVariableMap().get(varId).getInsnIdx());
    }

    public void addSuccessor(final CFGNode pNode) {
        successors.add(pNode);
    }

    public void addPredecessor(final CFGNode pNode) {
        predecessors.add(pNode);
    }

    public Set<UUID> getReachOut() {
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

    public Set<UUID> getReach() {
        return Collections.unmodifiableSet(reach);
    }

    /**
     * Returns the index number of this node.
     *
     * @return The index number of this node
     */
    public int getInsnIdx() {
        return insnIdx;
    }

    /**
     * Returns the set of {@link CFGNode}s that are predecessors of this node.
     *
     * @return The set of {@link CFGNode}s that are predecessors
     */
    public Set<CFGNode> getPredecessors() {
        return Collections.unmodifiableSet(predecessors);
    }

    public void addDefinition(UUID pDefinition) {
        definitions.add(pDefinition);
    }

    public Set<UUID> getDefinitions() {
        return Collections.unmodifiableSet(definitions);
    }

    public Set<UUID> getUses() {
        return Collections.unmodifiableSet(uses);
    }

    public int getOpcode() {
        return opcode;
    }

    @Override
    public String toString() {
        return String.format(
                "CFGNode: %d %s (%d predecessors, %d successors) | definitions %s | uses %s",
                insnIdx, JDFCUtils.getOpcode(opcode), predecessors.size(), successors.size(), definitions, uses);
    }
}
