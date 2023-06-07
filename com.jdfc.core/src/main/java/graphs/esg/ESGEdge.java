package graphs.esg;

import graphs.esg.nodes.ESGNode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ESGEdge {
    int sourceIdx;
    ESGNode source;
    int targetIdx;
    ESGNode target;
}
