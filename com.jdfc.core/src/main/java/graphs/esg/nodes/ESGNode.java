package graphs.esg.nodes;

import com.google.common.collect.Sets;
import data.DomainVariable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ESGNode {

    private DomainVariable var;
    private boolean isPossiblyNotRedefined;
    Set<ESGNode> pred;
    Set<ESGNode> succ;

    public ESGNode(DomainVariable var) {
        this.var = var;
        this.isPossiblyNotRedefined = true;
        this.pred = Sets.newLinkedHashSet();
        this.succ = Sets.newLinkedHashSet();
    }

    public boolean isZero() {
        return var instanceof DomainVariable.ZeroVariable;
    }
}
