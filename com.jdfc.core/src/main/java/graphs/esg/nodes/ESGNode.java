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

    private ESGNode(int sgnIndex) {
        this.sgnIndex = sgnIndex;
        this.dVar = new DomainVariable.ZeroVariable();
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
        public ESGZeroNode(int sgIndex) {
            super(sgIndex);
        }
    }

    public boolean isZero() {
        return dVar instanceof DomainVariable.ZeroVariable;
    }

    @Override
    public String toString() {
        return String.format("(%d, %s, %d, %d)", sgnIndex, dVar.getName(), pred.size(), succ.size());
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
