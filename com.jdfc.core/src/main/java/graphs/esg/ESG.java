package graphs.esg;

import com.google.common.collect.Multimap;
import graphs.esg.nodes.ESGNode;
import graphs.sg.SGImpl;

import java.util.NavigableMap;
import java.util.Set;

public interface ESG {

    SGImpl getSg();

    /**
     * Get all nodes of the combined super graph.
     *
     * @return
     */
    NavigableMap<Integer, ESGNode> getNodes();

    /**
     * Get all edges of the combined super graph.
     *
     * @return
     */
    Multimap<Integer, Integer> getEdges();

    Set<ESGEdge> getESGEdges();
}
