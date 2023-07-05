package graphs.sg.nodes;

import graphs.cfg.nodes.CFGNode;
import lombok.Data;
import utils.JDFCUtils;

import java.util.Objects;

@Data
public class SGReturnSiteNode extends SGNode {

    private int callNodeIdx;
    private int entryNodeIdx;
    private int exitNodeIdx;

    public SGReturnSiteNode(int index, CFGNode node) {
        super(index, node);
    }

    @Override
    public String toString() {
        return String.format(
                "SGReturnSiteNode: %d %s (%d preds, %d succs) | definitions %s | uses %s",
                this.getInsnIndex(), JDFCUtils.getOpcode(this.getOpcode()), this.getPred().size(), this.getSucc().size(), this.getDefinitions(), this.getUses());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SGReturnSiteNode that = (SGReturnSiteNode) o;
        return getIndex() == that.getIndex()
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
