package graphs.esg.nodes;

import com.google.common.collect.Sets;
import data.ProgramVariable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import utils.JDFCUtils;

import java.util.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ESGNode {

    private int sgnIndex;
    private ProgramVariable var;
    private boolean isPossiblyNotRedefined;
    Set<ESGNode> pred;
    Set<ESGNode> succ;
    private int idx;
    /**
     * Stores the call idx and the corresponding method id
     * e.g.
     *      1 -> class::method
     */
    private Map<Integer, String> callIdxMethodIdMap = new TreeMap<>();
    /**
     * Stores the call idx and the corresponding variables
     * e.g.
     *      1 -> id1 -> var1,
     *      1 -> id2 -> var2
     */
    private Map<Integer, Map<UUID, ProgramVariable>> callIdxVarMaps = new TreeMap<>();
    /**
     * Stores the call idx and the corresponding liveness of variables
     * e.g.
     *      1 -> id1 -> true,
     *      1 -> id2 -> false
     */
    private Map<Integer, Map<UUID, Boolean>> callIdxLiveVarMap = new TreeMap<>();
    /**
     * Unused
     */
    private Map<Integer, Map<UUID, Boolean>> callIdxPosNotReMap = new TreeMap<>();
    /**
     * Unused
     */
    private List<String> callSequenceIdx;
    /**
     * Stores the call idx and definition matches
     * e.g.
     *      1 -> id1 -> id2
     *      1 -> id3 -> id4
     */
    private Map<Integer, Map<UUID, UUID>> definitionMaps = new HashMap<>();

    public ESGNode(int idx) {
        this.idx = idx;
    }

    private ESGNode(int sgnIndex, String className, String methodName) {
        this.sgnIndex = sgnIndex;
        this.var = new ProgramVariable.ZeroVariable(className, methodName);
        this.isPossiblyNotRedefined = true;
        this.pred = Sets.newLinkedHashSet();
        this.succ = Sets.newLinkedHashSet();
    }

    public ESGNode(int sgnIndex, ProgramVariable var) {
        this.sgnIndex = sgnIndex;
        this.var = var;
        this.isPossiblyNotRedefined = false;
        this.pred = Sets.newLinkedHashSet();
        this.succ = Sets.newLinkedHashSet();
    }

    public static class ESGZeroNode extends ESGNode {
        public ESGZeroNode(int sgIndex, String className, String methodName) {
            super(sgIndex, className, methodName);
        }
    }

    public boolean isZero() {
        return var instanceof ProgramVariable.ZeroVariable;
    }

    @Override
    public String toString() {
//        String redefined = isPossiblyNotRedefined ? "T" : "F";
//        if(var.getInstructionIndex() != Integer.MIN_VALUE) {
//            return String.format("(%d, %s:%d, %d, %d, %s)",
//                    sgnIndex,
//                    var.getName(),
//                    var.getInstructionIndex(),
//                    pred.size(),
//                    succ.size(),
//                    redefined);
//        }
        return String.format("(%d: %s)",
                idx,
                JDFCUtils.prettyPrintMap(callIdxMethodIdMap));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ESGNode esgNode = (ESGNode) o;
        return getSgnIndex() == esgNode.getSgnIndex() && isPossiblyNotRedefined() == esgNode.isPossiblyNotRedefined() && Objects.equals(getVar(), esgNode.getVar());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getSgnIndex(), getVar(), isPossiblyNotRedefined());
    }
}
