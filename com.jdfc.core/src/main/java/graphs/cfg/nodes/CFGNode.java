package graphs.cfg.nodes;

import com.google.common.collect.Sets;
import data.ProgramVariable;
import graphs.cfg.CFG;
import lombok.Data;
import lombok.NoArgsConstructor;
import utils.JDFCUtils;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A node in the {@link CFG}.
 */

@Data
@NoArgsConstructor
public class CFGNode {

    /**
     * Name of the method's owner class (relative path).
     */
    private String className;

    /**
     * Name of the enclosing method (ASM descriptor without exceptions).
     */
    private String methodName;
    private Set<ProgramVariable> definitions;
    private Set<ProgramVariable> uses;
    private int insnIndex;
    private int opcode;
    private Set<CFGNode> pred;
    private Set<CFGNode> succ;
    private Set<ProgramVariable> reachOut;
    private Set<ProgramVariable> reach;

    public CFGNode(
            final String className,
            final String methodName,
            final int pIndex,
            final int pOpcode) {
        this(
                className,
                methodName,
                Sets.newLinkedHashSet(),
                Sets.newLinkedHashSet(),
                pIndex,
                pOpcode,
                Sets.newLinkedHashSet(),
                Sets.newLinkedHashSet());
    }

    public CFGNode(
            final String className,
            final String methodName,
            final Set<ProgramVariable> pDefinitions,
            final Set<ProgramVariable> pUses,
            final int pIndex,
            final int pOpcode) {
        this(className, methodName, pDefinitions, pUses, pIndex, pOpcode, Sets.newLinkedHashSet(), Sets.newLinkedHashSet());
    }

    public CFGNode(
            final String className,
            final String methodName,
            final Set<ProgramVariable> pDefinitions,
            final Set<ProgramVariable> pUses,
            final int pIndex,
            final int pOpcode,
            final Set<CFGNode> pPredecessors,
            final Set<CFGNode> pSuccessors) {
        this.className = className;
        this.methodName = methodName;
        definitions = pDefinitions;
        uses = pUses;
        insnIndex = pIndex;
        opcode = pOpcode;
        pred = pPredecessors;
        succ = pSuccessors;

        reachOut = Sets.newLinkedHashSet();
        reach = Sets.newLinkedHashSet();
    }

    public void resetReachOut() {
        reachOut.clear();
    }

    public void update() {
        for (CFGNode node : pred) {
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

    public void addSuccessor(final CFGNode pNode) {
        succ.add(pNode);
    }

    public void addPredecessor(final CFGNode pNode) {
        pred.add(pNode);
    }

    /**
     * Returns the set of {@link ProgramVariable}s in the ReachOut set.
     *
     * @return The set of {@link ProgramVariable}s in the ReachOut set
     */
    public Set<ProgramVariable> getReachOut() {
        return reachOut;
    }

    /**
     * Returns the set of {@link CFGNode}s that are successors of this node.
     *
     * @return The set of {@link CFGNode}s that are successors
     */
    public Set<CFGNode> getSucc() {
        return Collections.unmodifiableSet(succ);
    }

    public Set<ProgramVariable> getReach() {
        return reach;
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
    public Set<CFGNode> getPred() {
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
                "CFGNode: %d %s %s %s (%d preds, %d succs) | definitions %s | uses %s",
                insnIndex,
                JDFCUtils.getOpcode(opcode),
                className,
                methodName,
                pred.size(),
                succ.size(),
                definitions,
                uses);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CFGNode that = (CFGNode) o;
        return getInsnIndex() == that.getInsnIndex()
                && getOpcode() == that.getOpcode()
                && Objects.equals(getClassName(), that.getClassName())
                && Objects.equals(getMethodName(), that.getMethodName());
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                getInsnIndex(),
                getOpcode(),
                getClassName(),
                getMethodName());
    }
}
