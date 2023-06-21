package graphs.cfg.nodes;

import data.ProgramVariable;
import utils.JDFCUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class CFGEntryNode extends CFGNode {

    public CFGEntryNode(Set<ProgramVariable> pDefinitions, Set<ProgramVariable> pUses, Set<CFGNode> pPredecessors, Set<CFGNode> pSuccessors) {
        super(pDefinitions, pUses, Integer.MIN_VALUE, Integer.MIN_VALUE, pPredecessors, pSuccessors);
    }

    @Override
    public String toString() {
        return String.format(
                "CFGEntryNode: %d %s (%d preds, %d succs) | definitions %s | uses %s",
                this.getInsnIndex(), JDFCUtils.getOpcode(this.getOpcode()), this.getPred().size(),
                this.getSucc().size(), this.getDefinitions(), this.getUses());
    }

    // --- Getters, Setters --------------------------------------------------------------------------------------------

    // --- Helper Methods ----------------------------------------------------------------------------------------------


    /**
     * Get all definitions with corresponding position in the method's definition despite from "this".
     * <br>
     * The key is the position of the method param.
     * The value is the program variable of the method param.
     *
     * @return Mapping of position and program variable
     */
    public Map<Integer, ProgramVariable> getPVarArgs() {
        Map<Integer, ProgramVariable> pVarArgs = new HashMap<>();

        // TODO: keep an eye on this
        int counter = 0;
        for(ProgramVariable d : this.getDefinitions()) {
            if (!d.getName().equals("this")) {
                pVarArgs.put(counter, d);
                counter++;
            }
        }

        return pVarArgs;
    }
}
