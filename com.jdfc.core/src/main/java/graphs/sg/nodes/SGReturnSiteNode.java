package graphs.sg.nodes;

import data.ProgramVariable;
import graphs.cfg.nodes.CFGNode;
import lombok.Data;
import utils.ASMHelper;
import utils.JDFCUtils;

import java.util.Map;
import java.util.Objects;

@Data
public class SGReturnSiteNode extends SGNode {

    private ASMHelper asmHelper = new ASMHelper();

    private int callNodeIdx;
    private int exitNodeIdx;
    /**
     * The key is a definition of the current procedure.
     * The value is a definition of the invoked procedure.
     */
    private Map<ProgramVariable, ProgramVariable> definitionsMap;

    public SGReturnSiteNode(int index, int cfgIndex, int entryNodeIdx, CFGNode node) {
        super(index, cfgIndex, entryNodeIdx, node);
    }

    @Override
    public String toString() {
        return String.format("%d:%d SGReturnSiteNode: lio(%d,%d,%s) (%s::%s%s) ps(%d,%d)",
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
        SGReturnSiteNode that = (SGReturnSiteNode) o;
        return getIndex() == that.getIndex()
                && getLineNumber() == that.getLineNumber()
                && getCfgIndex() == that.getCfgIndex()
                && getEntryNodeIdx() == that.getEntryNodeIdx()
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
