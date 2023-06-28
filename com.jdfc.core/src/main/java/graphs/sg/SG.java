package graphs.sg;

import com.google.common.collect.Multimap;
import data.ProgramVariable;
import graphs.cfg.CFG;
import graphs.sg.nodes.SGCallNode;
import graphs.sg.nodes.SGEntryNode;
import graphs.sg.nodes.SGNode;
import graphs.sg.nodes.SGReturnSiteNode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import utils.JDFCUtils;

import java.util.LinkedList;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SG {

    private String className;
    private String methodName;
    private Map<String, CFG> cfgMap;
    private NavigableMap<Integer, SGNode> nodes;
    private Multimap<Integer, Integer> edges;
    private Map<SGCallNode, SGReturnSiteNode> returnSiteNodeMap;
    private Map<Integer, Integer> returnSiteIndexMap;
    private Multimap<String, SGCallNode> callersMap;

    public void calculateReachingDefinitions() {
        LinkedList<SGNode> workList = new LinkedList<>();
        for (Map.Entry<Integer, SGNode> node : nodes.entrySet()) {
            node.getValue().resetReachOut();
            workList.add(node.getValue());
        }

        while (!workList.isEmpty()) {
            SGNode node = workList.poll();
            Set<ProgramVariable> oldValue = node.getReachOut();
            node.update();
            if (!node.getReachOut().equals(oldValue)) {
                workList.addAll(node.getPred());
            }
        }
    }

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
