package algos;

import graphs.esg.ESG;
import graphs.esg.ESGEdge;

import java.util.LinkedList;
import java.util.Set;

public class TabulationAlgorithm {

    private ESG graph;

    private Set<ESGEdge> pathEdgeSet;

    private Set<ESGEdge> summaryEdgeSet;

    private LinkedList<ESGEdge> workList;

    public TabulationAlgorithm(ESG graph) {
//        this.graph = graph;
//        this.pathEdgeSet = new HashSet<>();
//        this.summaryEdgeSet = new HashSet<>();
//        this.workList = new LinkedList<>();
//
//        ESGNode mainNode = new ESGNode(graph.getSg().getEntryNode(), new ZeroVariable(), true);
//        pathEdgeSet.add(new ESGEdge(0, mainNode, 0, mainNode));
//        this.workList.add(new ESGEdge(0, mainNode, 0, mainNode));
    }

    public void execute() {
//        while(!this.workList.isEmpty()) {
//            ESGEdge e = workList.pop();
//
//            if (e.getTarget() instanceof ESGCallNode) {
//                // entryNodes = find entry nodes by names from calledProc(e.getTarget)
//                // for (entryNode : entryNodes) {
//                //     if (<e.getTarget(), var> -> <entry(calledProc(n)), var> exists) {
//                //         propagate(new Edge(<entry(calledProc(n)), var>, <entry(calledProc(n)), var>)
//                //     }
//                // }
//                // returnSites = find corresponding return site nodes by return(e.getTarget) mapping
//                // for (returnSiteNode : returnSites) {
//                //     propagate (new Edge(e.getSource(), returnSiteNode)
//                // }
//            } else if (e.getTarget() instanceof ESGExitNode) {
//                // for(CallNode c : callers(p)) { // all call nodes that call procedure p
//                //     get possiblyNotRedefinedVarsDuringProcedure
//                //     for(var : possiblyNotRedefinedVarsDuringProcedure)
//                //         if (<c, var> -> <return(c), var> not in summaryEdgeSet) {
//                //             insert summary edge
//                //             for(var : <entry(procOf(c)), var> -> <c, var> in PathEdge {
//                //                 propagate(new Edge(<entry(procOf(c)), var>, <returnSite(c), var>))
//                //             }
//                //         }
//                //     }
//            } else {
//                // for (node : succ(e.getTarget()) {
//                //     if (edge target -> succ exists) {
//                //         propagate(new Edge(e.getSource(), node)
//                //     }
//                // }
//            }
//        }
    }

}
