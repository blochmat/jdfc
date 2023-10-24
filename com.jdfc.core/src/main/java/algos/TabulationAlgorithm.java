package algos;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import data.ClassData;
import data.ProgramVariable;
import data.ProjectData;
import graphs.esg.ESG;
import graphs.esg.ESGEdge;
import graphs.esg.nodes.ESGNode;
import graphs.sg.SG;
import graphs.sg.nodes.*;
import instr.ClassMetaData;
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

        ESGEdge initialEdge = new ESGEdge(
                0,
                0,
                mainMId,
                mainMId,
                ZERO,
                ZERO
        );
        this.pathEdgeSet.add(initialEdge); // [2]
        this.workList.add(initialEdge); // [3]

        //--- ForwardTabulateSLRPs -------------------------------------------------------------------------------------
        while(!workList.isEmpty()) { // [10]
            ESGEdge currPathEdge = workList.pop(); // [11]

            // Path Edge Source
            SGNode peSrcNode = sg.getNodes().get(currPathEdge.getSgnSourceIdx());
            int peSrcNodeIdx = currPathEdge.getSgnSourceIdx();
            String peSrcMethodId = currPathEdge.getSourceMethodId();
            ProgramVariable peSrcVar = currPathEdge.getSourceVar();

            // Path Edge Target
            SGNode peTargetNode = sg.getNodes().get(currPathEdge.getSgnTargetIdx());
            int peTargetNodeIdx = currPathEdge.getSgnTargetIdx();
            String peTargetMethodId = currPathEdge.getTargetMethodId();
            ProgramVariable peTargetVar = currPathEdge.getTargetVar();

            // [13] - [20]
            if (peTargetNode instanceof SGCallNode) {
                Collection<ESGEdge> esgEdges = esg.getEdges().get(peTargetNodeIdx);
                for (ESGEdge esgEdge : esgEdges) {
                    boolean basicEdgeExits = Objects.equals(peSrcMethodId, esgEdge.getSourceMethodId())
                            && Objects.equals(peSrcVar, esgEdge.getSourceVar());
                    boolean matchEdgeExists = false;
                    if (esg.getCallerToCalleeDefinitionMap().containsKey(esgEdge.getSgnSourceIdx())) {
                        matchEdgeExists = esg.getCallerToCalleeDefinitionMap().get(esgEdge.getSgnSourceIdx()).containsKey(peTargetVar);
                    }
                    if (basicEdgeExits || matchEdgeExists) {
                        int mIdx = esgEdge.getSgnTargetIdx();
                        String d3MId = esgEdge.getTargetMethodId();
                        ProgramVariable d3 = esgEdge.getTargetVar();
                        SGNode mSGNode = sg.getNodes().get(mIdx);

                        if (mSGNode instanceof SGEntryNode) {
                            propagate(new ESGEdge(
                                    peTargetNodeIdx,
                                    mIdx,
                                    peTargetMethodId,
                                    d3MId,
                                    peTargetVar,
                                    d3)
                            );
                        } else {
                            propagate(new ESGEdge(
                                    peSrcNodeIdx,
                                    mIdx,
                                    peSrcMethodId,
                                    d3MId,
                                    peSrcVar,
                                    d3
                            ));
                        }
                    }
                }

                for (ESGEdge esgEdge : summaryEdgeSet) {
                    if (Objects.equals(peTargetNode.getIndex(), esgEdge.getSgnSourceIdx())
                            && Objects.equals(peSrcMethodId, esgEdge.getSourceMethodId())
                            && Objects.equals(peSrcVar, esgEdge.getSourceVar())) {
                        int mIdx = esgEdge.getSgnTargetIdx();
                        String d3MId = esgEdge.getTargetMethodId();
                        ProgramVariable d3 = esgEdge.getTargetVar();
                        SGNode mSGNode = sg.getNodes().get(mIdx);
                        if (mSGNode instanceof SGReturnSiteNode) {
                            propagate(new ESGEdge(
                                    peSrcNode.getIndex(),
                                    mIdx,
                                    peSrcMethodId,
                                    d3MId,
                                    peSrcVar,
                                    d3
                            ));
                        }
                    }
                }
            // [21] - [32]
//            } else if (peTargetNode instanceof SGExitNode && !Objects.equals(peTargetNode, sg.getExitNode())) {
//                Collection<Integer> callers = sg.getCallersMap().get(peTargetNode.getMethodName());
//                for (Integer idx : callers) {
//                    SGCallNode c = (SGCallNode) sg.getNodes().get(idx);
//                    int cIdx = c.getIndex();
//                    Collection<ESGEdge> callEdges = esg.getEdges().get(c.getIndex());
//                    Collection<ESGEdge> exitEdges = esg.getEdges().get(peTargetNode.getIndex());
//
//                    for (ESGEdge callEdge : callEdges) {
//                        if (Objects.equals(cIdx, callEdge.getSgnSourceIdx())
//                                && Objects.equals(peSrcNodeIdx, callEdge.getSgnTargetIdx())
//                                && Objects.equals(peSrcMethodId, callEdge.getTargetMethodId())
//                                && Objects.equals(peSrcVar, callEdge.getTargetVar())) {
//                            String d4MethodIdentifier = callEdge.getSourceMethodId();
//                            ProgramVariable d4 = callEdge.getSourceVar();
//
//                            for (ESGEdge exitEdge : exitEdges) {
//                                // todo: Nullpointer
//                                int rIdx = sg.getReturnSiteIndexMap().get(cIdx);
//                                if (Objects.equals(peTargetNodeIdx, exitEdge.getSgnSourceIdx())
//                                        && Objects.equals(peTargetMethodId, exitEdge.getSourceMethodId())
//                                        && Objects.equals(peTargetVar, exitEdge.getSourceVar())
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
                Collection<ESGEdge> esgEdges = esg.getEdges().get(peTargetNode.getIndex());
                for (ESGEdge e : esgEdges) {
                    SGNode sgTargetNode = sg.getNodes().get(peTargetNodeIdx);

                    // Path edge source
                    int newPeSgnSourceIdx = sgTargetNode.getEntryNodeIdx();
                    String newPeSourceMethodId = e.getTargetMethodId();
                    ProgramVariable newPeSourceVar = e.getTargetVar();

                    // Path edge target
                    int newPeSgnTargetIdx = e.getSgnTargetIdx();
                    String newPeTargetMethodId = e.getTargetMethodId();
                    ProgramVariable newPeTargetVar = e.getTargetVar();

                    propagate(new ESGEdge(
                            newPeSgnSourceIdx,
                            newPeSgnTargetIdx,
                            newPeSourceMethodId,
                            newPeTargetMethodId,
                            newPeSourceVar,
                            newPeTargetVar)
                    );
                }
//            }
            }
        }

        //--- CREATE MVP -----------------------------------------------------------------------------------------------
        Multimap<Integer, ProgramVariable> bigXSet = ArrayListMultimap.create();

        for(Map.Entry<Integer, Map<String, Map<UUID, ESGNode>>> esgLineSectionEntry : esg.getNodes().entrySet()) {
            SGNode sgNode = sg.getNodes().get(esgLineSectionEntry.getKey());
            int sgIdx = sgNode.getIndex();
            ClassMetaData classMetaData = ProjectData.getInstance().getClassMetaDataMap().get(sgNode.getClassName().replace("/", "."));
            ClassData classData = ProjectData.getInstance().getClassDataMap().get(classMetaData.getClassDataId());
            SGEntryNode sgEntryNode = classData.getMethodByInternalName(sgNode.getMethodName()).getSg().getEntryNode();
            int sgEntryIdx = sgEntryNode.getIndex();

            for(ESGEdge pathEdge : pathEdgeSet) {
                if(Objects.equals(sgEntryIdx, pathEdge.getSgnSourceIdx())
                        && Objects.equals(sgIdx, pathEdge.getSgnTargetIdx())
                        && !Objects.equals(ZERO, pathEdge.getTargetVar())) {
                    ProgramVariable pVar = pathEdge.getTargetVar();
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
