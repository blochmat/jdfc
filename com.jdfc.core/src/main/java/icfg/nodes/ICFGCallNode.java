package icfg.nodes;

import icfg.data.ProgramVariable;
import utils.JDFCUtils;

import java.util.Set;

public class ICFGCallNode extends ICFGNode {
    public ICFGCallNode(int pIndex, int pOpcode) {
        super(pIndex, pOpcode);
    }

    public ICFGCallNode(Set<ProgramVariable> pDefinitions, Set<ProgramVariable> pUses, int pIndex, int pOpcode) {
        super(pDefinitions, pUses, pIndex, pOpcode);
    }

    public ICFGCallNode(Set<ProgramVariable> pDefinitions, Set<ProgramVariable> pUses, int pIndex, int pOpcode, Set<ICFGNode> pPredecessors, Set<ICFGNode> pSuccessors) {
        super(pDefinitions, pUses, pIndex, pOpcode, pPredecessors, pSuccessors);
    }

    @Override
    public String toString() {
        return String.format(
                "ICFGCallNode: %d %s (%d predecessors, %d successors)",
                this.getIndex(), JDFCUtils.getOpcode(this.getOpcode()), this.getPredecessors().size(), this.getSuccessors().size());
    }
}
