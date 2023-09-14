package graphs.sg.nodes;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import data.DomainVariable;
import data.ProgramVariable;
import graphs.cfg.nodes.CFGNode;
import lombok.Data;
import utils.JDFCUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Data
public class SGEntryNode extends SGNode {

    private BiMap<ProgramVariable, ProgramVariable> useDefMap;
    private Map<DomainVariable, DomainVariable> dVarMap;
    private int callNodeIdx;
    private int exitNodeIdx;
    private int returnSiteNodeIdx;

    public SGEntryNode(int index, CFGNode node) {
        super(index, node);
        this.useDefMap = HashBiMap.create();
        this.dVarMap = new HashMap<>();
    }

    @Override
    public String toString() {
        return String.format(
                "SGEntryNode: %s %s %d %s (%d preds, %d succs)",
                this.getClassName(),
                this.getMethodName(),
                this.getInsnIndex(),
                JDFCUtils.getOpcode(this.getOpcode()),
                this.getPred().size(),
                this.getSucc().size());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SGEntryNode that = (SGEntryNode) o;
        return getIndex() == that.getIndex()
                && getInsnIndex() == that.getInsnIndex()
                && getOpcode() == that.getOpcode()
                && Objects.equals(getClassName(), that.getClassName())
                && Objects.equals(getMethodName(), that.getMethodName());
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }
}
