package graphs.cfg.nodes;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import data.ProgramVariable;
import graphs.cfg.CFG;
import lombok.Data;
import lombok.NoArgsConstructor;
import utils.JDFCUtils;

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
     * Name of the enclosing method (ASM internal name).
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

    public CFGNode(
            final String className,
            final String methodName,
            final Set<ProgramVariable> pDefinitions,
            final Set<ProgramVariable> pUses,
            final int pIndex,
            final int pOpcode,
            final Set<CFGNode> pPredecessors,
            final Set<CFGNode> pSuccessors,
            final Set<ProgramVariable> reach,
            final Set<ProgramVariable> reachOut) {
        this.className = className;
        this.methodName = methodName;
        this.definitions = pDefinitions;
        this.uses = pUses;
        this.insnIndex = pIndex;
        this.opcode = pOpcode;
        this.pred = pPredecessors;
        this.succ = pSuccessors;

        this.reachOut = reach;
        this.reach = reachOut;
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
        reachOut.stream().filter(this::isRedefinedVariable).collect(Collectors.toList()).forEach(
                reachOut::remove);
    }

    public boolean isRedefinedVariable(ProgramVariable variable) {
        return definitions
                .stream()
                .anyMatch(
                        programVariable ->
                                programVariable.getName().equals(variable.getName())
                                        && !Objects.equals(programVariable.getInstructionIndex(), variable.getInstructionIndex()));
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

    public Set<ProgramVariable> getDefinitions() {
        return ImmutableSet.copyOf(definitions);
    }

    public Set<ProgramVariable> getUses() {
        return ImmutableSet.copyOf(uses);
    }

    public Set<ProgramVariable> getReach() {
        return ImmutableSet.copyOf(reach);
    }

    public Set<ProgramVariable> getReachOut() {
        return ImmutableSet.copyOf(reachOut);
    }
}
