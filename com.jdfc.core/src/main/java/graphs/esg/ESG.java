package graphs.esg;

import com.google.common.collect.Multimap;
import graphs.sg.SG;

import java.util.NavigableMap;
import java.util.Set;

public interface ESG {

    SG getSg();

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
