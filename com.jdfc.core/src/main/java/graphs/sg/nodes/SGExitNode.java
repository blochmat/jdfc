package graphs.sg.nodes;

import data.ProgramVariable;
import graphs.cfg.nodes.CFGNode;
import lombok.Data;
import utils.ASMHelper;
import utils.JDFCUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Data
public class SGExitNode extends SGNode {

    private ASMHelper asmHelper = new ASMHelper();

    /**
     * The key is a definition of the invoked procedure.
     * The value is a definition of the invoking procedure.
     */
    private Map<ProgramVariable, ProgramVariable> definitionsMap;
    private int callNodeIdx;
    private int returnSiteNodeIdx;

    public SGExitNode(int index, int cfgIndex, int entryNodeIdx, CFGNode node) {
        super(index, cfgIndex, entryNodeIdx, node);
        this.definitionsMap = new HashMap<>();
    }

    @Override
    public String toString() {
        return String.format("%d:%d SGExitNode: lio(%d,%d,%s) (%s::%s%s) ps(%d,%d)",
                getIndex(),
                getCfgIndex(),
                getLineNumber(),
                getInsnIndex(),
                JDFCUtils.getOpcode(getOpcode()),
                getClassName(),
                getMethodName(),
                asmHelper.isStatic(getMethodAccess()) ? "::static" : "",
                getPred().size(),
                getSucc().size());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SGExitNode that = (SGExitNode) o;
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
        return super.hashCode();
    }
}
