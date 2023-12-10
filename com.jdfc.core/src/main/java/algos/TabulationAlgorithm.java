package algos;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import data.ProgramVariable;
import graphs.esg.ESG;
import graphs.esg.ESGEdge;
import graphs.sg.SG;
import graphs.sg.nodes.SGCallNode;
import graphs.sg.nodes.SGEntryNode;
import graphs.sg.nodes.SGExitNode;
import graphs.sg.nodes.SGNode;
import lombok.Data;

import java.util.*;

@Data
public class TabulationAlgorithm {

    private ESG esg;

    private Set<ESGEdge> pathEdgeSet;

    private Set<ESGEdge> summaryEdgeSet;

    private LinkedList<ESGEdge> workList;

    public TabulationAlgorithm(ESG esg) {
        this.esg = esg; // [1]
        this.pathEdgeSet = Sets.newLinkedHashSet();
        this.workList = new LinkedList<>();
        this.summaryEdgeSet = new HashSet<>(); // [4]
    }

    public Multimap<Integer, ProgramVariable> execute() {
        SG sg = this.esg.getSg();
        String mainMId = String.format("%s :: %s", sg.getClassName().substring(1).replace(".class", ""), sg.getMethodName());
        ProgramVariable ZERO = new ProgramVariable.ZeroVariable(sg.getClassName().substring(1).replace(".class", ""), sg.getMethodName());

        ESGEdge initialEdge = new ESGEdge(0, 0, 0, 0, ZERO.getId(), ZERO.getId());
        this.pathEdgeSet.add(initialEdge); // [2]
        this.workList.add(initialEdge); // [3]

        //--- ForwardTabulateSLRPs -------------------------------------------------------------------------------------
        while(!workList.isEmpty()) { // [10]
            ESGEdge currPathEdge = workList.pop(); // [11]

            // Path Edge Source
            SGNode peSrcNode = sg.getNodes().get(currPathEdge.getSrcIdx());
            int peSrcEsgIdx = currPathEdge.getSrcIdx();
            int peSrcCallIdx = currPathEdge.getSrcCallSeqIdx();
            UUID peSrcVarId = currPathEdge.getSrcVarId();

            // Path Edge Target
            SGNode peTrgtNode = sg.getNodes().get(currPathEdge.getTrgtIdx());
            int peTrgtEsgIdx = currPathEdge.getTrgtIdx();
            int peTrgtCallIdx = currPathEdge.getTrgtCallSeqIdx();
            UUID peTrgtVarId = currPathEdge.getTrgtVarId();

            // [13] - [20]
            if (peTrgtNode instanceof SGCallNode) {
                Collection<ESGEdge> esgEdges = esg.getEdges().get(peTrgtEsgIdx);
                for (ESGEdge esgEdge : esgEdges) {
                    int srcCallIdx = esgEdge.getSrcCallSeqIdx();
                    int trgtCallIdx = esgEdge.getTrgtCallSeqIdx();
                    if (srcCallIdx < trgtCallIdx) {
                        // Trgt is entry node
                        int trgtIdx = esgEdge.getTrgtIdx();
                        UUID trgtVarId = esgEdge.getTrgtVarId();
                        propagate(new ESGEdge(trgtIdx, trgtIdx, trgtCallIdx, trgtCallIdx, trgtVarId, trgtVarId));
                    }


                }

//                for (ESGEdge esgEdge : summaryEdgeSet) {
//                    if (Objects.equals(peTrgtNode.getIndex(), esgEdge.getSrcIdx())
//                            && Objects.equals(peSrcCallIdx, esgEdge.getSrcCallSeqIdx())
//                            && Objects.equals(peSrcVarId, esgEdge.getSrcVarId())) {
//                        int mIdx = esgEdge.getTrgtIdx();
//                        String d3MId = esgEdge.getTrgtCallSeqIdx();
//                        ProgramVariable d3 = esgEdge.getTrgtVarId();
//                        SGNode mSGNode = sg.getNodes().get(mIdx);
//                        if (mSGNode instanceof SGReturnSiteNode) {
//                            propagate(new ESGEdge(
//                                    peSrcNode.getIndex(),
//                                    mIdx,
//                                    peSrcCallIdx,
//                                    d3MId,
//                                    peSrcVarId,
//                                    d3
//                            ));
//                        }
//                    }
//                }
                // [21] - [32]
            } else if (peTrgtNode instanceof SGExitNode && !Objects.equals(peTrgtNode, sg.getExitNode())) {
//                Collection<Integer> callers = sg.getCallersMap().get(peTrgtNode.getMethodName());
//                for (Integer idx : callers) {
//                    SGCallNode c = (SGCallNode) sg.getNodes().get(idx);
//                    int cIdx = c.getIndex();
//                    Collection<ESGEdge> callEdges = esg.getEdges().get(c.getIndex());
//                    Collection<ESGEdge> exitEdges = esg.getEdges().get(peTrgtNode.getIndex());
//
//                    for (ESGEdge callEdge : callEdges) {
//                        if (Objects.equals(cIdx, callEdge.getSgnSourceIdx())
//                                && Objects.equals(peSrcEsgIdx, callEdge.getSgnTargetIdx())
//                                && Objects.equals(peSrcCallIdx, callEdge.getTargetMethodId())
//                                && Objects.equals(peSrcVarId, callEdge.getTargetVar())) {
//                            String d4MethodIdentifier = callEdge.getSourceMethodId();
//                            ProgramVariable d4 = callEdge.getSourceVar();
//
//                            for (ESGEdge exitEdge : exitEdges) {
//                                // todo: Nullpointer
//                                int rIdx = sg.getReturnSiteIndexMap().get(cIdx);
//                                if (Objects.equals(peTrgtEsgIdx, exitEdge.getSgnSourceIdx())
//                                        && Objects.equals(peTrgtCallIdx, exitEdge.getSourceMethodId())
//                                        && Objects.equals(peTrgtVarId, exitEdge.getSourceVar())
//                                        && Objects.equals(rIdx, exitEdge.getSgnTargetIdx())) {
//                                    String d5MethodIdentifier = exitEdge.getTargetMethodId();
//                                    ProgramVariable d5 = exitEdge.getTargetVar();
//                                    ESGEdge e = new ESGEdge(cIdx, rIdx, d4MethodIdentifier, d5MethodIdentifier, d4, d5);
//                                    if (!summaryEdgeSet.contains(e)) {
//                                        summaryEdgeSet.add(e);
//
//                                        ClassMetaData classMetaData = ProjectData.getInstance().getClassMetaDataMap().get(c.getClassName());
//                                        // todo: NullPointer
//                                        ClassData cData = ProjectData.getInstance().getClassDataMap().get(classMetaData.getClassDataId());
//                                        SGEntryNode cEntryNode = cData.getMethodByInternalName(c.getMethodName())
//                                                .getSg()
//                                                .getEntryNode();
//                                        int cEntryIdx = cEntryNode.getIndex();
//                                        Collection<ESGEdge> entryEdges = esg.getEdges().get(cEntryIdx);
//                                        for (ESGEdge entryEdge : entryEdges) {
//                                            int eIdx = entryEdge.getSgnSourceIdx();
//                                            String d3MethodIdentifier = entryEdge.getSourceMethodId();
//                                            ProgramVariable d3 = entryEdge.getSourceVar();
//
//                                            ESGEdge search = new ESGEdge(eIdx,
//                                                    cIdx,
//                                                    d3MethodIdentifier,
//                                                    d4MethodIdentifier,
//                                                    d3,
//                                                    d4);
//                                            if (pathEdgeSet.contains(search)) {
//                                                propagate(new ESGEdge(
//                                                        eIdx,
//                                                        rIdx,
//                                                        d3MethodIdentifier,
//                                                        d5MethodIdentifier,
//                                                        d3,
//                                                        d5
//                                                ));
//                                            }
//                                        }
//                                    }
//                                }
//                            }
//                        }
//                    }
//                }
                // [33] - [37]
                // if node is not call p or exit p
                //      propagate a path edge from entry p to target of edge
            } else {
//                Collection<ESGEdge> esgEdges = esg.getEdges().get(peTrgtNode.getIndex());
//                for (ESGEdge e : esgEdges) {
//                    SGNode sgTargetNode = sg.getNodes().get(peTrgtEsgIdx);
//
//                    // Path edge source
//                    int newPeSgnSourceIdx = sgTargetNode.getEntryNodeIdx();
//                    String newPeSourceMethodId = e.getTrgtCallSeqIdx();
//                    ProgramVariable newPeSourceVar = e.getTrgtVarId();
//
//                    // Path edge target
//                    int newPeSgnTargetIdx = e.getTrgtIdx();
//                    String newPeTargetMethodId = e.getTrgtCallSeqIdx();
//                    ProgramVariable newPeTargetVar = e.getTrgtVarId();
//
//                    propagate(new ESGEdge(
//                            newPeSgnSourceIdx,
//                            newPeSgnTargetIdx,
//                            newPeSourceMethodId,
//                            newPeTargetMethodId,
//                            newPeSourceVar,
//                            newPeTargetVar)
//                    );
//                }
            }
        }

        //--- CREATE MVP -----------------------------------------------------------------------------------------------
        Multimap<Integer, ProgramVariable> bigXSet = ArrayListMultimap.create();

//        for(Map.Entry<Integer, Map<String, Map<UUID, ESGNode>>> esgLineSectionEntry : esg.getNodes().entrySet()) {
//            SGNode sgNode = sg.getNodes().get(esgLineSectionEntry.getKey());
//            int sgIdx = sgNode.getIndex();
//            ClassMetaData classMetaData = ProjectData.getInstance().getClassMetaDataMap().get(sgNode.getClassName().replace("/", "."));
//            ClassData classData = ProjectData.getInstance().getClassDataMap().get(classMetaData.getClassDataId());
//            SGEntryNode sgEntryNode = classData.getMethodByInternalName(sgNode.getMethodName()).getSg().getEntryNode();
//            int sgEntryIdx = sgEntryNode.getIndex();
//
//            for(ESGEdge pathEdge : pathEdgeSet) {
//                if(Objects.equals(sgEntryIdx, pathEdge.getSrcIdx())
//                        && Objects.equals(sgIdx, pathEdge.getTrgtIdx())
//                        && !Objects.equals(ZERO, pathEdge.getTrgtVarId())) {
//                    ProgramVariable pVar = pathEdge.getTrgtVarId();
//                    bigXSet.put(sgIdx, pVar);
//                }
//            }
//        }

        return bigXSet;
    }

    private void propagate(ESGEdge e) {
        if(!this.pathEdgeSet.contains(e)) {
            this.pathEdgeSet.add(e);
            this.workList.add(e);
        }
    }

}
