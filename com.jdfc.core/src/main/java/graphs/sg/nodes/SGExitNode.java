package graphs.sg.nodes;

import data.DomainVariable;
import data.ProgramVariable;
import graphs.cfg.nodes.CFGNode;
import lombok.Data;
import utils.JDFCUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Data
public class SGExitNode extends SGNode {

    private Map<ProgramVariable, ProgramVariable> pVarMap;

    private Map<DomainVariable, DomainVariable> dVarMap;

    public SGExitNode(int index, CFGNode node) {
        super(index, node);
        this.pVarMap = new HashMap<>();
        this.dVarMap = new HashMap<>();
    }

    @Override
    public String toString() {
        return String.format(
                "SGExitNode: %d %s (%d preds, %d succs) | definitions %s | uses %s",
                this.getInsnIndex(), JDFCUtils.getOpcode(this.getOpcode()), this.getPred().size(), this.getSucc().size(), this.getDefinitions(), this.getUses());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SGExitNode that = (SGExitNode) o;
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
