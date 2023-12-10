package algos;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import data.ProgramVariable;
import graphs.esg.ESG;
import graphs.esg.ESGEdge;
import graphs.esg.nodes.ESGNode;
import graphs.sg.SG;
import graphs.sg.nodes.SGCallNode;
import graphs.sg.nodes.SGEntryNode;
import graphs.sg.nodes.SGExitNode;
import graphs.sg.nodes.SGNode;
import lombok.Data;
import org.checkerframework.checker.units.qual.A;

import java.util.*;
import java.util.stream.Collectors;

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

    public Map<Integer, Set<UUID>> execute() {
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
            int peSrcIdx = currPathEdge.getSrcIdx();
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
                SGCallNode sgCallNode = (SGCallNode) peTrgtNode;
                for (ESGEdge esgEdge : esgEdges) {
                    int trgtIdx = esgEdge.getTrgtIdx();
                    int trgtCallIdx = esgEdge.getTrgtCallSeqIdx();
                    UUID trgtVarId = esgEdge.getTrgtVarId();
                    // [14] - [16]
                    if (esgEdge.getSrcCallSeqIdx() < trgtCallIdx) {
                        // Trgt is entry node
                        propagate(new ESGEdge(trgtIdx, trgtIdx, trgtCallIdx, trgtCallIdx, trgtVarId, trgtVarId));
                    }

                    // [17] - [19]: E#
                    if (trgtIdx == sgCallNode.getReturnSiteNodeIdx()) {
                        int entryNodeIdx = peTrgtNode.getEntryNodeIdx();
                        propagate(new ESGEdge(entryNodeIdx, trgtIdx, peSrcCallIdx, trgtCallIdx, peSrcVarId, trgtVarId));
                    }
                }

                // [17] - [19]: SummaryEdge
                for (ESGEdge esgEdge : summaryEdgeSet) {
                    int trgtIdx = esgEdge.getTrgtIdx();
                    int trgtCallIdx = esgEdge.getTrgtCallSeqIdx();
                    UUID trgtVarId = esgEdge.getTrgtVarId();
                    if (trgtIdx == sgCallNode.getReturnSiteNodeIdx()) {
                        int entryNodeIdx = peTrgtNode.getEntryNodeIdx();
                        propagate(new ESGEdge(entryNodeIdx, trgtIdx, peSrcCallIdx, trgtCallIdx, peSrcVarId, trgtVarId));
                    }
                }
            // [21]
            } else if (peTrgtNode instanceof SGExitNode && !Objects.equals(peTrgtNode, sg.getExitNode())) {
                Collection<Integer> callers = sg.getCallersMap().get(peTrgtNode.getMethodName());

                // [22]
                // for all call nodes calling the exit node's procedure
                for (Integer c : callers) {
                    Set<ESGEdge> esgCallEdges = esg.getEdges().get(c)
                            .stream()
                            .filter(e -> e.getSrcCallSeqIdx() < e.getTrgtCallSeqIdx())
                            .collect(Collectors.toSet());
                    Set<ESGEdge> esgExitEdges = esg.getEdges().get(peTrgtNode.getIndex())
                            .stream()
                            .filter(e -> e.getSrcCallSeqIdx() > e.getTrgtCallSeqIdx())
                            .collect(Collectors.toSet());

                    // [23]
                    for (ESGEdge callEdge : esgCallEdges) {
                        int d4Idx = callEdge.getSrcIdx();
                        int d4CallIdx = callEdge.getSrcCallSeqIdx();
                        UUID d4VarId = callEdge.getSrcVarId();

                        for (ESGEdge exitEdge : esgExitEdges) {
                            int d5Idx = exitEdge.getTrgtIdx();
                            int d5CallIdx = exitEdge.getTrgtCallSeqIdx();
                            UUID d5VarId = exitEdge.getTrgtVarId();

                            ESGEdge summaryEdge = new ESGEdge(
                                    callEdge.getSrcIdx(), exitEdge.getTrgtIdx(),
                                    callEdge.getSrcCallSeqIdx(), exitEdge.getTrgtCallSeqIdx(),
                                    callEdge.getSrcVarId(), exitEdge.getTrgtVarId());

                            if (d4CallIdx == d5CallIdx
                                    && Objects.equals(d4VarId, d5VarId)) {
                                // [24], [25]
                                this.summaryEdgeSet.add(summaryEdge);

                                // [26]
                                List<ESGEdge> toAdd = new ArrayList<>();
                                for (ESGEdge pe : this.pathEdgeSet) {
                                    int d3Idx = pe.getSrcIdx();
                                    int d3CallIdx = pe.getSrcCallSeqIdx();
                                    UUID d3VarId = pe.getSrcVarId();

                                    if (d4Idx == pe.getTrgtIdx()
                                        && d4CallIdx == pe.getTrgtCallSeqIdx()
                                        && d4VarId == pe.getTrgtVarId()) {
                                        // [27]
                                        toAdd.add(new ESGEdge(d3Idx, d5Idx, d3CallIdx, d5CallIdx, d3VarId, d5VarId));
                                    }
                                }

                                for (ESGEdge e : toAdd) {
                                    propagate(e);
                                }
                            }
                        }
                    }
                }
            // [33]
            // if node is not call p or exit p
            } else {
                // [34]
                if (peTrgtNode != null) {
                    Collection<ESGEdge> esgEdges = esg.getEdges().get(peTrgtNode.getIndex());
                    for (ESGEdge e : esgEdges) {
                        int d3Idx = e.getTrgtIdx();
                        int d3CallIdx = e.getTrgtCallSeqIdx();
                        UUID d3VarId = e.getTrgtVarId();
                        // [35]
                        propagate(new ESGEdge(peSrcIdx, d3Idx, peSrcCallIdx, d3CallIdx, peSrcVarId, d3VarId));
                    }
                }
            }
        }

        // [7]
        //--- CREATE MVP -----------------------------------------------------------------------------------------------
        Map<Integer, Set<UUID>> bigXSet = new HashMap<>();

        for (Map.Entry<Integer, ESGNode> esgNodeEntry : esg.getNodes().entrySet()) {
            int esgIdx = esgNodeEntry.getKey();
            ESGNode esgNode = esgNodeEntry.getValue();
            Map<Integer, Map<UUID, ProgramVariable>> esgVarsMap = esgNode.getCallSeqIdxVarMap();

            for (Map.Entry<Integer, Map<UUID, ProgramVariable>> callIdxEntry : esgVarsMap.entrySet()) {
                int d2CallIdx = callIdxEntry.getKey();
                Map<UUID, ProgramVariable> varMap = callIdxEntry.getValue();
                for (Map.Entry<UUID, ProgramVariable> varEntry : varMap.entrySet()) {
                    UUID d2VarId = varEntry.getKey();
                    ProgramVariable d2Var = varEntry.getValue();

                    SGNode sgNode = sg.getNodes().get(esgIdx);

                    if (sgNode != null) {
                        for (ESGEdge pe : this.pathEdgeSet) {
                            if (pe.getSrcIdx() == sgNode.getEntryNodeIdx()
                                    && pe.getTrgtIdx() == esgIdx
                                    && pe.getTrgtCallSeqIdx() == d2CallIdx
                                    && pe.getTrgtVarId() == d2VarId) {

                                if (!bigXSet.containsKey(esgIdx)) {
                                    bigXSet.put(esgIdx, new HashSet<>());
                                }

                                bigXSet.get(esgIdx).add(d2VarId);
                            }
                        }
                    }
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
