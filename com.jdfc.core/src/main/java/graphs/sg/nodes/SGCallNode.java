package graphs.sg.nodes;

import data.ProgramVariable;
import graphs.cfg.nodes.CFGNode;
import utils.JDFCUtils;

import java.util.Map;
import java.util.Set;

public class SGCallNode extends SGNode {

    private Map<ProgramVariable, ProgramVariable> pVarMap;

    public SGCallNode(String internalMethodName, CFGNode node, Map<ProgramVariable, ProgramVariable> pVarMap) {
        super(internalMethodName, node);
        this.pVarMap = pVarMap;
    }

    public SGCallNode(Set<ProgramVariable> pDefinitions, Set<ProgramVariable> pUses, Set<CFGNode> pPredecessors, Set<CFGNode> pSuccessors) {
        super(pDefinitions, pUses, Integer.MIN_VALUE, Integer.MIN_VALUE, pPredecessors, pSuccessors);
    }

    @Override
    public String toString() {
        return String.format(
                "SGCallNode: %d %s (%d preds, %d succs) | definitions %s | uses %s",
                this.getInsnIndex(), JDFCUtils.getOpcode(this.getOpcode()), this.getPred().size(), this.getSucc().size(), this.getDefinitions(), this.getUses());
    }

    // --- Getters, Setters --------------------------------------------------------------------------------------------

    public Map<ProgramVariable, ProgramVariable> getpVarMap() {
        return pVarMap;
    }

    public void setpVarMap(Map<ProgramVariable, ProgramVariable> pVarMap) {
        this.pVarMap = pVarMap;
    }
}
