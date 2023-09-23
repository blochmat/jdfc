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
    private int entryNodeIdx;
    private int insnIndex;
    private int opcode;
    private String className;
    private String methodName;
    private int lineNumber;
    private Set<ProgramVariable> definitions;
    private Set<ProgramVariable> uses;
    private Set<SGNode> pred;
    private Set<SGNode> succ;
    private Set<ProgramVariable> cfgReachOut;
    private Set<ProgramVariable> cfgReach;

    public SGNode(int index, int cfgIndex, int entryNodeIdx, CFGNode node) {
        this.index = index;
        this.cfgIndex = cfgIndex;
        this.entryNodeIdx = entryNodeIdx;
        this.className = node.getClassName();
        this.methodName = node.getMethodName();
        this.lineNumber = node.getLineNumber();
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
        return String.format("%d:%d SGNode: lio(%d,%d,%s) (%s::%s) ps(%d,%d)",
                index,
                cfgIndex,
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
        SGNode that = (SGNode) o;
        return getIndex() == that.getIndex()
                && getCfgIndex() == that.getCfgIndex()
                && getEntryNodeIdx() == that.getEntryNodeIdx()
                && getInsnIndex() == that.getInsnIndex()
                && getOpcode() == that.getOpcode()
                && Objects.equals(getClassName(), that.getClassName())
                && Objects.equals(getMethodName(), that.getMethodName())
                && getLineNumber() == that.getLineNumber();
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                getIndex(),
                getCfgIndex(),
                getEntryNodeIdx(),
                getInsnIndex(),
                getOpcode(),
                getClassName(),
                getMethodName(),
                getLineNumber());
    }

    public Set<ProgramVariable> getCfgReach() {
        return ImmutableSet.copyOf(cfgReach);
    }

    public Set<ProgramVariable> getCfgReachOut() {
        return ImmutableSet.copyOf(cfgReachOut);
    }
}
