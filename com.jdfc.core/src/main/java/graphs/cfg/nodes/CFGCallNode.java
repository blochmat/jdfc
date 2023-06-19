package graphs.cfg.nodes;

import data.ProgramVariable;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import utils.JDFCUtils;

import java.util.Map;
import java.util.Set;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class CFGCallNode extends CFGNode {

    String owner;
    String shortInternalMethodName;
    boolean isInterface;
    Map<Integer, ProgramVariable> pVarArgs;

    public CFGCallNode(int index, int opcode, String owner, String shortInternalMethodName, boolean isInterface, Map<Integer, ProgramVariable> pVarArgs) {
       super(index, opcode);
       this.owner = owner;
       this.shortInternalMethodName = shortInternalMethodName;
       this.isInterface = isInterface;
       this.pVarArgs = pVarArgs;
    }

    public CFGCallNode(Set<ProgramVariable> pDefinitions, Set<ProgramVariable> pUses, Set<CFGNode> pPredecessors,
                       Set<CFGNode> pSuccessors) {
        super(pDefinitions, pUses, Integer.MIN_VALUE, Integer.MIN_VALUE, pPredecessors, pSuccessors);
    }

    @Override
    public String toString() {
        return String.format(
                "CFGCallNode: %d %s %s %s (%d preds, %d succs) | definitions %s | uses %s",
                this.getInsnIndex(), JDFCUtils.getOpcode(this.getOpcode()), this.owner, this.shortInternalMethodName,
                this.getPred().size(), this.getSucc().size(), this.getDefinitions(), this.getUses());
    }
}
