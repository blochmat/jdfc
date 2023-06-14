package graphs.sg;

import com.google.common.collect.Multimap;
import graphs.sg.nodes.SGNode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import utils.JDFCUtils;

import java.util.NavigableMap;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SGImpl implements SG {

    private String internalMethodName;
    private NavigableMap<Integer, SGNode> nodes;
    private Multimap<Integer, Integer> edges;

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
