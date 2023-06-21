package graphs.sg.nodes;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import data.ProgramVariable;
import graphs.cfg.nodes.CFGNode;
import utils.JDFCUtils;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

public class SGNode {
    
    private String internalMethodName;
    private Set<ProgramVariable> definitions;
    private Set<ProgramVariable> uses;
    private int insnIndex;
    private int opcode;
    private Set<SGNode> pred;
    private Set<SGNode> succ;
    private Set<ProgramVariable> reachOut;
    private Set<ProgramVariable> reach;

    public SGNode(String internalMethodName, CFGNode node) {
        this.internalMethodName = internalMethodName;
        this.definitions = node.getDefinitions();
        this.uses = node.getUses();
        this.insnIndex = node.getInsnIndex();
        this.opcode = node.getOpcode();
        this.pred = Sets.newLinkedHashSet();
        this.succ = Sets.newLinkedHashSet();
        this.reachOut = node.getReachOut();
        this.reach = node.getReach();
    }

    public void resetReachOut() {
        reachOut.clear();
    }

    public void update() {
        for (SGNode node : pred) {
            reach.addAll(node.getReachOut());
        }
        reachOut.clear();
        reachOut.addAll(reach);
        reachOut.addAll(definitions);
        reachOut.removeAll(
                reachOut.stream().filter(this::isRedefinedVariable).collect(Collectors.toList()));
    }

    public boolean isRedefinedVariable(ProgramVariable variable) {
        return definitions
                .stream()
                .anyMatch(
                        programVariable ->
                                programVariable.getName().equals(variable.getName())
                                        && programVariable.getInstructionIndex() != variable.getInstructionIndex());
    }

    public void addSuccessor(final SGNode pNode) {
        succ.add(pNode);
    }

    public void addPredecessor(final SGNode pNode) {
        pred.add(pNode);
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
    public Set<SGNode> getSucc() {
        return Collections.unmodifiableSet(succ);
    }

    public Set<ProgramVariable> getReach() {
        return Collections.unmodifiableSet(reach);
    }

    /**
     * Returns the index number of this node.
     *
     * @return The index number of this node
     */
    public int getInsnIndex() {
        return insnIndex;
    }

    /**
     * Returns the set of {@link CFGNode}s that are predecessors of this node.
     *
     * @return The set of {@link CFGNode}s that are predecessors
     */
    public Set<SGNode> getPred() {
        return Collections.unmodifiableSet(pred);
    }

    public void addDefinition(ProgramVariable pDefinition) {
        definitions.add(pDefinition);
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

    public int getOpcode() {
        return opcode;
    }

    @Override
    public String toString() {
        return String.format(
                "SGNode: %d %s (%d preds, %d succs) | definitions %s | uses %s",
                insnIndex, JDFCUtils.getOpcode(opcode), pred.size(), succ.size(), definitions, uses);
    }
}
