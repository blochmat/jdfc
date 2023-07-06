package graphs.cfg.nodes;

import data.DomainVariable;
import data.ProgramVariable;
import lombok.Data;
import utils.JDFCUtils;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Data
public class CFGEntryNode extends CFGNode {

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
        super(className, methodName, pDefinitions, pUses, Integer.MIN_VALUE, Integer.MIN_VALUE, pPredecessors, pSuccessors);
        this.pVarMap = pVarMap;
        this.dVarMap = dVarMap;
    }

    @Override
    public void addFieldDefinitions(Set<ProgramVariable> fieldDefinitions) {
        super.addFieldDefinitions(fieldDefinitions);
    }

    @Override
    public String toString() {
        return String.format(
                "CFGEntryNode: %d %s (%d preds, %d succs) | definitions %s | uses %s",
                this.getInsnIndex(), JDFCUtils.getOpcode(this.getOpcode()), this.getPred().size(),
                this.getSucc().size(), this.getDefinitions(), this.getUses());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CFGEntryNode that = (CFGEntryNode) o;
        return getInsnIndex() == that.getInsnIndex()
                && getOpcode() == that.getOpcode()
                && Objects.equals(getClassName(), that.getClassName())
                && Objects.equals(getMethodName(), that.getMethodName());
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    // --- Getters, Setters --------------------------------------------------------------------------------------------

    // --- Helper Methods ----------------------------------------------------------------------------------------------
}