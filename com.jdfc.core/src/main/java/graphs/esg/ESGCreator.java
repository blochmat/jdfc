package graphs.esg;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import data.ClassExecutionData;
import data.DomainVariable;
import data.MethodData;
import graphs.esg.nodes.ESGNode;
import graphs.sg.SG;
import graphs.sg.nodes.SGCallNode;
import graphs.sg.nodes.SGExitNode;
import graphs.sg.nodes.SGNode;
import lombok.extern.slf4j.Slf4j;
import utils.JDFCUtils;

import java.util.Collection;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;

@Slf4j
public class ESGCreator {


    public static void createESGsForClass(ClassExecutionData cData) {
        for(MethodData mData : cData.getMethods().values()) {
            mData.setEsg(ESGCreator.createESGForMethod(cData, mData));
//            mData.getSg().calculateReachingDefinitions();
//            mData.calculateInterDefUsePairs();
        }
    }

    public static ESG createESGForMethod(ClassExecutionData cData, MethodData mData) {
        SG sg = mData.getSg();

        //--- CREATE NODES ---------------------------------------------------------------------------------------------
        NavigableMap<Integer, NavigableMap<Integer, ESGNode>> esgNodes = Maps.newTreeMap();
        esgNodes.computeIfAbsent(Integer.MIN_VALUE, k -> Maps.newTreeMap());
        esgNodes.get(Integer.MIN_VALUE).put(-1, new ESGNode.ESGZeroNode(Integer.MIN_VALUE));

        for(SGNode sgNode : sg.getNodes().values()) {
            int sgNodeIdx = sgNode.getIndex();
            esgNodes.computeIfAbsent(sgNodeIdx, k -> Maps.newTreeMap());
            esgNodes.get(sgNodeIdx).put(-1, new ESGNode.ESGZeroNode(sgNodeIdx));
            Set<DomainVariable> domain = cData.getMethodByInternalName(sgNode.getMethodName()).getCfg().getDomain();

            for(DomainVariable dVar : domain) {
                esgNodes.get(sgNode.getIndex()).put(dVar.getIndex(), new ESGNode(sgNodeIdx, dVar));
            }
        }

        // --- DEBUG NODES ---------------------------------------------------------------------------------------------
        if(log.isDebugEnabled()) {
            StringBuilder sb = new StringBuilder();
            sb.append(cData.getRelativePath()).append(" ");
            sb.append(mData.buildInternalMethodName()).append("\n");

            for(SGNode sgNode : sg.getNodes().values()) {
                for(Map.Entry<Integer, ESGNode> entry : esgNodes.get(sgNode.getIndex()).entrySet()) {
                    sb.append("(")
                            .append(entry.getValue().getSgnIndex())
                            .append(",").append(entry.getValue().getDVar().getIndex())
                            .append(")")
                            .append(": ")
                            .append(entry.getValue().getDVar().getName()).append(", ");
                }
                sb.append("\n");
            }
            JDFCUtils.logThis(sb.toString(), "exploded_nodes");
        }


        //--- CREATE EDGES ---------------------------------------------------------------------------------------------
        Multimap<Integer, ESGEdge> esgEdges = ArrayListMultimap.create();

        // iterate over sg
        for(SGNode sgNode : sg.getNodes().values()) {
            Map<Integer, ESGNode> esgSourceNodeMap = esgNodes.get(sgNode.getIndex());
            Collection<Integer> sgnTargetIdxList = sg.getEdges().get(sgNode.getIndex());

            // iterate over domain vars
            for(Map.Entry<Integer, ESGNode> esgNodeEntry : esgNodes.get(sgNode.getIndex()).entrySet()) {
                DomainVariable domainVariable = esgNodeEntry.getValue().getDVar();
                if(sgNode.getIndex() == 0) {
                    // special case for very first node of the sg
                    esgEdges.put(Integer.MIN_VALUE, new ESGEdge(
                            Integer.MIN_VALUE,
                            0,
                            -1,
                            domainVariable.getIndex()));
                }

                if (sgNode instanceof SGCallNode) {
                    SGCallNode sgCallNode = (SGCallNode) sgNode;
                    // Iterate over all domain vars in source
                    for(Integer sourceDVarIdx : esgSourceNodeMap.keySet()) {
                        // for all target sg nodes
                        for(Integer sgnTargetIdx: sgnTargetIdxList) {
                            if(sourceDVarIdx == -1) {
                                // special case zeroVar
                                esgEdges.put(sgCallNode.getIndex(), new ESGEdge(
                                        sgCallNode.getIndex(),
                                        sgnTargetIdx,
                                        sourceDVarIdx,
                                        sourceDVarIdx));
                            } else {
                                Integer targetDVarIdx = sgCallNode.getDVarMap().get(sourceDVarIdx);
                                if(targetDVarIdx != null) {
                                    // green
                                    esgEdges.put(sgCallNode.getIndex(), new ESGEdge(
                                            sgCallNode.getIndex(),
                                            sgnTargetIdx,
                                            sourceDVarIdx,
                                            targetDVarIdx));
                                }
                            }
                        }
                    }
                } else if (sgNode instanceof SGExitNode) {
                    SGExitNode sgExitNode = (SGExitNode) sgNode;
                    // Iterate over all domain vars in source
                    for(Integer sourceDVarIdx : esgSourceNodeMap.keySet()) {
                        // for all target sg nodes
                        for(Integer sgnTargetIdx: sgnTargetIdxList) {
                            if(sourceDVarIdx == -1) {
                                // special case zeroVar
                                esgEdges.put(sgNode.getIndex(), new ESGEdge(
                                        sgNode.getIndex(),
                                        sgnTargetIdx,
                                        sourceDVarIdx,
                                        sourceDVarIdx));
                            } else {
                                Integer targetDVarIdx = sgExitNode.getDVarMap().get(sourceDVarIdx);
                                if(targetDVarIdx != null) {
                                    // green
                                    esgEdges.put(sgNode.getIndex(), new ESGEdge(
                                            sgNode.getIndex(),
                                            sgnTargetIdx,
                                            sourceDVarIdx,
                                            targetDVarIdx));
                                }
                            }
                        }
                    }
                } else {
                    // Iterate over all domain vars in source
                    for(Integer sourceDVarIdx : esgSourceNodeMap.keySet()) {
                        // for all target sg nodes
                        for(Integer sgnTargetIdx: sgnTargetIdxList) {
                            // green
                            esgEdges.put(sgNode.getIndex(), new ESGEdge(
                                    sgNode.getIndex(),
                                    sgnTargetIdx,
                                    sourceDVarIdx,
                                    sourceDVarIdx));
                        }
                    }
                }
            }
        }

        //--- DEGUG EDGES ----------------------------------------------------------------------------------------------
        if(log.isDebugEnabled()) {
            StringBuilder sb = new StringBuilder();
            sb.append(cData.getRelativePath()).append(" ");
            sb.append(mData.buildInternalMethodName()).append("\n");
            sb.append(JDFCUtils.prettyPrintMultimap(esgEdges));
            JDFCUtils.logThis(sb.toString(), "exploded_edges");
        }

        //--- CREATE ESG -----------------------------------------------------------------------------------------------
        return new ESG(esgNodes, esgEdges);
    }
}
