package icfg.nodes;

import data.ProgramVariable;
import cfg.nodes.CFGNode;
import utils.JDFCUtils;

import java.util.Set;

public class ICFGReturnNode extends CFGNode {
    public ICFGReturnNode(int pIndex, int pOpcode) {
        super(pIndex, pOpcode);
    }

    public ICFGReturnNode(Set<ProgramVariable> pDefinitions, Set<ProgramVariable> pUses, int pIndex, int pOpcode) {
        super(pDefinitions, pUses, pIndex, pOpcode);
    }

    public ICFGReturnNode(Set<ProgramVariable> pDefinitions, Set<ProgramVariable> pUses, int pIndex, int pOpcode, Set<CFGNode> pPredecessors, Set<CFGNode> pSuccessors) {
        super(pDefinitions, pUses, pIndex, pOpcode, pPredecessors, pSuccessors);
    }

    @Override
    public String toString() {
        return String.format(
                "ICFGReturnNode: %d %s (%d predecessors, %d successors)",
                this.getIndex(), JDFCUtils.getOpcode(this.getOpcode()), this.getPredecessors().size(), this.getSuccessors().size());
    }
}
