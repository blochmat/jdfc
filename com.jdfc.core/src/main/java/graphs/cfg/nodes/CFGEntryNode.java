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
            int lineNumber,
            Set<ProgramVariable> definitions,
            Set<ProgramVariable> uses,
            Set<CFGNode> predecessors,
            Set<CFGNode> successors,
            Map<Integer, ProgramVariable> pVarMap,
            Map<Integer, DomainVariable> dVarMap) {
        super(className, methodName, lineNumber, definitions, uses, Integer.MIN_VALUE, Integer.MIN_VALUE, predecessors, successors);
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
                "CFGEntryNode: lio(%d,%d,%s) (%s::%s) ps(%d,%d)",
                getLineNumber(),
                this.getInsnIndex(),
                JDFCUtils.getOpcode(this.getOpcode()),
                this.getClassName(),
                this.getMethodName(),
                this.getPred().size(),
                this.getSucc().size());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CFGEntryNode that = (CFGEntryNode) o;
        return getLineNumber() == that.getLineNumber()
                && getInsnIndex() == that.getInsnIndex()
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