package graphs.esg;

import graphs.sg.InterVariable;
import graphs.sg.SGNode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ESGNode {

    private SGNode node;

    private InterVariable var;

    private boolean isPossiblyNotRedefined;

    // if true create edge to succ(node)
}
