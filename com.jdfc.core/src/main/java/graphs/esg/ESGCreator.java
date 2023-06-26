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
        String mainMethodKey = String.format("%s :: %s", className, methodName);


        //--- CREATE DOMAIN --------------------------------------------------------------------------------------------
        Map<String, NavigableMap<Integer, DomainVariable>> domain = new HashMap<>();
        for(SGNode sgNode : sg.getNodes().values()) {
            String sgNodeClassName = sgNode.getClassName();
            String sgNodeMethodName = sgNode.getMethodName();
            String sgNodeMethodKey = ESGCreator.buildMethodIdentifier(sgNodeClassName, sgNodeMethodName);
            if(!domain.containsKey(sgNodeMethodKey)) {
                domain.put(sgNodeMethodKey, Maps.newTreeMap());
                for(Map.Entry<Integer, DomainVariable> dVarEntry : cData.getMethodByInternalName(sgNodeMethodName).getCfg().getDomain().entrySet()) {
                    String dVarQualifier = ESGCreator.buildMethodIdentifier(dVarEntry.getValue().getClassName(), dVarEntry.getValue().getMethodName());
                    if(dVarEntry.getValue().getName().equals("this") && !dVarQualifier.equals(mainMethodKey)) {
                        continue;
                    }
                    if(Objects.equals(sgNodeMethodKey, mainMethodKey)) {
                        domain.get(sgNodeMethodKey).put(-1, new DomainVariable.ZeroVariable(className, methodName));
                    }
                    domain.get(sgNodeMethodKey).put(dVarEntry.getKey(), dVarEntry.getValue());
                }
            }
        }

        //--- DEBUG DOMAIN ---------------------------------------------------------------------------------------------
        if(log.isDebugEnabled()) {
            StringBuilder sb = new StringBuilder();
            sb.append(mainMethodKey).append("\n");

            for(Map.Entry<String, NavigableMap<Integer, DomainVariable>> domainMethodEntry : domain.entrySet()) {
                sb.append(domainMethodEntry.getKey()).append("\n");
                sb.append(JDFCUtils.prettyPrintMap(domainMethodEntry.getValue()));
                sb.append("\n");
            }

            JDFCUtils.logThis(sb.toString(), "ESGCreator_domain");
        }

        //--- CREATE NODES ---------------------------------------------------------------------------------------------
        NavigableMap<Integer, Map<String, NavigableMap<Integer, ESGNode>>> esgNodes = Maps.newTreeMap();
        esgNodes.computeIfAbsent(Integer.MIN_VALUE, k -> Maps.newTreeMap());
        esgNodes.get(Integer.MIN_VALUE).computeIfAbsent(mainMethodKey, k -> Maps.newTreeMap());
        esgNodes.get(Integer.MIN_VALUE)
                .get(mainMethodKey)
                .put(-1, new ESGNode.ESGZeroNode(Integer.MIN_VALUE, className, methodName));

        for(SGNode sgNode : sg.getNodes().values()) {
            int sgNodeIdx = sgNode.getIndex();

            esgNodes.computeIfAbsent(sgNodeIdx, k -> Maps.newTreeMap());
            esgNodes.get(sgNodeIdx).computeIfAbsent(mainMethodKey, k -> Maps.newTreeMap());
            esgNodes.get(sgNodeIdx)
                    .get(mainMethodKey)
                    .put(-1, new ESGNode.ESGZeroNode(sgNodeIdx, className, methodName));

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

        //--- CREATE EDGES ---------------------------------------------------------------------------------------------
        Multimap<Integer, ESGEdge> esgEdges = ArrayListMultimap.create();

        // iterate over esg
        for(Map.Entry<Integer, Map<String, NavigableMap<Integer, ESGNode>>> esgSGNodeEntry : esgNodes.entrySet()) {
            if(esgSGNodeEntry.getKey() == Integer.MIN_VALUE) {
                continue;
            }
            SGNode sgNode = sg.getNodes().get(esgSGNodeEntry.getKey());
            int sgNodeIdx = sgNode.getIndex();
            String sgNodeClassName = sgNode.getClassName();
            String sgNodeMethodName = sgNode.getMethodName();
            String sgNodeMethodKey = ESGCreator.buildMethodIdentifier(sgNodeClassName, sgNodeMethodName);
            Collection<Integer> sgnTargetIdxList = sg.getEdges().get(sgNodeIdx);

            Map<String, NavigableMap<Integer, DomainVariable>> activeDomain = new HashMap<>();
            activeDomain.computeIfAbsent(mainMethodKey, k -> domain.get(mainMethodKey));
            if(sgNode instanceof SGCallNode) {
                String calledClassName = ((SGCallNode) sgNode).getCalledClassName();
                String calledMethodName = ((SGCallNode) sgNode).getCalledMethodName();
                String subroutineKey = ESGCreator.buildMethodIdentifier(calledClassName, calledMethodName);
                activeDomain.computeIfAbsent(subroutineKey, k -> domain.get(subroutineKey));
            }
            if(!(sgNode instanceof SGExitNode)) {
                activeDomain.computeIfAbsent(sgNodeMethodKey, k -> domain.get(sgNodeMethodKey));
            }

            if(log.isDebugEnabled()) {
                StringBuilder sb = new StringBuilder();
                sb.append(mainMethodKey).append("\n");

                for(Map.Entry<String, NavigableMap<Integer, DomainVariable>> domainMethodEntry : activeDomain.entrySet()) {
                    sb.append(domainMethodEntry.getKey()).append("\n");
                    sb.append(JDFCUtils.prettyPrintMap(domainMethodEntry.getValue()));
                    sb.append("\n");
                }

                JDFCUtils.logThis(sb.toString(), String.valueOf(sgNodeIdx));
            }

            // iterate over methods in node
            for(Map.Entry<String, NavigableMap<Integer, ESGNode>> esgNodesMethodEntry : esgSGNodeEntry.getValue().entrySet()) {
                // iterate over esg nodes
                for(Map.Entry<Integer, ESGNode> esgNodeEntry : esgNodesMethodEntry.getValue().entrySet()) {
                    DomainVariable dVar = esgNodeEntry.getValue().getDVar();
                    int dVarIdx = esgNodeEntry.getValue().getDVar().getIndex();

                    // check if current node is part of active domain
                    for(Map.Entry<String, NavigableMap<Integer, DomainVariable>> activeDomainMethodEntry : activeDomain.entrySet()) {
                        if(activeDomainMethodEntry.getValue().containsValue(dVar)) {
                            // Insert node before idx 0
                            if(sgNodeIdx == 0) {
                                // special case for very first node of the sg
                                esgEdges.put(Integer.MIN_VALUE, new ESGEdge(
                                        Integer.MIN_VALUE,
                                        0,
                                        mainMethodKey,
                                        sgNodeMethodKey,
                                        -1,
                                        dVarIdx));
                            }

                            if (sgNode instanceof SGCallNode) {
                                SGCallNode sgCallNode = (SGCallNode) sgNode;
                                // for all target sg nodes
                                for(Integer sgnTargetIdx: sgnTargetIdxList) {
                                    // variables are not passed -> draw edge from var to var
                                    //                          -> insert 0 to var edge for initialization
                                    // varibales are passed -> draw matching edge

                                    if(dVar.getClassName().equals(className)) {
                                        // determine target node
                                        DomainVariable targetDVar = sgCallNode.getDVarMap().get(dVar);
                                        String calledMethodKey = buildMethodIdentifier(sgCallNode.getCalledClassName(), sgCallNode.getCalledMethodName());
                                        // look at variables at main domain
                                        if(dVar.getMethodName().equals(sgNodeMethodName)) {
                                            // if target exists
                                            if(targetDVar != null) {
                                                // Match parameters
                                                esgEdges.put(sgNodeIdx, new ESGEdge(
                                                        sgNodeIdx,
                                                        sgnTargetIdx,
                                                        esgNodesMethodEntry.getKey(),
                                                        calledMethodKey,
                                                        dVarIdx,
                                                        targetDVar.getIndex()));
                                            } else {
                                                //  No matching -> draw straight line for own variables
                                                esgEdges.put(sgNodeIdx, new ESGEdge(
                                                        sgNodeIdx,
                                                        sgnTargetIdx,
                                                        esgNodesMethodEntry.getKey(),
                                                        esgNodesMethodEntry.getKey(),
                                                        dVarIdx,
                                                        dVarIdx));
                                            }
                                        } else {
                                            if(dVarIdx == -1 || dVarIdx == 0) {
                                                // initialize other variables from new domain
                                                esgEdges.put(sgNodeIdx, new ESGEdge(
                                                        sgNodeIdx,
                                                        sgnTargetIdx,
                                                        mainMethodKey,
                                                        mainMethodKey,
                                                        dVarIdx,
                                                        dVarIdx));
                                            } else {
                                                // initialize other variables from new domain
                                                esgEdges.put(sgNodeIdx, new ESGEdge(
                                                        sgNodeIdx,
                                                        sgnTargetIdx,
                                                        mainMethodKey,
                                                        calledMethodKey,
                                                        -1,
                                                        dVarIdx));
                                            }
                                        }
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
                                        DomainVariable targetDVar = sgExitNode.getDVarMap().get(dVar);
                                        if(targetDVar != null) {
                                            esgEdges.put(sgNodeIdx, new ESGEdge(
                                                    sgNodeIdx,
                                                    sgnTargetIdx,
                                                    esgNodesMethodEntry.getKey(),
                                                    esgNodesMethodEntry.getKey(),
                                                    dVarIdx,
                                                    targetDVar.getIndex()));
                                        }
                                        esgEdges.put(sgNodeIdx, new ESGEdge(
                                                sgNodeIdx,
                                                sgnTargetIdx,
                                                esgNodesMethodEntry.getKey(),
                                                esgNodesMethodEntry.getKey(),
                                                dVarIdx,
                                                dVarIdx));
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
            }
        }

        //--- DEGUG EDGES ----------------------------------------------------------------------------------------------
//        if(log.isDebugEnabled()) {
//            StringBuilder sb = new StringBuilder();
//            sb.append(cData.getRelativePath()).append(" ");
//            sb.append(mData.buildInternalMethodName()).append("\n");
//            sb.append(JDFCUtils.prettyPrintMultimap(esgEdges));
//            JDFCUtils.logThis(sb.toString(), "exploded_edges");
//        }

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

            second.setPossiblyNotRedefined(true);
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

        //--- DEGUG EDGES ----------------------------------------------------------------------------------------------
        if(log.isDebugEnabled()) {
            StringBuilder sb = new StringBuilder();
            sb.append(cData.getRelativePath()).append(" ");
            sb.append(mData.buildInternalMethodName()).append("\n");
            sb.append(JDFCUtils.prettyPrintMultimap(esgEdges));
            JDFCUtils.logThis(sb.toString(), "exploded_edges");
        }

        //--- CREATE ESG -----------------------------------------------------------------------------------------------
        return new ESG(sg, esgNodes, esgEdges);
    }


    private static String buildMethodIdentifier(String className, String methodName) {
        return String.format("%s :: %s", className, methodName);
    }
}
