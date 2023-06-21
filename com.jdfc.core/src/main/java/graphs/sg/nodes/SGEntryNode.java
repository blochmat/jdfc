package graphs.sg.nodes;

import graphs.cfg.nodes.CFGNode;
import utils.JDFCUtils;

public class SGEntryNode extends SGNode {

    public SGEntryNode(String internalMethodName, CFGNode node) {
        super(internalMethodName, node);
    }

    @Override
    public String toString() {
        return String.format(
                "SGEntryNode: %d %s (%d preds, %d succs) | definitions %s | uses %s",
                this.getInsnIndex(), JDFCUtils.getOpcode(this.getOpcode()), this.getPred().size(), this.getSucc().size(), this.getDefinitions(), this.getUses());
    }
}
