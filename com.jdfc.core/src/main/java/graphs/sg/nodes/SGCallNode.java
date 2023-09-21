package graphs.sg.nodes;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import data.DomainVariable;
import data.ProgramVariable;
import graphs.cfg.nodes.CFGCallNode;
import lombok.Data;
import utils.JDFCUtils;

import java.util.Objects;

@Data
public class SGCallNode extends SGNode {

    private String calledClassName;
    private String calledMethodName;
    private boolean isInterface;
    private boolean isCalledSGPresent;
    private BiMap<ProgramVariable, ProgramVariable> pVarMap;
    private BiMap<DomainVariable, DomainVariable> dVarMap;
    private int entryNodeIdx;
    private int exitNodeIdx;
    private int returnSiteNodeIdx;

    public SGCallNode(int index, int cfgIndex, CFGCallNode node) {
        super(index, cfgIndex, node);
        this.calledClassName = node.getCalledClassName();
        this.calledMethodName = node.getCalledMethodName();
        this.isInterface = node.isCalledIsInterface();
        this.isCalledSGPresent = true;
        this.pVarMap = HashBiMap.create();
        this.dVarMap = HashBiMap.create();
    }

    @Override
    public String toString() {
        return String.format("%d:%d SGCallNode: lio(%d,%d,%s) (%s::%s) (%s::%s) ps(%d,%d)",
                getIndex(),
                getCfgIndex(),
                getLineNumber(),
                getInsnIndex(),
                JDFCUtils.getOpcode(getOpcode()),
                getClassName(),
                getMethodName(),
                getCalledClassName(),
                getCalledMethodName(),
                getPred().size(),
                getSucc().size());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        SGCallNode that = (SGCallNode) o;
        return getIndex() == that.getIndex()
                && getLineNumber() == that.getLineNumber()
                && getCfgIndex() == that.getCfgIndex()
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
                getLineNumber(),
                getCfgIndex(),
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
