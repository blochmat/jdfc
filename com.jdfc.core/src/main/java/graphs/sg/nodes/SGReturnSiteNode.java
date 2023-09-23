package graphs.sg.nodes;

import graphs.cfg.nodes.CFGNode;
import lombok.Data;
import utils.JDFCUtils;

import java.util.Objects;

@Data
public class SGReturnSiteNode extends SGNode {

    private int callNodeIdx;
    private int exitNodeIdx;

    public SGReturnSiteNode(int index, int cfgIndex, int entryNodeIdx, CFGNode node) {
        super(index, cfgIndex, entryNodeIdx, node);
    }

    @Override
    public String toString() {
        return String.format("%d:%d SGReturnSiteNode: lio(%d,%d,%s) (%s::%s) ps(%d,%d)",
                getIndex(),
                getCfgIndex(),
                getLineNumber(),
                getInsnIndex(),
                JDFCUtils.getOpcode(getOpcode()),
                getClassName(),
                getMethodName(),
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
                && Objects.equals(getMethodName(), that.getMethodName());
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }
}
