package graphs.sg;

import com.google.common.collect.Multimap;
import graphs.cfg.CFG;
import graphs.sg.nodes.SGNode;

import java.util.NavigableMap;
import java.util.Set;

public interface SG {

    public Set<CFG> getCFGs();

    public CFG getMainCFG();

    public SGNode getEntryNode();

    /**
     * Get all nodes of the combined super graph.
     *
     * @return
     */
    NavigableMap<Double, SGNode> getNodes();

    /**
     * Get all edges of the combined super graph.
     *
     * @return
     */
    Multimap<Integer, Integer> getEdges();

    Set<InterVariable> getDomain();
}
