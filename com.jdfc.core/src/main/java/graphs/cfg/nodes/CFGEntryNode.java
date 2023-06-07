package graphs.cfg.nodes;

import data.ProgramVariable;
import utils.JDFCUtils;

import java.util.Set;

public class CFGEntryNode extends CFGNode {

    public CFGEntryNode(Set<ProgramVariable> pDefinitions, Set<ProgramVariable> pUses, Set<CFGNode> pPredecessors, Set<CFGNode> pSuccessors) {
        super(pDefinitions, pUses, Integer.MIN_VALUE, Integer.MIN_VALUE, pPredecessors, pSuccessors);
    }

    @Override
    public String toString() {
        return String.format(
                "CFGEntryNode: %d %s (%d predecessors, %d successors) | definitions %s | uses %s",
                this.getIndex(), JDFCUtils.getOpcode(this.getOpcode()), this.getPredecessors().size(), this.getSuccessors().size(), this.getDefinitions(), this.getUses());
    }
}
