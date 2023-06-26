package graphs.esg.nodes;

import com.google.common.collect.Sets;
import data.DomainVariable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Objects;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ESGNode {

    private int sgnIndex;
    private DomainVariable dVar;
    private boolean isPossiblyNotRedefined;
    Set<ESGNode> pred;
    Set<ESGNode> succ;

    private ESGNode(int sgnIndex, String className, String methodName) {
        this.sgnIndex = sgnIndex;
        this.dVar = new DomainVariable.ZeroVariable(className, methodName);
        this.isPossiblyNotRedefined = true;
        this.pred = Sets.newLinkedHashSet();
        this.succ = Sets.newLinkedHashSet();
    }

    public ESGNode(int sgnIndex, DomainVariable dVar) {
        this.sgnIndex = sgnIndex;
        this.dVar = dVar;
        this.isPossiblyNotRedefined = true;
        this.pred = Sets.newLinkedHashSet();
        this.succ = Sets.newLinkedHashSet();
    }

    public static class ESGZeroNode extends ESGNode {
        public ESGZeroNode(int sgIndex, String className, String methodName) {
            super(sgIndex, className, methodName);
        }
    }

    public boolean isZero() {
        return dVar instanceof DomainVariable.ZeroVariable;
    }

    @Override
    public String toString() {
        String redefined = isPossiblyNotRedefined ? "T" : "F";
        return String.format("(%d, %s, %s, %d, %d, %s)", sgnIndex, dVar.getIndex(), dVar.getName(), pred.size(), succ.size(), redefined);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ESGNode esgNode = (ESGNode) o;
        return getSgnIndex() == esgNode.getSgnIndex() && isPossiblyNotRedefined() == esgNode.isPossiblyNotRedefined() && Objects.equals(dVar, esgNode.dVar);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getSgnIndex(), dVar, isPossiblyNotRedefined());
    }
}
