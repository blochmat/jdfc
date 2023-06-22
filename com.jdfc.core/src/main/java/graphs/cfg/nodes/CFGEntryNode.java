package graphs.cfg.nodes;

import data.DomainVariable;
import data.ProgramVariable;
import lombok.Data;
import utils.JDFCUtils;

import java.util.Map;
import java.util.Set;

@Data
public class CFGEntryNode extends CFGNode {

    /**
     * Name of the called method's owner class (relative path).
     */
    private final String className;

    /**
     * Name of the called method (ASM descriptor without exceptions).
     */
    private final String methodName;

    /**
     * Mapping of all program variables (value) with position of appearance in the method call (key). <br>
     * Additionally contains all other local variables defined in the method as they do not get in the way when matching
     * parameters apart from "this".
     *
     */
    private final Map<Integer, ProgramVariable> pVarMap;

    /**
     * Mapping of all domain variables (value) with position of appearance in the method call (key) apart from "this" <br>
     */
    private final Map<Integer, DomainVariable> dVarMap;

    public CFGEntryNode(
            String className,
            String methodName,
            Set<ProgramVariable> pDefinitions,
            Set<ProgramVariable> pUses,
            Set<CFGNode> pPredecessors,
            Set<CFGNode> pSuccessors,
            Map<Integer, ProgramVariable> pVarMap,
            Map<Integer, DomainVariable> dVarMap) {
        super(pDefinitions, pUses, Integer.MIN_VALUE, Integer.MIN_VALUE, pPredecessors, pSuccessors);
        this.className = className;
        this.methodName = methodName;
        this.pVarMap = pVarMap;
        this.dVarMap = dVarMap;
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
}