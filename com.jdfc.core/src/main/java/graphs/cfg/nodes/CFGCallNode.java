package graphs.cfg.nodes;

import data.DomainVariable;
import data.ProgramVariable;
import lombok.Data;
import lombok.EqualsAndHashCode;
import utils.JDFCUtils;

import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = true)
public class CFGCallNode extends CFGNode {

    /**
     * Name of the called method's owner class (relative path).
     */
    private final String className;

    /**
     * Name of the called method (ASM descriptor without exceptions).
     */
    private final String methodName;

    /**
     * If the methods owner class is an interface
     */
    private final boolean isInterface;

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
     * @param className name of the called methods owner class
     * @param methodName name of the called method (ASM descriptor without exceptions)
     * @param isInterface if the methods owner class is an interface
     * @param pVarMap mapping of passed program variables (value) with position in method call (key)
     * @param dVarMap mapping of passed domain variables (value) with position in method call (key)
     */
    public CFGCallNode(
            int index,
            int opcode,
            String className,
            String methodName,
            boolean isInterface,
            Map<Integer, ProgramVariable> pVarMap,
            Map<Integer, DomainVariable> dVarMap) {
       super(index, opcode);
       this.className = className;
       this.methodName = methodName;
       this.isInterface = isInterface;
       this.pVarMap = pVarMap;
       this.dVarMap = dVarMap;
    }

    @Override
    public String toString() {
        return String.format(
                "CFGCallNode: %d %s %s %s (%d preds, %d succs) | definitions %s | uses %s",
                this.getInsnIndex(), JDFCUtils.getOpcode(this.getOpcode()), this.className, this.methodName,
                this.getPred().size(), this.getSucc().size(), this.getDefinitions(), this.getUses());
    }
}
