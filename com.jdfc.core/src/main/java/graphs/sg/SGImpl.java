package graphs.sg;

import com.google.common.collect.Multimap;
import data.ProgramVariable;
import graphs.sg.nodes.SGCallNode;
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
public class SGImpl implements SG {

    private String internalMethodName;
    private NavigableMap<Integer, SGNode> nodes;
    private Multimap<Integer, Integer> edges;
    private Map<SGCallNode, SGReturnSiteNode> callReturnNodeMap;
    private Map<Integer, Integer> callReturnIdxMap;

    @Override
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

    @Override
    public SGNode getEntryNode() {
        return null;
    }

    @Override
    public NavigableMap<Integer, SGNode> getNodes() {
        return this.nodes;
    }

    @Override
    public Multimap<Integer, Integer> getEdges() {
        return this.edges;
    }

    @Override
    public Set<InterVariable> getDomain() {
        return null;
    }

    @Override
    public String toString() {
        return String.format("SGImpl for method %s (containing %d nodes)%n %s", internalMethodName, nodes.size(), JDFCUtils.prettyPrintMap(nodes));
    }
}
