package graphs.sg.nodes;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Multimap;
import data.DomainVariable;
import data.ProgramVariable;
import graphs.cfg.nodes.CFGNode;
import lombok.Data;
import utils.JDFCUtils;

import java.util.Objects;

@Data
public class SGEntryNode extends SGNode {

    /**
     * The key is a definition of the current procedure.
     * The value is a list of definitions of the invoking procedure.
     */
    private Multimap<ProgramVariable, ProgramVariable> definitionsMap;
    private BiMap<DomainVariable, DomainVariable> dVarMap;
    private int callNodeIdx;
    private int exitNodeIdx;
    private int returnSiteNodeIdx;

    public SGEntryNode(int index, int cfgIndex, CFGNode node) {
        super(index, cfgIndex, index, node);
        this.definitionsMap = ArrayListMultimap.create();
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
                && getEntryNodeIdx() == that.getEntryNodeIdx()
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
