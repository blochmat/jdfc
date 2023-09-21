package graphs.sg.nodes;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import data.ProgramVariable;
import graphs.cfg.nodes.CFGNode;
import lombok.Data;
import lombok.NoArgsConstructor;
import utils.JDFCUtils;

import java.util.Objects;
import java.util.Set;

@Data
@NoArgsConstructor
public class SGNode {

    private int index;
    private int cfgIndex;
    private int insnIndex;
    private int opcode;
    private String className;
    private String methodName;
    private Set<ProgramVariable> definitions;
    private Set<ProgramVariable> uses;
    private Set<SGNode> pred;
    private Set<SGNode> succ;
    private Set<ProgramVariable> cfgReachOut;
    private Set<ProgramVariable> cfgReach;

    public SGNode(int index, int cfgIndex, CFGNode node) {
        this.index = index;
        this.cfgIndex = cfgIndex;
        this.className = node.getClassName();
        this.methodName = node.getMethodName();
        this.definitions = node.getDefinitions();
        this.uses = node.getUses();
        this.insnIndex = node.getInsnIndex();
        this.opcode = node.getOpcode();
        this.pred = Sets.newLinkedHashSet();
        this.succ = Sets.newLinkedHashSet();
        this.cfgReachOut = node.getReachOut();
        this.cfgReach = node.getReach();
    }

    @Override
    public String toString() {
        return String.format("SGNode: %s %s %d %s (%d preds, %d succs)",
                className, methodName, insnIndex, JDFCUtils.getOpcode(opcode), pred.size(), succ.size());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SGNode that = (SGNode) o;
        return getIndex() == that.getIndex()
                && getCfgIndex() == that.getCfgIndex()
                && getInsnIndex() == that.getInsnIndex()
                && getOpcode() == that.getOpcode()
                && Objects.equals(getClassName(), that.getClassName())
                && Objects.equals(getMethodName(), that.getMethodName());
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                getIndex(),
                getCfgIndex(),
                getInsnIndex(),
                getOpcode(),
                getClassName(),
                getMethodName());
    }

    public Set<ProgramVariable> getCfgReach() {
        return ImmutableSet.copyOf(cfgReach);
    }

    public Set<ProgramVariable> getCfgReachOut() {
        return ImmutableSet.copyOf(cfgReachOut);
    }
}
