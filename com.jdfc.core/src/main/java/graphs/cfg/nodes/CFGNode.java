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

    private int index;
    private String className;
    private String methodName;
    private int lineNumber;
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
            final int lineNumber,
            final int index,
            final int opcode) {
        this(className, methodName, lineNumber, Sets.newLinkedHashSet(), Sets.newLinkedHashSet(), index, opcode,
                Sets.newLinkedHashSet(), Sets.newLinkedHashSet());
    }

    public CFGNode(
            final String className,
            final String methodName,
            final int lineNumber,
            final Set<ProgramVariable> definitions,
            final Set<ProgramVariable> uses,
            final int index,
            final int opcode) {
        this(className, methodName, lineNumber, definitions, uses, index, opcode, Sets.newLinkedHashSet(),
                Sets.newLinkedHashSet());
    }

    public CFGNode(
            final String className,
            final String methodName,
            final int lineNumber,
            final Set<ProgramVariable> definitions,
            final Set<ProgramVariable> uses,
            final int index,
            final int opcode,
            final Set<CFGNode> pPredecessors,
            final Set<CFGNode> pSuccessors) {
        this.className = className;
        this.methodName = methodName;
        this.lineNumber = lineNumber;
        this.definitions = definitions;
        this.uses = uses;
        insnIndex = index;
        this.opcode = opcode;
        pred = pPredecessors;
        succ = pSuccessors;

        reachOut = Sets.newLinkedHashSet();
        reach = Sets.newLinkedHashSet();
    }

    public CFGNode(
            final String className,
            final String methodName,
            final int lineNumber,
            final Set<ProgramVariable> definitions,
            final Set<ProgramVariable> uses,
            final int index,
            final int opcode,
            final Set<CFGNode> predecessors,
            final Set<CFGNode> successors,
            final Set<ProgramVariable> reach,
            final Set<ProgramVariable> reachOut) {
        this.className = className;
        this.methodName = methodName;
        this.lineNumber = lineNumber;
        this.definitions = definitions;
        this.uses = uses;
        this.insnIndex = index;
        this.opcode = opcode;
        this.pred = predecessors;
        this.succ = successors;

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
                                        && programVariable.getName().equals(this.methodName)
                                        && programVariable.getMethodName().equals(variable.getMethodName())
                                        && !Objects.equals(programVariable.getInstructionIndex(), variable.getInstructionIndex()));
    }

    @Override
    public String toString() {
        return String.format(
                "%d CFGNode: lio(%d,%d,%s) (%s::%s) ps(%d,%d)",
                index,
                lineNumber,
                insnIndex,
                JDFCUtils.getOpcode(opcode),
                className,
                methodName,
                pred.size(),
                succ.size());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CFGNode that = (CFGNode) o;
        return getLineNumber() == that.getLineNumber()
                && getInsnIndex() == that.getInsnIndex()
                && getOpcode() == that.getOpcode()
                && Objects.equals(getClassName(), that.getClassName())
                && Objects.equals(getMethodName(), that.getMethodName());
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                getLineNumber(),
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

    void addFieldDefinitions(Set<ProgramVariable> fieldDefinitions) {
        this.definitions.addAll(fieldDefinitions);
    }
}
