package graphs.esg;

import com.google.common.collect.Multimap;
import graphs.esg.nodes.ESGNode;
import graphs.sg.SG;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.NavigableMap;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ESG {
    private SG sg;
    private NavigableMap<Integer, NavigableMap<Integer, ESGNode>> nodes;
    private Multimap<Integer, ESGEdge> edges;
}
