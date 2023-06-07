package algos;

import graphs.esg.ESG;
import graphs.esg.ESGEdge;
import graphs.esg.ESGNode;
import graphs.sg.ZeroVariable;

import java.util.*;

public class TabulationAlgorithm {

    private ESG graph;

    private Set<ESGEdge> pathEdgeSet;

    private Set<ESGEdge> summaryEdgeSet;

    private LinkedList<ESGEdge> workList;

    public TabulationAlgorithm(ESG graph) {
        this.graph = graph;
        this.pathEdgeSet = new HashSet<>();
        this.summaryEdgeSet = new HashSet<>();
        this.workList = new LinkedList<>();

        ESGNode mainNode = new ESGNode(graph.getSg().getEntryNode(), new ZeroVariable(), true);
        pathEdgeSet.add(new ESGEdge(0, mainNode, 0, mainNode));
        this.workList.add(new ESGEdge(0, mainNode, 0, mainNode));
    }

    public void execute() {
        while(!this.workList.isEmpty()) {
            ESGEdge e = workList.pop();
        }
    }

}
