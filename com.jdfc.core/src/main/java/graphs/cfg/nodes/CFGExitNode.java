package graphs.cfg.nodes;

import utils.JDFCUtils;

import java.util.Set;
import java.util.UUID;

public class CFGExitNode extends CFGNode {

    public CFGExitNode(Set<UUID> pDefinitions, Set<UUID> pUses, Set<CFGNode> pPredecessors, Set<CFGNode> pSuccessors) {
        super(pDefinitions, pUses, Integer.MAX_VALUE, Integer.MAX_VALUE, pPredecessors, pSuccessors);
    }

    @Override
    public String toString() {
        return String.format(
                "CFGExitNode: %d %s (%d predecessors, %d successors) | definitions %s | uses %s",
                this.getInsnIdx(), JDFCUtils.getOpcode(this.getOpcode()), this.getPredecessors().size(), this.getSuccessors().size(), this.getDefinitions(), this.getUses());
    }
}
