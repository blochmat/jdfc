package graphs.cfg.nodes;

import data.ProgramVariable;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import utils.JDFCUtils;

import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class CFGCallNode extends CFGNode {

    /**
     * Name of the called method's owner class (relative path).
     */
    String owner;

    /**
     * Name of the called method (ASM descriptor without exceptions).
     */
    String shortInternalMethodName;

    /**
     * If the methods owner class is an interface
     */
    boolean isInterface;

    /**
     * Mapping of all program variables (value) with position in the method call (key). <br>
     * NOTE: Might contain viewer params than defined for the method, because constant params are not included.
     */
    Map<Integer, ProgramVariable> pVarArgs;

    /**
     *
     * @param index index in the control flow graph
     * @param opcode opcode of the instruction
     * @param owner name of the called methods owner class
     * @param shortInternalMethodName name of the called method (ASM descriptor without exceptions)
     * @param isInterface if the methods owner class is an interface
     * @param pVarArgs mapping of passed program variables (value) with position in method call (key)
     */
    public CFGCallNode(int index, int opcode, String owner, String shortInternalMethodName, boolean isInterface, Map<Integer, ProgramVariable> pVarArgs) {
       super(index, opcode);
       this.owner = owner;
       this.shortInternalMethodName = shortInternalMethodName;
       this.isInterface = isInterface;
       this.pVarArgs = pVarArgs;
    }

    @Override
    public String toString() {
        return String.format(
                "CFGCallNode: %d %s %s %s (%d preds, %d succs) | definitions %s | uses %s",
                this.getInsnIndex(), JDFCUtils.getOpcode(this.getOpcode()), this.owner, this.shortInternalMethodName,
                this.getPred().size(), this.getSucc().size(), this.getDefinitions(), this.getUses());
    }
}
