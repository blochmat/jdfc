package graphs.sg.nodes;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import data.DomainVariable;
import data.ProgramVariable;
import graphs.cfg.nodes.CFGNode;
import lombok.Data;
import utils.JDFCUtils;

import java.util.Objects;

@Data
public class SGEntryNode extends SGNode {

    private BiMap<ProgramVariable, ProgramVariable> pVarMap;
    private BiMap<DomainVariable, DomainVariable> dVarMap;
    private int callNodeIdx;
    private int exitNodeIdx;
    private int returnSiteNodeIdx;

    public SGEntryNode(int index, int cfgIndex, CFGNode node) {
        super(index, cfgIndex, node);
        this.pVarMap = HashBiMap.create();
        this.dVarMap = HashBiMap.create();
    }

    @Override
    public String toString() {
        return String.format("%d:%d SGEntryNode: lio(%d,%d,%s) (%s::%s) ps(%d,%d)",
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
        SGEntryNode that = (SGEntryNode) o;
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
