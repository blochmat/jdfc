package graphs.cfg.nodes;

import data.ProgramVariable;
import lombok.Data;
import utils.JDFCUtils;

import java.util.Objects;
import java.util.Set;

@Data
public class CFGExitNode extends CFGNode {

    public CFGExitNode(
            String className,
            String methodName,
            Set<ProgramVariable> pDefinitions,
            Set<ProgramVariable> pUses,
            Set<CFGNode> pPredecessors,
            Set<CFGNode> pSuccessors) {
        super(className, methodName, pDefinitions, pUses, Integer.MIN_VALUE, Integer.MIN_VALUE, pPredecessors, pSuccessors);
    }

    @Override
    public String toString() {
        return String.format(
                "CFGExitNode: %d %s (%d preds, %d succs) | definitions %s | uses %s",
                this.getInsnIndex(), JDFCUtils.getOpcode(this.getOpcode()), this.getPred().size(), this.getSucc().size(), this.getDefinitions(), this.getUses());
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CFGExitNode that = (CFGExitNode) o;
        return getInsnIndex() == that.getInsnIndex()
                && getOpcode() == that.getOpcode()
                && Objects.equals(getClassName(), that.getClassName())
                && Objects.equals(getMethodName(), that.getMethodName());
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }
}
