package algos;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import data.ClassExecutionData;
import data.ProgramVariable;
import data.singleton.CoverageDataStore;
import graphs.esg.ESG;
import graphs.esg.ESGEdge;
import graphs.esg.nodes.ESGNode;
import graphs.sg.SG;
import graphs.sg.nodes.*;
import lombok.Data;
import utils.JDFCUtils;

import java.util.*;

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

    public Multimap<Integer, ProgramVariable> execute() {
        SG sg = this.esg.getSg();
        String mainMId = String.format("%s :: %s", sg.getClassName(), sg.getMethodName());
        ProgramVariable ZERO = new ProgramVariable.ZeroVariable(sg.getClassName(), sg.getMethodName());

        //--- ForwardTabulateSLRPs -------------------------------------------------------------------------------------
        ESGEdge initialEdge = new ESGEdge(
                0,
                0,
                mainMId,
                mainMId,
                ZERO,
                ZERO
        );
        this.pathEdgeSet.add(initialEdge);
        this.workList.add(initialEdge);

        int iteration = 0;

        while(!workList.isEmpty()) {
            if(mainMId.contains("callFoo")) {
                JDFCUtils.logThis(JDFCUtils.prettyPrintArray(workList.toArray(new ESGEdge[0])), "worklist_" + iteration);
            }
            iteration++;
            ESGEdge currPathEdge = workList.pop();

            // Source
            SGNode s = sg.getNodes().get(currPathEdge.getSgnSourceIdx());
            int sIdx = currPathEdge.getSgnSourceIdx();
            String d1MId = currPathEdge.getSourceDVarMethodName();
            ProgramVariable d1 = currPathEdge.getSourcePVar();

            // Target
            SGNode n = sg.getNodes().get(currPathEdge.getSgnTargetIdx());
            int nIdx = currPathEdge.getSgnTargetIdx();
            String d2MId = currPathEdge.getTargetDVarMethodName();
            ProgramVariable d2 = currPathEdge.getTargetPVar();

            if(n instanceof SGCallNode) {
                Collection<ESGEdge> esgEdges = esg.getEdges().get(nIdx);
                for(ESGEdge esgEdge : esgEdges) {
                    if(Objects.equals(nIdx, esgEdge.getSgnSourceIdx())
                            && Objects.equals(d1MId, esgEdge.getSourceDVarMethodName())
                            && Objects.equals(d1, esgEdge.getSourcePVar())) {
                        int mIdx = esgEdge.getSgnTargetIdx();
                        String d3MId = esgEdge.getTargetDVarMethodName();
                        ProgramVariable d3 = esgEdge.getTargetPVar();
                        SGNode mSGNode = sg.getNodes().get(mIdx);

                        if(mSGNode instanceof SGEntryNode) {
                            propagate(new ESGEdge(
                                    mIdx,
                                    mIdx,
                                    d3MId,
                                    d3MId,
                                    d3,
                                    d3)
                            );
                        } else {
                            propagate(new ESGEdge(
                                    sIdx,
                                    mIdx,
                                    d1MId,
                                    d3MId,
                                    d1,
                                    d3
                            ));
                        }
                    }
                }

                for(ESGEdge esgEdge : summaryEdgeSet) {
                    if(Objects.equals(n.getIndex(), esgEdge.getSgnSourceIdx())
                            && Objects.equals(d1MId, esgEdge.getSourceDVarMethodName())
                            && Objects.equals(d1, esgEdge.getSourcePVar())) {
                        int mIdx = esgEdge.getSgnTargetIdx();
                        String d3MId = esgEdge.getTargetDVarMethodName();
                        ProgramVariable d3 = esgEdge.getTargetPVar();
                        SGNode mSGNode = sg.getNodes().get(mIdx);
                        if(mSGNode instanceof SGReturnSiteNode) {
                            propagate(new ESGEdge(
                                    s.getIndex(),
                                    mIdx,
                                    d1MId,
                                    d3MId,
                                    d1,
                                    d3
                            ));
                        }
                    }
                }
            }
            else if (n instanceof SGExitNode && !Objects.equals(n, sg.getExitNode())) {
                Collection<SGCallNode> callers = sg.getCallersMap().get(n.getMethodName());
                for(SGCallNode c : callers) {
                    int cIdx = c.getIndex();
                    Collection<ESGEdge> callEdges = esg.getEdges().get(c.getIndex());
                    Collection<ESGEdge> exitEdges = esg.getEdges().get(n.getIndex());

                    for(ESGEdge callEdge : callEdges) {
                        if(Objects.equals(cIdx, callEdge.getSgnSourceIdx())
                                && Objects.equals(sIdx, callEdge.getSgnTargetIdx())
                                && Objects.equals(d1MId, callEdge.getTargetDVarMethodName())
                                && Objects.equals(d1, callEdge.getTargetPVar())) {
                            String d4MethodIdentifier = callEdge.getSourceDVarMethodName();
                            ProgramVariable d4 = callEdge.getSourcePVar();

                            for (ESGEdge exitEdge : exitEdges) {
                                int rIdx = sg.getReturnSiteIndexMap().get(cIdx);
                                if(Objects.equals(nIdx, exitEdge.getSgnSourceIdx())
                                        && Objects.equals(d2MId, exitEdge.getSourceDVarMethodName())
                                        && Objects.equals(d2, exitEdge.getSourcePVar())
                                        && Objects.equals(rIdx, exitEdge.getSgnTargetIdx())) {
                                    String d5MethodIdentifier = exitEdge.getTargetDVarMethodName();
                                    ProgramVariable d5 = exitEdge.getTargetPVar();
                                    ESGEdge e = new ESGEdge(cIdx, rIdx, d4MethodIdentifier, d5MethodIdentifier, d4, d5);
                                    if(!summaryEdgeSet.contains(e)){
                                        summaryEdgeSet.add(e);

                                        ClassExecutionData cData = (ClassExecutionData) CoverageDataStore
                                                .getInstance().findClassDataNode(c.getClassName()).getData();
                                        SGEntryNode cEntryNode = cData.getMethodByInternalName(c.getMethodName())
                                                .getSg()
                                                .getEntryNode();
                                        int cEntryIdx = cEntryNode.getIndex();
                                        Collection<ESGEdge> entryEdges = esg.getEdges().get(cEntryIdx);
                                        for(ESGEdge entryEdge: entryEdges) {
                                            int eIdx = entryEdge.getSgnSourceIdx();
                                            String d3MethodIdentifier = entryEdge.getSourceDVarMethodName();
                                            ProgramVariable d3 = entryEdge.getSourcePVar();

                                            ESGEdge search = new ESGEdge(eIdx,
                                                    cIdx,
                                                    d3MethodIdentifier,
                                                    d4MethodIdentifier,
                                                    d3,
                                                    d4);
                                            if(pathEdgeSet.contains(search)) {
                                                propagate(new ESGEdge(
                                                        eIdx,
                                                        rIdx,
                                                        d3MethodIdentifier,
                                                        d5MethodIdentifier,
                                                        d3,
                                                        d5
                                                ));
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            else {
                Collection<ESGEdge> esgEdges = esg.getEdges().get(n.getIndex());
                for(ESGEdge esgEdge : esgEdges) {
                    if(Objects.equals(nIdx, esgEdge.getSgnSourceIdx())
                            && Objects.equals(d2MId, esgEdge.getSourceDVarMethodName())
                            && Objects.equals(d2, esgEdge.getSourcePVar())) {
                        int mIdx = esgEdge.getSgnTargetIdx();
                        String d3MId = esgEdge.getTargetDVarMethodName();
                        ProgramVariable d3 = esgEdge.getTargetPVar();

                        propagate(new ESGEdge(
                                sIdx,
                                mIdx,
                                d1MId,
                                d3MId,
                                d1,
                                d3)
                        );
                    }
                }
            }
        }

        //--- CREATE MVP -----------------------------------------------------------------------------------------------
        Multimap<Integer, ProgramVariable> bigXSet = ArrayListMultimap.create();

        for(Map.Entry<Integer, Map<String, Map<UUID, ESGNode>>> esgLineSectionEntry : esg.getNodes().entrySet()) {
            SGNode sgNode = sg.getNodes().get(esgLineSectionEntry.getKey());
            int sgIdx = sgNode.getIndex();
            ClassExecutionData cData = (ClassExecutionData) CoverageDataStore
                    .getInstance()
                    .findClassDataNode(sgNode.getClassName())
                    .getData();
            SGEntryNode sgEntryNode = cData.getMethodByInternalName(sgNode.getMethodName()).getSg().getEntryNode();
            int sgEntryIdx = sgEntryNode.getIndex();

            for(ESGEdge pathEdge : pathEdgeSet) {
                if(Objects.equals(sgEntryIdx, pathEdge.getSgnSourceIdx())
                        && Objects.equals(sgIdx, pathEdge.getSgnTargetIdx())
                        && !Objects.equals(ZERO, pathEdge.getTargetPVar())) {
                    ProgramVariable pVar = pathEdge.getTargetPVar();
                    bigXSet.put(sgIdx, pVar);
                }
            }
        }

        return bigXSet;
    }

    private void propagate(ESGEdge e) {
        if(!this.pathEdgeSet.contains(e)) {
            this.pathEdgeSet.add(e);
            this.workList.add(e);
        }
    }

}
