package graphs.esg;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.BiMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import data.ClassExecutionData;
import data.DomainVariable;
import data.MethodData;
import graphs.esg.nodes.ESGNode;
import graphs.sg.SG;
import graphs.sg.nodes.SGCallNode;
import graphs.sg.nodes.SGNode;
import lombok.extern.slf4j.Slf4j;
import utils.JDFCUtils;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

@Slf4j
public class ESGCreator {


    public static void createESGsForClass(ClassExecutionData cData) {
        for(MethodData mData : cData.getMethods().values()) {
            mData.setEsg(ESGCreator.createESGForMethod(cData, mData));
            mData.getSg().calculateReachingDefinitions();
            mData.calculateInterDefUsePairs();
        }
    }

    public static ESG createESGForMethod(ClassExecutionData cData, MethodData mData) {
        SG sg = mData.getSg();

        Map<Integer, Map<Integer, ESGNode>> esgNodes = Maps.newTreeMap();
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

        Map<Integer, BiMap<Integer, Integer>> domainVarMap = mData.getSg().getDomainVarMap();

        Multimap<Integer, ESGEdge> esgEdges = ArrayListMultimap.create();
        // iterate over sg
        for(SGNode sgNode : sg.getNodes().values()) {
            Collection<Integer> sgnTargetIdxList = sg.getEdges().get(sgNode.getIndex());

            // iterate over domain vars
            for(Map.Entry<Integer, ESGNode> esgNodeEntry : esgNodes.get(sgNode.getIndex()).entrySet()) {
                DomainVariable domainVariable = esgNodeEntry.getValue().getDVar();

                if(sgNode.getIndex() == 0) {
                    // special case for very first node of the sg
                    esgEdges.put(Integer.MIN_VALUE, new ESGEdge(
                            Integer.MIN_VALUE,
                            -1,
                            0,
                            domainVariable.getIndex()));
                }

                // red
                Map<Integer, ESGNode> esgSourceNodeMap = esgNodes.get(sgNode.getIndex());

                if (sgNode instanceof SGCallNode) {
                    // Iterate over all domain vars in source
                    for(Integer sourceDVarIdx : esgSourceNodeMap.keySet()) {
                        // for all target sg nodes
                        for(Integer sgnTargetIdx: sgnTargetIdxList) {
                            if(sourceDVarIdx == -1) {
                                // special case zeroVar
                                esgEdges.put(sgNode.getIndex(), new ESGEdge(
                                        sgNode.getIndex(),
                                        sourceDVarIdx,
                                        sgnTargetIdx,
                                        sourceDVarIdx));
                            } else {
                                Integer targetDVarIdx = domainVarMap.get(sgNode.getIndex()).get(sourceDVarIdx);
                                if(targetDVarIdx != null) {
                                    // green
                                    esgEdges.put(sgNode.getIndex(), new ESGEdge(
                                            sgNode.getIndex(),
                                            sourceDVarIdx,
                                            sgnTargetIdx,
                                            targetDVarIdx));
                                }
                            }
                        }
                    }
//                } else if (sgNode instanceof SGExitNode) {
//                    // Iterate over all domain vars in source
//                    for(Integer sourceDVarIdx : esgSourceNodeMap.keySet()) {
//                        // for all target sg nodes
//                        for(Integer sgnTargetIdx: sgnTargetIdxList) {
//                            if(sourceDVarIdx == -1) {
//                                // special case zeroVar
//                                esgEdges.put(sgNode.getIndex(), new ESGEdge(
//                                        sgNode.getIndex(),
//                                        sourceDVarIdx,
//                                        sgnTargetIdx,
//                                        sourceDVarIdx));
//                            } else {
//                                Integer targetDVarIdx = domainVarMap.get(sgNode.getIndex()).inverse().get(sourceDVarIdx);
//                                if(targetDVarIdx != null) {
//                                    // green
//                                    esgEdges.put(sgNode.getIndex(), new ESGEdge(
//                                            sgNode.getIndex(),
//                                            sourceDVarIdx,
//                                            sgnTargetIdx,
//                                            targetDVarIdx));
//                                }
//                            }
//                        }
//                    }
                } else {
                    // Iterate over all domain vars in source
                    for(Integer sourceDVarIdx : esgSourceNodeMap.keySet()) {
                        // for all target sg nodes
                        for(Integer sgnTargetIdx: sgnTargetIdxList) {
                            // green
                            esgEdges.put(sgNode.getIndex(), new ESGEdge(
                                    sgNode.getIndex(),
                                    sourceDVarIdx,
                                    sgnTargetIdx,
                                    sourceDVarIdx));
                        }
                    }
                }
            }
        }

        if(log.isDebugEnabled()) {
            StringBuilder sb = new StringBuilder();
            sb.append(cData.getRelativePath()).append(" ");
            sb.append(mData.buildInternalMethodName()).append("\n");
            sb.append(JDFCUtils.prettyPrintMultimap(esgEdges));
            JDFCUtils.logThis(sb.toString(), "exploded_edges");
        }
        return null;
    }
}
