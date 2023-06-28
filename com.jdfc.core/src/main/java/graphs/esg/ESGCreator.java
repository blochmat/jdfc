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
        String mainMethodClassName = cData.getRelativePath();
        String mainMethodName = mData.buildInternalMethodName();
        String mainMethodIdentifier = ESGCreator.buildMethodIdentifier(mainMethodClassName, mainMethodName);


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
                    if(dVarEntry.getValue().getName().equals("this") && !dVarQualifier.equals(mainMethodIdentifier)) {
                        continue;
                    }
                    if(Objects.equals(sgNodeMethodKey, mainMethodIdentifier)) {
                        domain.get(sgNodeMethodKey).put(-1, new DomainVariable.ZeroVariable(mainMethodClassName, mainMethodName));
                    }
                    domain.get(sgNodeMethodKey).put(dVarEntry.getKey(), dVarEntry.getValue());
                }
            }
        }

        //--- DEBUG DOMAIN ---------------------------------------------------------------------------------------------
        if(log.isDebugEnabled()) {
            StringBuilder sb = new StringBuilder();
            sb.append(mainMethodIdentifier).append("\n");

            for(Map.Entry<String, NavigableMap<Integer, DomainVariable>> domainMethodEntry : domain.entrySet()) {
                sb.append(domainMethodEntry.getKey()).append("\n");
                sb.append(JDFCUtils.prettyPrintMap(domainMethodEntry.getValue()));
                sb.append("\n");
            }

            JDFCUtils.logThis(sb.toString(), "ESGCreator_domain");
        }

        //--- CREATE NODES ---------------------------------------------------------------------------------------------
        NavigableMap<Integer, Map<String, NavigableMap<Integer, ESGNode>>> esgNodes = Maps.newTreeMap();
//        esgNodes.computeIfAbsent(Integer.MIN_VALUE, k -> Maps.newTreeMap());
//        esgNodes.get(Integer.MIN_VALUE).computeIfAbsent(mainMethodIdentifier, k -> Maps.newTreeMap());
//        esgNodes.get(Integer.MIN_VALUE)
//                .get(mainMethodIdentifier)
//                .put(-1, new ESGNode.ESGZeroNode(Integer.MIN_VALUE, mainMethodClassName, mainMethodName));

        for(SGNode sgNode : sg.getNodes().values()) {
            int sgNodeIdx = sgNode.getIndex();

            esgNodes.computeIfAbsent(sgNodeIdx, k -> Maps.newTreeMap());
            esgNodes.get(sgNodeIdx).computeIfAbsent(mainMethodIdentifier, k -> Maps.newTreeMap());
            esgNodes.get(sgNodeIdx)
                    .get(mainMethodIdentifier)
                    .put(-1, new ESGNode.ESGZeroNode(sgNodeIdx, mainMethodClassName, mainMethodName));

            for(Map.Entry<String, NavigableMap<Integer, DomainVariable>> domainMethodEntry : domain.entrySet()) {
                for(Map.Entry<Integer, DomainVariable> dVarEntry : domainMethodEntry.getValue().entrySet()) {
                    esgNodes.get(sgNodeIdx).computeIfAbsent(domainMethodEntry.getKey(), k -> Maps.newTreeMap());

                    if(dVarEntry.getValue() instanceof DomainVariable.ZeroVariable) {
                        esgNodes.get(sgNodeIdx)
                                .get(mainMethodIdentifier)
                                .put(-1, new ESGNode.ESGZeroNode(sgNodeIdx, mainMethodClassName, mainMethodName));
                    } else {
                        esgNodes.get(sgNodeIdx)
                                .get(domainMethodEntry.getKey())
                                .put(dVarEntry.getKey(), new ESGNode(sgNodeIdx, dVarEntry.getValue()));
                    }
                }
            }
        }

        // --- DEBUG NODES ---------------------------------------------------------------------------------------------
//        if(log.isDebugEnabled()) {
//            StringBuilder sb = new StringBuilder();
//            sb.append(mainMethodClassName).append(" ");
//            sb.append(mainMethodName).append("\n");
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

        for(SGNode currSGNode : sg.getNodes().values()) {
            int currSGNodeIdx = currSGNode.getIndex();
            String currSGNodeClassName = currSGNode.getClassName();
            String currSGNodeMethodName = currSGNode.getMethodName();
            String currSGNodeMethodIdentifier = ESGCreator.buildMethodIdentifier(currSGNodeClassName, currSGNodeMethodName);

            // all domain variables \ esg nodes possibly reachable from the current sg idx
            Map<String, NavigableMap<Integer, DomainVariable>> reachableDVars = new HashMap<>();
            reachableDVars.computeIfAbsent(mainMethodIdentifier, k -> domain.get(mainMethodIdentifier));
            if(currSGNode instanceof SGCallNode) {
                String calledClassName = ((SGCallNode) currSGNode).getCalledClassName();
                String calledMethodName = ((SGCallNode) currSGNode).getCalledMethodName();
                String subroutineKey = ESGCreator.buildMethodIdentifier(calledClassName, calledMethodName);
                reachableDVars.computeIfAbsent(subroutineKey, k -> domain.get(subroutineKey));
            }
            if(!(currSGNode instanceof SGExitNode)) {
                reachableDVars.computeIfAbsent(currSGNodeMethodIdentifier, k -> domain.get(currSGNodeMethodIdentifier));
            }

            //--- DEBUG ACTIVE DOMAIN ----------------------------------------------------------------------------------
            if(log.isDebugEnabled()
                    && (currSGNodeIdx == 19
                        || currSGNodeIdx == 20
                        || currSGNodeIdx == 32
                        || currSGNodeIdx == 33
                        || currSGNodeIdx == 56
                        || currSGNodeIdx == 57)) {
                StringBuilder sb = new StringBuilder();
                sb.append(mainMethodIdentifier).append("\n");

                for(Map.Entry<String, NavigableMap<Integer, DomainVariable>> domainMethodEntry : reachableDVars.entrySet()) {
                    sb.append(domainMethodEntry.getKey()).append("\n");
                    sb.append(JDFCUtils.prettyPrintMap(domainMethodEntry.getValue()));
                    sb.append("\n");
                }

                JDFCUtils.logThis(sb.toString(), String.valueOf(currSGNodeIdx));
            }

//            if(log.isDebugEnabled()) {
//                StringBuilder sb = new StringBuilder();
//                sb.append(mainMethodIdentifier).append("\n");
//
//                for(Map.Entry<String, NavigableMap<Integer, DomainVariable>> domainMethodEntry : reachableDVars.entrySet()) {
//                    sb.append(domainMethodEntry.getKey()).append("\n");
//                    sb.append(JDFCUtils.prettyPrintMap(domainMethodEntry.getValue()));
//                    sb.append("\n");
//                }
//
//                JDFCUtils.logThis(sb.toString(), String.valueOf(currSGNodeIdx));
//            }

            // for every reachable domain variable / esg node method section
            for(Map.Entry<String, NavigableMap<Integer, DomainVariable>> reachableDVarMethodEntry : reachableDVars.entrySet()) {
                String currMethodIdentifier = reachableDVarMethodEntry.getKey();
                NavigableMap<Integer, DomainVariable> domainVariables = reachableDVarMethodEntry.getValue();

                if(currMethodIdentifier.equals(mainMethodIdentifier)) {
                    // main method dVars
                    for (DomainVariable dVar : domainVariables.values()) {
                        int dVarIdx = dVar.getIndex();

//                        if (currSGNodeIdx == 0) {
//                            // special case for very first node of the sg
//                            esgEdges.put(Integer.MIN_VALUE, new ESGEdge(
//                                    Integer.MIN_VALUE,
//                                    0,
//                                    currMethodIdentifier,
//                                    currMethodIdentifier,
//                                    -1,
//                                    dVarIdx));
//                        }

                        Collection<Integer> currSGNodeTargets = sg.getEdges().get(currSGNodeIdx);
                        for (Integer currSGNodeTargetIdx : currSGNodeTargets) {
                            if (currSGNode instanceof SGCallNode) {
                                SGCallNode currSGCallNode = (SGCallNode) currSGNode;

                                if (currSGCallNode.isCalledSGPresent()) {
                                    DomainVariable targetDVar = currSGCallNode.getDVarMap().get(dVar);
                                    String calledMethodIdentifier = ESGCreator.buildMethodIdentifier(currSGCallNode.getCalledClassName(), currSGCallNode.getCalledMethodName());

                                    if (targetDVar != null) {
                                        // Match found
                                        esgEdges.put(currSGNodeIdx, new ESGEdge(
                                                currSGNodeIdx,
                                                currSGNodeTargetIdx,
                                                currMethodIdentifier,
                                                calledMethodIdentifier,
                                                dVarIdx,
                                                targetDVar.getIndex()));

                                        esgEdges.put(currSGNodeIdx, new ESGEdge(
                                                currSGNodeIdx,
                                                currSGNodeTargetIdx,
                                                currMethodIdentifier,
                                                currMethodIdentifier,
                                                dVarIdx,
                                                dVarIdx));
                                    } else {
                                        //  No matching: draw straight line for own variables
                                        esgEdges.put(currSGNodeIdx, new ESGEdge(
                                                currSGNodeIdx,
                                                currSGNodeTargetIdx,
                                                currMethodIdentifier,
                                                currMethodIdentifier,
                                                dVarIdx,
                                                dVarIdx));
                                    }
                                } else {
                                    // if SG is not present
                                    // draw straight line for own var
                                    esgEdges.put(currSGNodeIdx, new ESGEdge(
                                            currSGNodeIdx,
                                            currSGNodeTargetIdx,
                                            currMethodIdentifier,
                                            currMethodIdentifier,
                                            dVarIdx,
                                            dVarIdx));
                                }
                            } else {
                                if(currSGNode.getIndex() == 0) {
                                    // initialize variables of main domain
                                    esgEdges.put(currSGNodeIdx, new ESGEdge(
                                            currSGNodeIdx,
                                            currSGNodeTargetIdx,
                                            currMethodIdentifier,
                                            currMethodIdentifier,
                                            -1,
                                            dVarIdx));
                                } else {
                                    // create edge if variable is not redefined
                                    SGNode targetNode = sg.getNodes().get(currSGNodeTargetIdx);
                                    if (targetNode.getDefinitions().stream().noneMatch(pVar ->
                                            Objects.equals(pVar.getName(), dVar.getName())
                                                    && Objects.equals(pVar.getDescriptor(), dVar.getDescriptor()))) {
                                        esgEdges.put(currSGNodeIdx, new ESGEdge(
                                                currSGNodeIdx,
                                                currSGNodeTargetIdx,
                                                currMethodIdentifier,
                                                currMethodIdentifier,
                                                dVarIdx,
                                                dVarIdx));
                                    } else {
                                        esgNodes.get(currSGNodeIdx).get(currMethodIdentifier).get(dVarIdx).setPossiblyNotRedefined(false);
                                    }
                                }
                            }
                        }
                    }
                }
                else {
                    // subroutine dVars
                    for (DomainVariable dVar : domainVariables.values()) {
                        int dVarIdx = dVar.getIndex();

                        Collection<Integer> currSGNodeTargets = sg.getEdges().get(currSGNodeIdx);
                        for (Integer currSGNodeTargetIdx : currSGNodeTargets) {
                            if (currSGNode instanceof SGCallNode) {
                                SGCallNode currSGCallNode = (SGCallNode) currSGNode;

                                if (currSGCallNode.isCalledSGPresent()) {
                                    DomainVariable targetDVar = currSGCallNode.getDVarMap().get(dVar);
                                    String calledMethodIdentifier = ESGCreator.buildMethodIdentifier(currSGCallNode.getCalledClassName(), currSGCallNode.getCalledMethodName());

                                    if (targetDVar != null) {
                                        // Match found
                                        esgEdges.put(currSGNodeIdx, new ESGEdge(
                                                currSGNodeIdx,
                                                currSGNodeTargetIdx,
                                                currMethodIdentifier,
                                                calledMethodIdentifier,
                                                dVarIdx,
                                                targetDVar.getIndex()));
                                    } else {
                                        if(currMethodIdentifier.equals(calledMethodIdentifier)
                                                && !currSGCallNode.getDVarMap().containsValue(dVar)) {
                                            // initialize variables of called method
                                            esgEdges.put(currSGNodeIdx, new ESGEdge(
                                                    currSGNodeIdx,
                                                    currSGNodeTargetIdx,
                                                    mainMethodIdentifier,
                                                    currMethodIdentifier,
                                                    -1,
                                                    dVarIdx));
                                        }
                                    }
                                } else {
                                    // if SG is not present
                                    // draw straight line for own var
                                    esgEdges.put(currSGNodeIdx, new ESGEdge(
                                            currSGNodeIdx,
                                            currSGNodeTargetIdx,
                                            currMethodIdentifier,
                                            currMethodIdentifier,
                                            dVarIdx,
                                            dVarIdx));
                                }
                            }
                            else if(currSGNode instanceof SGExitNode) {
                                SGExitNode currSGExitNode = (SGExitNode) currSGNode;
                                DomainVariable targetDVar = currSGExitNode.getDVarMap().get(dVar);

                                if (targetDVar != null) {
                                    // Match found
                                    esgEdges.put(currSGNodeIdx, new ESGEdge(
                                            currSGNodeIdx,
                                            currSGNodeTargetIdx,
                                            currMethodIdentifier,
                                            buildMethodIdentifier(targetDVar.getClassName(), targetDVar.getMethodName()),
                                            dVarIdx,
                                            targetDVar.getIndex()));
                                }

                            }
                            else {
                                // create edge if variable is not redefined
                                SGNode targetNode = sg.getNodes().get(currSGNodeTargetIdx);
                                if (targetNode.getDefinitions().stream().noneMatch(pVar ->
                                        Objects.equals(pVar.getName(), dVar.getName())
                                                && Objects.equals(pVar.getDescriptor(), dVar.getDescriptor()))) {
                                    esgEdges.put(currSGNodeIdx, new ESGEdge(
                                            currSGNodeIdx,
                                            currSGNodeTargetIdx,
                                            currMethodIdentifier,
                                            currMethodIdentifier,
                                            dVarIdx,
                                            dVarIdx));
                                } else {
                                    esgNodes.get(currSGNodeIdx).get(currMethodIdentifier).get(dVarIdx).setPossiblyNotRedefined(false);
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
            sb.append(mainMethodClassName).append(" ");
            sb.append(mainMethodName).append("\n");

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
