package graphs.sg.nodes;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import data.ProgramVariable;
import graphs.cfg.nodes.CFGCallNode;
import lombok.Data;
import utils.ASMHelper;
import utils.JDFCUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Data
public class SGCallNode extends SGNode {

    private ASMHelper asmHelper = new ASMHelper();

    private String calledClassName;
    private String calledMethodName;
    private boolean isInterface;
    private boolean isCalledSGPresent;

    /**
     * The key is a definition of the invoked procedure.
     * The value is a definition of the invoking procedure.
     */
    private Map<ProgramVariable, ProgramVariable> definitionsMap;
    private int exitNodeIdx;
    private int returnSiteNodeIdx;

    public SGCallNode(int index, int cfgIndex, int entryNodeIdx, CFGCallNode node) {
        super(index, cfgIndex, entryNodeIdx, node);
        this.calledClassName = node.getCalledClassName();
        this.calledMethodName = node.getCalledMethodName();
        this.isInterface = node.isCalledIsInterface();
        this.isCalledSGPresent = true;
        this.definitionsMap = new HashMap<>();
    }

    @Override
    public String toString() {
        return String.format("%d:%d SGCallNode: lio(%d,%d,%s) (%s::%s%s) (%s::%s) ps(%d,%d)",
                getIndex(),
                getCfgIndex(),
                getLineNumber(),
                getInsnIndex(),
                JDFCUtils.getOpcode(getOpcode()),
                getClassName(),
                getMethodName(),
                this.asmHelper.isStatic(getMethodAccess()) ? "::static" : "",
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
                && getEntryNodeIdx() == that.getEntryNodeIdx()
                && getInsnIndex() == that.getInsnIndex()
                && getOpcode() == that.getOpcode()
                && Objects.equals(getClassName(), that.getClassName())
                && Objects.equals(getMethodName(), that.getMethodName())
                && Objects.equals(getMethodAccess(), that.getMethodAccess())
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
                getEntryNodeIdx(),
                getCfgIndex(),
                getInsnIndex(),
                getOpcode(),
                getClassName(),
                getMethodName(),
                getMethodAccess(),
                isInterface(),
                isCalledSGPresent(),
                getCalledClassName(),
                getCalledMethodName());
    }
}
