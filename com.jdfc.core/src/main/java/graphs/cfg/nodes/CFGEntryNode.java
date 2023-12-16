package graphs.cfg.nodes;

import data.ProgramVariable;
import lombok.Data;
import utils.ASMHelper;
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

    private ASMHelper asmHelper = new ASMHelper();

    public CFGEntryNode(
            String className,
            String methodName,
            int methodAccess,
            int lineNumber,
            Set<ProgramVariable> definitions,
            Set<ProgramVariable> uses,
            Set<CFGNode> predecessors,
            Set<CFGNode> successors,
            Map<Integer, ProgramVariable> pVarMap) {
        super(className, methodName, methodAccess, lineNumber, definitions, uses, Integer.MIN_VALUE, Integer.MIN_VALUE, predecessors, successors);
        this.pVarMap = pVarMap;
    }

    @Override
    public void addFieldDefinitions(Set<ProgramVariable> fieldDefinitions) {
        super.addFieldDefinitions(fieldDefinitions);
    }

    @Override
    public String toString() {
        return String.format(
                "%d CFGEntryNode: lio(%d,%d,%s) (%s::%s%s) ps(%d,%d)",
                getIndex(),
                getLineNumber(),
                this.getInsnIndex(),
                JDFCUtils.getOpcode(this.getOpcode()),
                this.getClassName(),
                this.getMethodName(),
                this.asmHelper.isStatic(this.getMethodAccess()) ? "::static" : "",
                this.getPred().size(),
                this.getSucc().size());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CFGEntryNode that = (CFGEntryNode) o;
        return getIndex() == that.getIndex()
                && getInsnIndex() == that.getInsnIndex()
                && getOpcode() == that.getOpcode()
                && getLineNumber() == that.getLineNumber()
                && Objects.equals(getClassName(), that.getClassName())
                && Objects.equals(getMethodName(), that.getMethodName());
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }
}