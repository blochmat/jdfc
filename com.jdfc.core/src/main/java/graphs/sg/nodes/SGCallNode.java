package graphs.sg.nodes;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import data.DomainVariable;
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
    private boolean isCalledSGPresent;
    private BiMap<ProgramVariable, ProgramVariable> useDefMap;
    private Map<DomainVariable, DomainVariable> dVarMap;

    public SGCallNode(int index, CFGCallNode node) {
        super(index, node);
        this.calledClassName = node.getCalledClassName();
        this.calledMethodName = node.getCalledMethodName();
        this.isInterface = node.isCalledIsInterface();
        this.isCalledSGPresent = true;
        this.useDefMap = HashBiMap.create();
        this.dVarMap = new HashMap<>();
    }

    public SGCallNode(int index,
                      CFGCallNode node,
                      BiMap<ProgramVariable, ProgramVariable> useDefMap,
                      Map<DomainVariable, DomainVariable> dVarMap) {
        super(index, node);
        this.calledClassName = node.getCalledClassName();
        this.calledMethodName = node.getCalledMethodName();
        this.isInterface = node.isCalledIsInterface();
        this.isCalledSGPresent = true;
        this.useDefMap = useDefMap;
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
                && Objects.equals(isCalledSGPresent(), that.isCalledSGPresent())
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
                isCalledSGPresent(),
                getCalledClassName(),
                getCalledMethodName());
    }
}
