package icfg.nodes;

import data.ProgramVariable;
import cfg.nodes.CFGNode;
import utils.JDFCUtils;

import java.util.Set;

public class ICFGCallNode extends CFGNode {
    private final String methodName;

    public ICFGCallNode(int pIndex, int pOpcode, String methodName) {
        super(pIndex, pOpcode);
        this.methodName = methodName;
    }

    public ICFGCallNode(Set<ProgramVariable> pDefinitions, Set<ProgramVariable> pUses, int pIndex, int pOpcode, String methodName) {
        super(pDefinitions, pUses, pIndex, pOpcode);
        this.methodName = methodName;
    }

    public ICFGCallNode(Set<ProgramVariable> pDefinitions, Set<ProgramVariable> pUses, int pIndex, int pOpcode, Set<CFGNode> pPredecessors, Set<CFGNode> pSuccessors, String methodName) {
        super(pDefinitions, pUses, pIndex, pOpcode, pPredecessors, pSuccessors);
        this.methodName = methodName;
    }

    public String getMethodName() {
        return methodName;
    }

    @Override
    public String toString() {
        return String.format(
                "ICFGCallNode: %d %s %s (%d predecessors, %d successors)",
                this.getIndex(), JDFCUtils.getOpcode(this.getOpcode()), this.methodName, this.getPredecessors().size(), this.getSuccessors().size());
    }
}
