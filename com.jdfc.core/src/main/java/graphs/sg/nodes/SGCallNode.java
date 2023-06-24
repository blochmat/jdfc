package graphs.sg.nodes;

import data.ProgramVariable;
import graphs.cfg.nodes.CFGCallNode;
import lombok.Data;
import utils.JDFCUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Data
public class SGCallNode extends SGNode {

    private String calledClassName;
    private String calledMethodName;
    private boolean isInterface;
    private Map<ProgramVariable, ProgramVariable> pVarMap;
    private Map<Integer, Integer> dVarMap;

    public SGCallNode(int index, CFGCallNode node) {
        super(index, node);
        this.calledClassName = node.getCalledClassName();
        this.calledMethodName = node.getCalledMethodName();
        this.isInterface = node.isCalledIsInterface();
        this.pVarMap = new HashMap<>();
        this.dVarMap = new HashMap<>();
    }

    public SGCallNode(int index,
                      CFGCallNode node,
                      Map<ProgramVariable, ProgramVariable> pVarMap,
                      Map<Integer, Integer> dVarMap) {
        super(index, node);
        this.calledClassName = node.getCalledClassName();
        this.calledMethodName = node.getCalledMethodName();
        this.isInterface = node.isCalledIsInterface();
        this.pVarMap = pVarMap;
        this.dVarMap = dVarMap;
    }

    @Override
    public String toString() {
        return String.format(
                "SGCallNode: %d %s (%d preds, %d succs) | definitions %s | uses %s",
                this.getInsnIndex(), JDFCUtils.getOpcode(this.getOpcode()), this.getPred().size(), this.getSucc().size(), this.getDefinitions(), this.getUses());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        SGCallNode that = (SGCallNode) o;
        return getIndex() == that.getIndex()
                && getInsnIndex() == that.getInsnIndex()
                && getOpcode() == that.getOpcode()
                && Objects.equals(getClassName(), that.getClassName())
                && Objects.equals(getMethodName(), that.getMethodName())
                && Objects.equals(isInterface(), that.isInterface())
                && Objects.equals(getCalledClassName(), that.getCalledClassName())
                && Objects.equals(getCalledMethodName(), that.getCalledMethodName());
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                getIndex(),
                getInsnIndex(),
                getOpcode(),
                getClassName(),
                getMethodName(),
                isInterface(),
                getCalledClassName(),
                getCalledMethodName());
    }
}
