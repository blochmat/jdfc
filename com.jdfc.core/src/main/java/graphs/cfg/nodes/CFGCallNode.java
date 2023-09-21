package graphs.cfg.nodes;

import data.DomainVariable;
import data.ProgramVariable;
import lombok.Data;
import utils.JDFCUtils;

import java.util.Map;
import java.util.Objects;

@Data
public class CFGCallNode extends CFGNode {

    /**
     * Name of the called method's owner class (relative path).
     */
    private final String calledClassName;

    /**
     * Name of the called method (ASM descriptor without exceptions).
     */
    private final String calledMethodName;

    /**
     * If the methods owner class is an interface
     */
    private final boolean calledIsInterface;

    /**
     * Mapping of all program variables (value) with position in the method call (key). <br>
     * NOTE: Might contain viewer params than defined for the method, because constant params are not included.
     */
    private final Map<Integer, ProgramVariable> pVarMap;

    /**
     * Mapping of all domain variables (value) with position in the method call (key). <br>
     * NOTE: Might contain viewer params than defined for the method, because constant params are not included.
     */
    private final Map<Integer, DomainVariable> dVarMap;

    /**
     *
     * @param index index in the control flow graph
     * @param opcode opcode of the instruction
     * @param className name of the enclosing method's owner class (relative path)
     * @param methodName name of the enclosing method (ASM internal name)
     * @param calledClassName name of the called method's owner class (relative path)
     * @param calledMethodName name of the called method (ASM internal name)
     * @param calledIsInterface if the called methods owner class is an interface
     * @param pVarMap mapping of passed program variables (value) with position in method call (key)
     * @param dVarMap mapping of passed domain variables (value) with position in method call (key)
     */
    public CFGCallNode(
            int index,
            int opcode,
            String className,
            String methodName,
            int lineNumber,
            String calledClassName,
            String calledMethodName,
            boolean calledIsInterface,
            Map<Integer, ProgramVariable> pVarMap,
            Map<Integer, DomainVariable> dVarMap) {
       super(className, methodName, lineNumber, index, opcode);
       this.calledClassName = calledClassName;
       this.calledMethodName = calledMethodName;
       this.calledIsInterface = calledIsInterface;
       this.pVarMap = pVarMap;
       this.dVarMap = dVarMap;
    }

    @Override
    public String toString() {
        return String.format(
                "%d CFGCallNode: lio(%d,%d,%s) (%s::%s) (%s::%s) ps(%d,%d)",
                this.getIndex(),
                this.getLineNumber(),
                this.getInsnIndex(),
                JDFCUtils.getOpcode(this.getOpcode()),
                this.getClassName(),
                this.getMethodName(),
                this.calledClassName,
                this.calledMethodName,
                this.getPred().size(),
                this.getSucc().size());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        CFGCallNode that = (CFGCallNode) o;
        return getLineNumber() == that.getLineNumber()
                && getInsnIndex() == that.getInsnIndex()
                && getOpcode() == that.getOpcode()
                && Objects.equals(getClassName(), that.getClassName())
                && Objects.equals(getMethodName(), that.getMethodName())
                && Objects.equals(isCalledIsInterface(), that.isCalledIsInterface())
                && Objects.equals(getCalledClassName(), that.getCalledClassName())
                && Objects.equals(getCalledMethodName(), that.getCalledMethodName());
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                getLineNumber(),
                getInsnIndex(),
                getOpcode(),
                getClassName(),
                getMethodName(),
                isCalledIsInterface(),
                getCalledClassName(),
                getCalledMethodName());
    }
}
