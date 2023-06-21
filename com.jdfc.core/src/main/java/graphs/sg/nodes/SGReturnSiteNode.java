package graphs.sg.nodes;

import data.ProgramVariable;
import graphs.cfg.nodes.CFGNode;
import utils.JDFCUtils;

import java.util.Map;

public class SGReturnSiteNode extends SGNode {

    Map<ProgramVariable, ProgramVariable> pVarMap;

    public SGReturnSiteNode(String internalMethodName, CFGNode node, Map<ProgramVariable, ProgramVariable> pVarMap) {
        super(internalMethodName, node);
        this.pVarMap = pVarMap;
    }

    @Override
    public String toString() {
        return String.format(
                "SGReturnSiteNode: %d %s (%d preds, %d succs) | definitions %s | uses %s",
                this.getInsnIndex(), JDFCUtils.getOpcode(this.getOpcode()), this.getPred().size(), this.getSucc().size(), this.getDefinitions(), this.getUses());
    }
}
