package algos;

import com.google.common.collect.Sets;
import graphs.esg.ESG;
import graphs.esg.ESGEdge;
import graphs.sg.SG;
import lombok.Data;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

@Data
public class TabulationAlgorithm {

    private ESG esg;

    private Set<ESGEdge> pathEdgeSet;

    private Set<ESGEdge> summaryEdgeSet;

    private LinkedList<ESGEdge> workList;

    public TabulationAlgorithm(ESG esg) {
        this.esg = esg;
        this.pathEdgeSet = Sets.newLinkedHashSet();
        this.workList = new LinkedList<>();
        this.summaryEdgeSet = new HashSet<>();
    }

    public void execute() {
        SG sg = this.esg.getSg();
        //--- ForwardTabulateSLRPs -------------------------------------------------------------------------------------
//        this.pathEdgeSet.add(new ESGEdge(Integer.MIN_VALUE, Integer.MIN_VALUE, -1, -1));
//        this.workList.add(new ESGEdge(Integer.MIN_VALUE, Integer.MIN_VALUE, -1, -1));
//        while(!this.workList.isEmpty()) {
//            ESGEdge e = workList.pop();
//            SGNode sgNode = sg.getNodes().get(e.getSgnTargetIdx());
//
//            if (sgNode instanceof SGCallNode) {
//                SGCallNode n = (SGCallNode) sgNode;
//
//                Map<Integer, ESGNode> esgNodeMap = this.esg.getNodes().get(n.getIndex());
//
//                for(Map.Entry<Integer, ESGNode> esgNodeEntry : esgNodeMap.entrySet()) {
//                    Collection<ESGEdge> edges = this.esg.getEdges().get(n.getIndex());
//
//                }
//
//            } else if (e.getTarget() instanceof ESGExitNode) {
//            } else {
//            }
//        }

        //--- When finished --------------------------------------------------------------------------------------------
    }

    private void propagate(ESGEdge e) {
        this.pathEdgeSet.add(e);
        this.workList.add(e);
    }

}
