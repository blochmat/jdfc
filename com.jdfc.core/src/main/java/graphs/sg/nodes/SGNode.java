package graphs.sg.nodes;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import data.ProgramVariable;
import graphs.cfg.nodes.CFGNode;
import lombok.Data;
import lombok.NoArgsConstructor;
import utils.ASMHelper;
import utils.JDFCUtils;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
public class SGNode {

    private ASMHelper asmHelper = new ASMHelper();

    private int index;
    private int cfgIndex;
    private int entryNodeIdx;
    private int insnIndex;
    private int opcode;
    private String className;
    private String methodName;
    private int methodAccess;
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
        this.methodAccess = node.getMethodAccess();
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

    public ProgramVariable getNewDefOf(ProgramVariable src) {
        List<ProgramVariable> newDefs = definitions.stream()
                .filter(x -> x.isNewDefOf(src))
                .collect(Collectors.toList());
        if (newDefs.size() == 1) {
            return newDefs.get(0);
        } else {
            return null;
        }
    }

    @Override
    public String toString() {
        return String.format("%d:%d SGNode: lio(%d,%d,%s) (%s::%s%s) ps(%d,%d)",
                index,
                cfgIndex,
                lineNumber,
                insnIndex,
                JDFCUtils.getOpcode(opcode),
                className,
                methodName,
                asmHelper.isStatic(methodAccess) ? "::static" : "",
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
                && getLineNumber() == that.getLineNumber()
                && getInsnIndex() == that.getInsnIndex()
                && getOpcode() == that.getOpcode()
                && Objects.equals(getClassName(), that.getClassName())
                && Objects.equals(getMethodName(), that.getMethodName())
                && Objects.equals(getMethodAccess(), that.getMethodAccess());
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
                getMethodAccess(),
                getLineNumber());
    }

    public Set<ProgramVariable> getCfgReach() {
        return ImmutableSet.copyOf(cfgReach);
    }

    public Set<ProgramVariable> getCfgReachOut() {
        return ImmutableSet.copyOf(cfgReachOut);
    }
}
