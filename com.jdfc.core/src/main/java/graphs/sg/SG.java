package graphs.sg;

import com.google.common.collect.Multimap;
import data.DomainVariable;
import graphs.sg.nodes.SGNode;

import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;

public interface SG {

    void calculateReachingDefinitions();

    public SGNode getEntryNode();

    /**
     * Get all nodes of the combined super graph.
     *
     * @return
     */
    NavigableMap<Integer, SGNode> getNodes();

    /**
     * Get all edges of the combined super graph.
     *
     * @return
     */
    Multimap<Integer, Integer> getEdges();

    Set<DomainVariable> getDomain();

    Map<DomainVariable, DomainVariable> getDomainVarMap();

    @Override
    public String toString();
}
