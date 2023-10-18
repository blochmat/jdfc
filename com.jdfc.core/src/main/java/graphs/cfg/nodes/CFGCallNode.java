package graphs.cfg.nodes;

import com.google.common.collect.Multimap;
import data.DomainVariable;
import data.ProgramVariable;
import lombok.Data;
import utils.ASMHelper;
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
     * The key of this map represents the index of a parameter of the method.
     * The value is the use associated with the index in the method invocation.
     * We use this map to be able to create {@code indexDefinitionMap}.
     */
    private final Map<Integer, ProgramVariable> indexUseMap;

    /**
     * The key of this map represents the index of a parameter of the method.
     * The value is a list of definitions of the current procedure that are associated with an index
     * due to a method invocation.
     * <p>
     * e.g. {0, [defA, defB]} represents the information that there are two possible invocations, namely
     *  1. int some = method(defA);
     *  2. int some = method(defB);
     */
    private Multimap<Integer, ProgramVariable> indexDefinitionsMap;

    private final Map<Integer, DomainVariable> dVarMap;

    private ASMHelper asmHelper = new ASMHelper();

    public CFGCallNode(
            int index,
            int opcode,
            String className,
            String methodName,
            int methodAccess,
            int lineNumber,
            String calledClassName,
            String calledMethodName,
            boolean calledIsInterface,
            Map<Integer, ProgramVariable> indexUseMap,
            Map<Integer, DomainVariable> dVarMap) {
       super(className, methodName, methodAccess, lineNumber, index, opcode);
       this.calledClassName = calledClassName;
       this.calledMethodName = calledMethodName;
       this.calledIsInterface = calledIsInterface;
       this.indexUseMap = indexUseMap;
       this.dVarMap = dVarMap;
    }

    @Override
    public String toString() {
        return String.format(
                "%d CFGCallNode: lio(%d,%d,%s) (%s::%s%s) (%s::%s) ps(%d,%d)",
                this.getIndex(),
                this.getLineNumber(),
                this.getInsnIndex(),
                JDFCUtils.getOpcode(this.getOpcode()),
                this.getClassName(),
                this.getMethodName(),
                this.asmHelper.isStatic(this.getMethodAccess()) ? "::static" : "",
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
        return getIndex() == that.getIndex()
                && getInsnIndex() == that.getInsnIndex()
                && getOpcode() == that.getOpcode()
                && getLineNumber() == that.getLineNumber()
                && Objects.equals(getClassName(), that.getClassName())
                && Objects.equals(getMethodName(), that.getMethodName())
                && Objects.equals(getMethodAccess(), that.getMethodAccess())
                && Objects.equals(isCalledIsInterface(), that.isCalledIsInterface())
                && Objects.equals(getCalledClassName(), that.getCalledClassName())
                && Objects.equals(getCalledMethodName(), that.getCalledMethodName());
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                getIndex(),
                getInsnIndex(),
                getOpcode(),
                getLineNumber(),
                getClassName(),
                getMethodName(),
                getMethodAccess(),
                isCalledIsInterface(),
                getCalledClassName(),
                getCalledMethodName());
    }
}
