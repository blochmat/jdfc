package graphs.esg;

import graphs.esg.nodes.ESGNode;
import graphs.sg.SG;
import lombok.Data;

import java.util.NavigableMap;

@Data
public class ESGImpl {

    private SG sg;
    private NavigableMap<Integer, ESGNode> nodes;
}
