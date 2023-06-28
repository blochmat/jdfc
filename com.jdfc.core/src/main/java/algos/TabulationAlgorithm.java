package algos;

import com.google.common.collect.Sets;
import graphs.esg.ESG;
import graphs.esg.ESGEdge;
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
//        SG sg = this.esg.getSg();
//        String mainMethodIdentifier = String.format("%s :: %s", sg.getClassName(), sg.getMethodName());
//        //--- ForwardTabulateSLRPs -------------------------------------------------------------------------------------
//        ESGEdge initialEdge = new ESGEdge(0, 0, mainMethodIdentifier, mainMethodIdentifier, -1, -1);
//        this.pathEdgeSet.add(initialEdge);
//        this.workList.add(initialEdge);
//
//        while(!workList.isEmpty()) {
//            ESGEdge currEdge = workList.pop();
//            SGNode s = sg.getNodes().get(currEdge.getSgnSourceIdx());
//            DomainVariable d1 = esg.getDomain().get(currEdge.getSourceDVarMethodName()).get(currEdge.getSourceDVarIdx());
//            String d1MethodIdentifier = String.format("%s :: %s", d1.getClassName(), d1.getMethodName());
//
//            SGNode n = sg.getNodes().get(currEdge.getSgnTargetIdx());
//            DomainVariable d2 = esg.getDomain().get(currEdge.getTargetDVarMethodName()).get(currEdge.getTargetDVarIdx());
//            String d2MethodIdentifier = String.format("%s :: %s", d2.getClassName(), d2.getMethodName());
//
//            if(n instanceof SGCallNode) {
//                SGCallNode nCall = (SGCallNode) n;
//                Collection<ESGEdge> esgEdges = esg.getEdges().get(n.getIndex());
//                for(ESGEdge esgEdge : esgEdges) {
//                    if(Objects.equals(n.getIndex(), esgEdge.getSgnSourceIdx())
//                            && Objects.equals(d1MethodIdentifier, esgEdge.getSourceDVarMethodName())
//                            && Objects.equals(d1.getIndex(), esgEdge.getSourceDVarIdx())) {
//                        int sIdx = esgEdge.getSgnTargetIdx();
//                        String d3MethodIdentifier = esgEdge.getTargetDVarMethodName();
//                        int d3Idx = esgEdge.getTargetDVarIdx();
//
//                        propagate(new ESGEdge(
//                                sIdx,
//                                sIdx,
//                                d3MethodIdentifier,
//                                d3MethodIdentifier,
//                                d3Idx,
//                                d3Idx)
//                        );
//                    }
//                }
//
//            }
//            else if (n instanceof SGExitNode && !Objects.equals(n, sg.getExitNode())) {
//
//            } else {
//                Collection<ESGEdge> esgEdges = esg.getEdges().get(n.getIndex());
//                for(ESGEdge esgEdge : esgEdges) {
//                    if(Objects.equals(n.getIndex(), esgEdge.getSgnSourceIdx())
//                            && Objects.equals(d2MethodIdentifier, esgEdge.getSourceDVarMethodName())
//                            && Objects.equals(d2.getIndex(), esgEdge.getSourceDVarIdx())) {
//                        int mIdx = esgEdge.getSgnTargetIdx();
//                        String d3MethodIdentifier = esgEdge.getTargetDVarMethodName();
//                        int d3Idx = esgEdge.getTargetDVarIdx();
//                        propagate(new ESGEdge(
//                                s.getIndex(),
//                                mIdx,
//                                d1MethodIdentifier,
//                                d3MethodIdentifier,
//                                d1.getIndex(),
//                                d3Idx)
//                        );
//                    }
//                }
//            }
//        }

        //--- When finished --------------------------------------------------------------------------------------------
    }

    private void propagate(ESGEdge e) {
        this.pathEdgeSet.add(e);
        this.workList.add(e);
    }

}
