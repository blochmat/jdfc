package algos;

import com.google.common.collect.Sets;
import data.ProgramVariable;
import graphs.esg.ESG;
import graphs.esg.ESGEdge;
import graphs.esg.nodes.ESGNode;
import graphs.sg.SG;
import graphs.sg.nodes.*;
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

    public Map<Integer, Set<UUID>> execute() {
        SG sg = this.esg.getSg();
        ProgramVariable ZERO = new ProgramVariable.ZeroVariable(sg.getClassName().substring(1).replace(".class", ""), sg.getMethodName());

        ESGEdge initialEdge = new ESGEdge(0, 0, 0, 0, ZERO.getId(), ZERO.getId());
        this.pathEdgeSet.add(initialEdge); // [2]
        this.workList.add(initialEdge); // [3]

        // [10]
        //--- ForwardTabulateSLRPs -------------------------------------------------------------------------------------
        while(!workList.isEmpty()) {
            ESGEdge currPathEdge = workList.pop(); // [11]

            // Path Edge Source
            int zeroIdx = currPathEdge.getSrcIdx();
            int zeroCallIdx = currPathEdge.getSrcCallSeqIdx();
            UUID zeroVarId = currPathEdge.getSrcVarId();

            // Path Edge Target
            SGNode n = sg.getNodes().get(currPathEdge.getTrgtIdx());
            int peTrgtIdx = currPathEdge.getTrgtIdx();

            if (n != null) {
                Collection<ESGEdge> esgEdges = esg.getEdges().get(peTrgtIdx);
                for (ESGEdge e : esgEdges) {
                    int trgtIdx = e.getTrgtIdx();
                    int trgtCallIdx = e.getTrgtCallSeqIdx();
                    UUID trgtVarId = e.getTrgtVarId();
                    ESGEdge newEdge = new ESGEdge(zeroIdx, trgtIdx, zeroCallIdx, trgtCallIdx, zeroVarId, trgtVarId);
                    propagate(newEdge);

                    // Connect matches
                    int srcIdx = e.getSrcIdx();
                    ESGNode srcNode = esg.getNodes().get(srcIdx);
                    connectMatches(srcNode, zeroIdx, trgtIdx, zeroCallIdx, zeroVarId, trgtVarId, new ArrayList<>());
                }
            }
        }

        // [7]
        //--- CREATE MVP -----------------------------------------------------------------------------------------------
        Map<Integer, Set<UUID>> bigXSet = new TreeMap<>();

        for (Map.Entry<Integer, ESGNode> esgNodeEntry : esg.getNodes().entrySet()) {
            int esgIdx = esgNodeEntry.getKey();
            ESGNode esgNode = esgNodeEntry.getValue();
            for (Map.Entry<Integer, Map<UUID, ProgramVariable>> callIdxEntry : esgNode.getCallIdxVarMaps().entrySet()) {
                int callIdx = callIdxEntry.getKey();
                Map<UUID, ProgramVariable> varMap = callIdxEntry.getValue();
                for (Map.Entry<UUID, ProgramVariable> varEntry : varMap.entrySet()) {
                    UUID varId = varEntry.getKey();
                    SGNode sgNode = sg.getNodes().get(esgIdx);
                    if (sgNode != null) {
                        ESGEdge e = new ESGEdge(0, esgIdx, 0, callIdx, ZERO.getId(), varId);
                        if (this.pathEdgeSet.contains(e)) {
                            if (!bigXSet.containsKey(esgIdx)) {
                                bigXSet.put(esgIdx, new HashSet<>());
                            }

                            bigXSet.get(esgIdx).add(varId);
                        }
                    }
                }
            }
        }

        return bigXSet;
    }

    /**
     * Recursive method to connect all inter-procedural matches of a variable
     *
     * @param srcNode ESGNode containing the definition matches
     * @param zeroIdx 0
     * @param trgtIdx Index of the target ESGNode
     * @param zeroCallIdx 0
     * @param zeroVarId basically 0
     * @param trgtVarId Variable id we are searching a match for
     */
    private void connectMatches(ESGNode srcNode, int zeroIdx, int trgtIdx, int zeroCallIdx, UUID zeroVarId, UUID trgtVarId, List<UUID> matched) {
        for (Map.Entry<Integer, Map<UUID, UUID>> callIdxEntry : srcNode.getDefinitionMaps().entrySet()) {
            Integer callIdx = callIdxEntry.getKey();
            Map<UUID, UUID> matches = callIdxEntry.getValue();
            UUID matchId = matches.get(trgtVarId);

            if (matchId != null && !matched.contains(matchId)) {
                ESGEdge matchEdge = new ESGEdge(zeroIdx, trgtIdx, zeroCallIdx, callIdx, zeroVarId, matchId);
                propagate(matchEdge);
                matched.add(matchId);

                connectMatches(srcNode, zeroIdx, trgtIdx, zeroCallIdx, zeroVarId, matchId, matched);
            }
        }
    }

    private void propagate(ESGEdge e) {
        if(!this.pathEdgeSet.contains(e)) {
            this.pathEdgeSet.add(e);
            this.workList.add(e);
        }
    }
}
