package graphs.sg;

import com.google.common.collect.Multimap;
import graphs.cfg.CFG;
import graphs.sg.nodes.SGEntryNode;
import graphs.sg.nodes.SGNode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import utils.JDFCUtils;

import java.util.Map;
import java.util.NavigableMap;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SG {

    private String className;
    private String methodName;
    private Map<String, CFG> cfgMap;
    private NavigableMap<Integer, SGNode> nodes;
    private Multimap<Integer, Integer> edges;
    private Map<Integer, Integer> returnSiteIndexMap;

    /**
     * Matches a procedure name to all call nodes calling the procedure
     */
    private Multimap<String, Integer> callersMap;

    public SGEntryNode getEntryNode() {
        return (SGEntryNode) this.nodes.get(0);
    }

    public SGNode getExitNode() {
        return this.nodes.get(this.nodes.size() - 1);
    }

    @Override
    public String toString() {
        return String.format("SG for method %s (containing %d nodes)%n %s", methodName, nodes.size(), JDFCUtils.prettyPrintMap(nodes));
    }
}
