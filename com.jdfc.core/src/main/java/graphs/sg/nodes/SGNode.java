package graphs.sg.nodes;

import com.google.common.collect.Sets;
import data.ProgramVariable;
import graphs.cfg.nodes.CFGNode;
import lombok.Data;
import lombok.NoArgsConstructor;
import utils.JDFCUtils;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
public class SGNode {

    private int index;
    private int insnIndex;
    private int opcode;
    private String className;
    private String methodName;
    private Set<ProgramVariable> definitions;
    private Set<ProgramVariable> uses;
    private Set<SGNode> pred;
    private Set<SGNode> succ;
    private Set<ProgramVariable> reachOut;
    private Set<ProgramVariable> reach;

    public SGNode(int index, CFGNode node) {
        this.index = index;
        this.className = node.getClassName();
        this.methodName = node.getMethodName();
        this.definitions = node.getDefinitions();
        this.uses = node.getUses();
        this.insnIndex = node.getInsnIndex();
        this.opcode = node.getOpcode();
        this.pred = Sets.newLinkedHashSet();
        this.succ = Sets.newLinkedHashSet();
        this.reachOut = Sets.newLinkedHashSet();
        this.reach = Sets.newLinkedHashSet();
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

    @Override
    public String toString() {
        return String.format(
                "SGNode: %s %s %d %s (%d preds, %d succs) | definitions %s | uses %s",
                className, methodName, insnIndex, JDFCUtils.getOpcode(opcode), pred.size(), succ.size(), definitions, uses);
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SGNode that = (SGNode) o;
        return getIndex() == that.getIndex()
                && getInsnIndex() == that.getInsnIndex()
                && getOpcode() == that.getOpcode()
                && Objects.equals(getClassName(), that.getClassName())
                && Objects.equals(getMethodName(), that.getMethodName());
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                getIndex(),
                getInsnIndex(),
                getOpcode(),
                getClassName(),
                getMethodName());
    }
}
