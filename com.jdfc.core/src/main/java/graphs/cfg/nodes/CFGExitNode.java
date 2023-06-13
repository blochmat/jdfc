package graphs.cfg.nodes;

import data.ProgramVariable;
import utils.JDFCUtils;

import java.util.Set;

public class CFGExitNode extends CFGNode {

    public CFGExitNode(Set<ProgramVariable> pDefinitions, Set<ProgramVariable> pUses, Set<CFGNode> pPredecessors, Set<CFGNode> pSuccessors) {
        super(pDefinitions, pUses, Integer.MAX_VALUE, Integer.MAX_VALUE, pPredecessors, pSuccessors);
    }

    @Override
    public String toString() {
        return String.format(
                "CFGExitNode: %d %s (%d preds, %d succs) | definitions %s | uses %s",
                this.getInsnIndex(), JDFCUtils.getOpcode(this.getOpcode()), this.getPred().size(), this.getSucc().size(), this.getDefinitions(), this.getUses());
    }
}
