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
public class SGExitNode extends SGNode {

    private BiMap<ProgramVariable, ProgramVariable> pVarMap;
    private Map<DomainVariable, DomainVariable> dVarMap;
    private int callNodeIdx;
    private int entryNodeIdx;
    private int returnSiteNodeIdx;

    public SGExitNode(int index, int cfgIndex, CFGNode node) {
        super(index, cfgIndex, node);
        this.pVarMap = HashBiMap.create();
        this.dVarMap = new HashMap<>();
    }

    @Override
    public String toString() {
        return String.format("%d:%d SGExitNode: lio(%d,%d,%s) (%s::%s) ps(%d,%d)",
                getIndex(),
                getCfgIndex(),
                getLineNumber(),
                getInsnIndex(),
                JDFCUtils.getOpcode(getOpcode()),
                getClassName(),
                getMethodName(),
                getPred().size(),
                getSucc().size());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SGExitNode that = (SGExitNode) o;
        return getIndex() == that.getIndex()
                && getLineNumber() == that.getLineNumber()
                && getCfgIndex() == that.getCfgIndex()
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
