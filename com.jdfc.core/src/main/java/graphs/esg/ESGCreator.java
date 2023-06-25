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

import java.util.*;

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
        String className = cData.getRelativePath();
        String methodName = mData.buildInternalMethodName();

        //--- CREATE DOMAIN --------------------------------------------------------------------------------------------
        Map<String, NavigableMap<Integer, DomainVariable>> domain = new HashMap<>();
        for(SGNode sgNode : sg.getNodes().values()) {
            String sgNodeMethodName = sgNode.getMethodName();
            if(!domain.containsKey(sgNodeMethodName)) {
                domain.put(sgNodeMethodName, Maps.newTreeMap());
                for(DomainVariable dVar : cData.getMethodByInternalName(sgNodeMethodName).getCfg().getDomain()) {
                    if(dVar.getName().equals("this") && !sgNodeMethodName.equals(methodName)) {
                        continue;
                    }
                    domain.get(sgNodeMethodName).put(dVar.getIndex(), dVar);
                }
            }
        }

        //--- DEBUG DOMAIN ---------------------------------------------------------------------------------------------
        if(log.isDebugEnabled()) {
            StringBuilder sb = new StringBuilder();
            sb.append(className).append(" ");
            sb.append(methodName).append("\n");

            for(Map.Entry<String, NavigableMap<Integer, DomainVariable>> domainMethodEntry : domain.entrySet()) {
                sb.append(domainMethodEntry.getKey()).append("\n");
                for(Map.Entry<Integer, DomainVariable> domainVariableEntry : domainMethodEntry.getValue().entrySet()) {
                    sb.append(domainVariableEntry.getValue()).append("  ");
                }
                sb.append("\n");
            }

            JDFCUtils.logThis(sb.toString(), "ESGCreator_domain");
        }

        //--- CREATE NODES ---------------------------------------------------------------------------------------------
        NavigableMap<Integer, Map<String, NavigableMap<Integer, ESGNode>>> esgNodes = Maps.newTreeMap();
        esgNodes.computeIfAbsent(Integer.MIN_VALUE, k -> Maps.newTreeMap());
        esgNodes.get(Integer.MIN_VALUE).computeIfAbsent(methodName, k -> Maps.newTreeMap());
        esgNodes.get(Integer.MIN_VALUE)
                .get(methodName)
                .put(-1, new ESGNode.ESGZeroNode(Integer.MIN_VALUE));

        for(SGNode sgNode : sg.getNodes().values()) {
            int sgNodeIdx = sgNode.getIndex();
            String sgNodeMethodName = sgNode.getMethodName();
            esgNodes.computeIfAbsent(sgNodeIdx, k -> Maps.newTreeMap());
            esgNodes.get(sgNodeIdx).computeIfAbsent(methodName, k -> Maps.newTreeMap());
            esgNodes.get(sgNodeIdx)
                    .get(methodName)
                    .put(-1, new ESGNode.ESGZeroNode(sgNodeIdx));

            for(Map.Entry<String, NavigableMap<Integer, DomainVariable>> domainMethodEntry : domain.entrySet()) {
                for(Map.Entry<Integer, DomainVariable> dVarEntry : domainMethodEntry.getValue().entrySet()) {
                    esgNodes.get(sgNodeIdx).computeIfAbsent(domainMethodEntry.getKey(), k -> Maps.newTreeMap());
                    esgNodes.get(sgNodeIdx)
                            .get(domainMethodEntry.getKey())
                            .put(dVarEntry.getKey(), new ESGNode(sgNodeIdx, dVarEntry.getValue()));
                }
            }
        }

        // --- DEBUG NODES ---------------------------------------------------------------------------------------------
        if(log.isDebugEnabled()) {
            StringBuilder sb = new StringBuilder();
            sb.append(className).append(" ");
            sb.append(methodName).append("\n");

            for(Map.Entry<Integer, Map<String, NavigableMap<Integer, ESGNode>>> esgSGNodeEntry : esgNodes.entrySet()) {
                for(Map.Entry<String, NavigableMap<Integer, ESGNode>> esgNodesMethodEntry : esgSGNodeEntry.getValue().entrySet()) {
                    for(Map.Entry<Integer, ESGNode> esgNodeEntry : esgNodesMethodEntry.getValue().entrySet()) {
                        sb.append(esgNodeEntry.getValue()).append("  ");
                    }
                }
                sb.append("\n");
            }

            JDFCUtils.logThis(sb.toString(), "exploded_nodes");
        }

        //--- CREATE EDGES ---------------------------------------------------------------------------------------------
        Multimap<Integer, ESGEdge> esgEdges = ArrayListMultimap.create();

        // iterate over esg
        for(Map.Entry<Integer, Map<String, NavigableMap<Integer, ESGNode>>> esgSGNodeEntry : esgNodes.entrySet()) {
            if(esgSGNodeEntry.getKey() == Integer.MIN_VALUE) {
                continue;
            }
            SGNode sgNode = sg.getNodes().get(esgSGNodeEntry.getKey());
            int sgNodeIdx = sgNode.getIndex();
            String sgNodeMethodName = sgNode.getMethodName();
            Collection<Integer> sgnTargetIdxList = sg.getEdges().get(sgNodeIdx);

            // iterate over methods in node
            for(Map.Entry<String, NavigableMap<Integer, ESGNode>> esgNodesMethodEntry : esgSGNodeEntry.getValue().entrySet()) {
                // iterate over esg nodes
                for(Map.Entry<Integer, ESGNode> esgNodeEntry : esgNodesMethodEntry.getValue().entrySet()) {
                    DomainVariable dVar = esgNodeEntry.getValue().getDVar();
                    int dVarIdx = esgNodeEntry.getValue().getDVar().getIndex();
                    if(sgNodeIdx == 0) {
                        // special case for very first node of the sg
                        esgEdges.put(Integer.MIN_VALUE, new ESGEdge(
                                Integer.MIN_VALUE,
                                0,
                                sgNodeMethodName,
                                esgNodesMethodEntry.getKey(),
                                -1,
                                dVarIdx));
                    }

                    if (sgNode instanceof SGCallNode) {
                        SGCallNode sgCallNode = (SGCallNode) sgNode;
                        // for all target sg nodes
                        for(Integer sgnTargetIdx: sgnTargetIdxList) {
                            if(dVarIdx == -1 || dVarIdx == 0) {
                                // special case zeroVar
                                esgEdges.put(sgNodeIdx, new ESGEdge(
                                        sgNodeIdx,
                                        sgnTargetIdx,
                                        esgNodesMethodEntry.getKey(),
                                        esgNodesMethodEntry.getKey(),
                                        dVarIdx,
                                        dVarIdx));
                            } else {
                                JDFCUtils.logThis("\n" + sgCallNode.getCalledMethodName() + "\n" + dVarIdx + "\n" + JDFCUtils.prettyPrintMap(sgCallNode.getDVarMap()), "hepp");
                                Integer targetDVarIdx = sgCallNode.getDVarMap().get(dVarIdx);
                                if(targetDVarIdx != null
                                        && dVar.getMethodName().equals(methodName)) {
                                    JDFCUtils.logThis("\n" + dVar.getName() + "\n" + targetDVarIdx, "entered");
                                    // green
                                    esgEdges.put(sgNodeIdx, new ESGEdge(
                                            sgNodeIdx,
                                            sgnTargetIdx,
                                            esgNodesMethodEntry.getKey(),
                                            sgCallNode.getCalledMethodName(),
                                            dVarIdx,
                                            targetDVarIdx));
                                }

                                esgEdges.put(sgNodeIdx, new ESGEdge(
                                        sgNodeIdx,
                                        sgnTargetIdx,
                                        esgNodesMethodEntry.getKey(),
                                        sgCallNode.getCalledMethodName(),
                                        dVarIdx,
                                        dVarIdx));
                                JDFCUtils.logThis("\n" + dVar.getName() + "\n" + targetDVarIdx, "not_entered");

                            }
                        }
                        // Iterate over all domain vars in source
                    } else if (sgNode instanceof SGExitNode) {
                        SGExitNode sgExitNode = (SGExitNode) sgNode;
                        // for all target sg nodes
                        for(Integer sgnTargetIdx: sgnTargetIdxList) {
                            String targetMethodName = sg.getNodes().get(sgnTargetIdx).getMethodName();
                            if(dVarIdx == -1) {
                                // special case zeroVar
                                esgEdges.put(sgNodeIdx, new ESGEdge(
                                        sgNodeIdx,
                                        sgnTargetIdx,
                                        esgNodesMethodEntry.getKey(),
                                        esgNodesMethodEntry.getKey(),
                                        dVarIdx,
                                        dVarIdx));
                            } else {
                                Integer targetDVarIdx = sgExitNode.getDVarMap().get(dVarIdx);
                                if(targetDVarIdx != null) {
                                    // green
                                    esgEdges.put(sgNodeIdx, new ESGEdge(
                                            sgNodeIdx,
                                            sgnTargetIdx,
                                            esgNodesMethodEntry.getKey(),
                                            esgNodesMethodEntry.getKey(),
                                            dVarIdx,
                                            targetDVarIdx));
                                }
                            }
                        }
                    } else {
                        // for all target sg nodes
                        for(Integer sgnTargetIdx: sgnTargetIdxList) {
                            SGNode targetNode = sg.getNodes().get(sgnTargetIdx);
                            if(targetNode.getDefinitions().stream().noneMatch(pVar ->
                                    Objects.equals(pVar.getName(), dVar.getName())
                                            && Objects.equals(pVar.getDescriptor(), dVar.getDescriptor()))) {
                                esgEdges.put(sgNodeIdx, new ESGEdge(
                                        sgNodeIdx,
                                        sgnTargetIdx,
                                        esgNodesMethodEntry.getKey(),
                                        esgNodesMethodEntry.getKey(),
                                        dVarIdx,
                                        dVarIdx));
                            } else {
                                esgNodesMethodEntry.getValue().get(dVarIdx).setPossiblyNotRedefined(false);
                            }
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

        //--- PRED & SUCC
        for(ESGEdge esgEdge : esgEdges.values()) {
            int sgnSourceIdx = esgEdge.getSgnSourceIdx();
            int sgnTargetIdx = esgEdge.getSgnTargetIdx();
            String sourceMethodName = esgEdge.getSourceDVarMethodName();
            String targetMethodName = esgEdge.getTargetDVarMethodName();
            int sourceDVarIdx = esgEdge.getSourceDVarIdx();
            int targetDVarIdx = esgEdge.getTargetDVarIdx();

            String debug = String.format("%d %s %d %d %s %d",
                    sgnSourceIdx, sourceMethodName, sourceDVarIdx, sgnTargetIdx, targetMethodName, targetDVarIdx);
            JDFCUtils.logThis(debug, "debug");

            ESGNode first = esgNodes.get(sgnSourceIdx).get(sourceMethodName).get(sourceDVarIdx);
            ESGNode second = esgNodes.get(sgnTargetIdx).get(targetMethodName).get(targetDVarIdx);
            first.getSucc().add(second);
            second.getPred().add(first);
        }

//        // --- DEBUG NODES ---------------------------------------------------------------------------------------------
//        if(log.isDebugEnabled()) {
//            StringBuilder sb = new StringBuilder();
//            sb.append(className).append(" ");
//            sb.append(methodName).append("\n");
//
//            for(Map.Entry<Integer, Map<String, NavigableMap<Integer, ESGNode>>> esgSGNodeEntry : esgNodes.entrySet()) {
//                for(Map.Entry<String, NavigableMap<Integer, ESGNode>> esgNodesMethodEntry : esgSGNodeEntry.getValue().entrySet()) {
//                    for(Map.Entry<Integer, ESGNode> esgNodeEntry : esgNodesMethodEntry.getValue().entrySet()) {
//                        sb.append(esgNodeEntry.getValue()).append("  ");
//                    }
//                }
//                sb.append("\n");
//            }
//
//            JDFCUtils.logThis(sb.toString(), "exploded_nodes");
//        }

//        //--- DEGUG EDGES ----------------------------------------------------------------------------------------------
//        if(log.isDebugEnabled()) {
//            StringBuilder sb = new StringBuilder();
//            sb.append(cData.getRelativePath()).append(" ");
//            sb.append(mData.buildInternalMethodName()).append("\n");
//            sb.append(JDFCUtils.prettyPrintMultimap(esgEdges));
//            JDFCUtils.logThis(sb.toString(), "exploded_edges");
//        }

        //--- CREATE ESG -----------------------------------------------------------------------------------------------
        return new ESG(sg, esgNodes, esgEdges);
    }
}
