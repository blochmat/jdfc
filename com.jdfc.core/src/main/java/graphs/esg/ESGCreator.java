package graphs.esg;

import algos.TabulationAlgorithm;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import data.ClassExecutionData;
import data.MethodData;
import data.ProgramVariable;
import graphs.esg.nodes.ESGNode;
import graphs.sg.SG;
import graphs.sg.nodes.SGCallNode;
import graphs.sg.nodes.SGEntryNode;
import graphs.sg.nodes.SGExitNode;
import graphs.sg.nodes.SGNode;
import lombok.extern.slf4j.Slf4j;
import utils.JDFCUtils;

import java.util.*;

@Slf4j
public class ESGCreator {

    public static void createESGsForClass(ClassExecutionData cData) {
        for(MethodData mData : cData.getMethods().values()) {
            ESG esg = ESGCreator.createESGForMethod(cData, mData);
            mData.setEsg(esg);
            TabulationAlgorithm tabulationAlgorithm = new TabulationAlgorithm(esg);
            Multimap<Integer, ProgramVariable> MVP = tabulationAlgorithm.execute();
            String debug = String.format("%s :: %s\n%s",
                    cData.getRelativePath(),
                    mData.buildInternalMethodName(),
                    JDFCUtils.prettyPrintMultimap(MVP));
            JDFCUtils.logThis(debug, "MVP");
        }
    }

    public static ESG createESGForMethod(ClassExecutionData cData, MethodData mData) {
        SG sg = mData.getSg();
        String mainMethodClassName = cData.getRelativePath();
        String mainMethodName = mData.buildInternalMethodName();
        String mainMethodIdentifier = ESGCreator.buildMethodIdentifier(mainMethodClassName, mainMethodName);
        Map<ProgramVariable, Boolean> liveVariableMap = new HashMap<>();


        //--- CREATE DOMAIN --------------------------------------------------------------------------------------------
        ProgramVariable ZERO = new ProgramVariable.ZeroVariable(mainMethodClassName, mainMethodName);
        Map<String, Map<UUID, ProgramVariable>> domain = new HashMap<>();
        for(SGNode sgNode : sg.getNodes().values()) {
            String sgNodeClassName = sgNode.getClassName();
            String sgNodeMethodName = sgNode.getMethodName();
            String sgNodeMethodIdentifier = ESGCreator.buildMethodIdentifier(sgNodeClassName, sgNodeMethodName);
            domain.computeIfAbsent(sgNodeMethodIdentifier, k -> new HashMap<>());
            if(Objects.equals(sgNodeMethodIdentifier, mainMethodIdentifier)) {
                domain.get(sgNodeMethodIdentifier).put(ZERO.getId(), ZERO);
                liveVariableMap.put(ZERO, true);
            }
            for(ProgramVariable def : sgNode.getDefinitions()) {
                if(!(Objects.equals(def.getName(), "this") && !Objects.equals(sgNodeMethodIdentifier, mainMethodIdentifier))) {
                    domain.get(sgNodeMethodIdentifier).put(def.getId(), def);
                    liveVariableMap.put(def, false);
                }
            }
        }

        //--- DEBUG DOMAIN ---------------------------------------------------------------------------------------------
        if(log.isDebugEnabled()) {
            StringBuilder sb = new StringBuilder();
            sb.append(mainMethodIdentifier).append("\n");

            for(Map.Entry<String, Map<UUID, ProgramVariable>> domainMethodEntry : domain.entrySet()) {
                sb.append(domainMethodEntry.getKey()).append("\n");
                sb.append(JDFCUtils.prettyPrintMap(domainMethodEntry.getValue()));
                sb.append("\n");
            }

            JDFCUtils.logThis(sb.toString(), "ESGCreator_domain");
        }

        //--- CREATE NODES ---------------------------------------------------------------------------------------------
        NavigableMap<Integer, Map<String, Map<UUID, ESGNode>>> esgNodes = Maps.newTreeMap();
        for(SGNode sgNode : sg.getNodes().values()) {
            int sgNodeIdx = sgNode.getIndex();

            esgNodes.computeIfAbsent(sgNodeIdx, k -> Maps.newTreeMap());
            esgNodes.get(sgNodeIdx).computeIfAbsent(mainMethodIdentifier, k -> Maps.newTreeMap());
            esgNodes.get(sgNodeIdx)
                    .get(mainMethodIdentifier)
                    .put(UUID.fromString("00000000-0000-0000-0000-000000000000"), new ESGNode.ESGZeroNode(sgNodeIdx, mainMethodClassName, mainMethodName));

            for(Map.Entry<String, Map<UUID, ProgramVariable>> domainMethodEntry : domain.entrySet()) {
                for(Map.Entry<UUID, ProgramVariable> pVarEntry : domainMethodEntry.getValue().entrySet()) {
                    esgNodes.get(sgNodeIdx).computeIfAbsent(domainMethodEntry.getKey(), k -> Maps.newTreeMap());

                    if(pVarEntry.getValue() instanceof ProgramVariable.ZeroVariable) {
                        esgNodes.get(sgNodeIdx)
                                .get(mainMethodIdentifier)
                                .put(UUID.fromString("00000000-0000-0000-0000-000000000000"), new ESGNode.ESGZeroNode(sgNodeIdx, mainMethodClassName, mainMethodName));
                    } else {
                        esgNodes.get(sgNodeIdx)
                                .get(domainMethodEntry.getKey())
                                .put(pVarEntry.getKey(), new ESGNode(sgNodeIdx, pVarEntry.getValue()));
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
//            for(Map.Entry<Integer, Map<String, Map<UUID, ESGNode>>> esgSGNodeEntry : esgNodes.entrySet()) {
//                for(Map.Entry<String, Map<UUID, ESGNode>> esgNodesMethodEntry : esgSGNodeEntry.getValue().entrySet()) {
//                    for(Map.Entry<UUID, ESGNode> esgNodeEntry : esgNodesMethodEntry.getValue().entrySet()) {
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
            Map<String, Map<UUID, ProgramVariable>> reachablePVars = new HashMap<>();
            reachablePVars.computeIfAbsent(mainMethodIdentifier, k -> domain.get(mainMethodIdentifier));
            if(currSGNode instanceof SGCallNode) {
                String calledClassName = ((SGCallNode) currSGNode).getCalledClassName();
                String calledMethodName = ((SGCallNode) currSGNode).getCalledMethodName();
                String subroutineKey = ESGCreator.buildMethodIdentifier(calledClassName, calledMethodName);
                reachablePVars.computeIfAbsent(subroutineKey, k -> domain.get(subroutineKey));
            }
            if(!(currSGNode instanceof SGExitNode)) {
                reachablePVars.computeIfAbsent(currSGNodeMethodIdentifier, k -> domain.get(currSGNodeMethodIdentifier));
            }

            //--- DEBUG ACTIVE DOMAIN ----------------------------------------------------------------------------------
            if(log.isDebugEnabled()
//                    && (currSGNodeIdx == 19
//                        || currSGNodeIdx == 20
//                        || currSGNodeIdx == 32
//                        || currSGNodeIdx == 33
//                        || currSGNodeIdx == 56
//                        || currSGNodeIdx == 57)
            ) {
                StringBuilder sb = new StringBuilder();
                sb.append(mainMethodIdentifier).append("\n");

                for(Map.Entry<String, Map<UUID, ProgramVariable>> domainMethodEntry : reachablePVars.entrySet()) {
                    sb.append(domainMethodEntry.getKey()).append("\n");
                    sb.append(JDFCUtils.prettyPrintMap(domainMethodEntry.getValue()));
                    sb.append("DEFINITIONS: \n");
                    sb.append(JDFCUtils.prettyPrintSet(currSGNode.getDefinitions()));
                    sb.append("\n");
                }

                JDFCUtils.logThis(sb.toString(), String.valueOf(currSGNodeIdx));
            }

//            if(log.isDebugEnabled()) {
//                StringBuilder sb = new StringBuilder();
//                sb.append(mainMethodIdentifier).append("\n");
//
//                for(Map.Entry<String, NavigableMap<Integer, DomainVariable>> domainMethodEntry : reachablePVars.entrySet()) {
//                    sb.append(domainMethodEntry.getKey()).append("\n");
//                    sb.append(JDFCUtils.prettyPrintMap(domainMethodEntry.getValue()));
//                    sb.append("\n");
//                }
//
//                JDFCUtils.logThis(sb.toString(), String.valueOf(currSGNodeIdx));
//            }

            // --- CREATE EDGES ----------------------------------------------------------------------------------------
            // for every reachable domain variable / esg node method section
            for(Map.Entry<String, Map<UUID, ProgramVariable>> reachablePVarMethodEntry : reachablePVars.entrySet()) {
                String currMethodIdentifier = reachablePVarMethodEntry.getKey();
                Map<UUID, ProgramVariable> programVariables = reachablePVarMethodEntry.getValue();

//                if(currMethodIdentifier.equals(mainMethodIdentifier)) {
                    // main method dVars
                    for (ProgramVariable pVar : programVariables.values()) {
                        UUID pVarId = pVar.getId();

                        Collection<Integer> currSGNodeTargets = sg.getEdges().get(currSGNodeIdx);
                        for (Integer currSGNodeTargetIdx : currSGNodeTargets) {
                            if (currSGNode instanceof SGCallNode) {
                                SGCallNode currSGCallNode = (SGCallNode) currSGNode;

                                if (currSGCallNode.isCalledSGPresent()) {
                                    SGNode currSGTargetNode = sg.getNodes().get(currSGNodeTargetIdx);
                                    String calledMethodIdentifier = ESGCreator.buildMethodIdentifier(
                                            currSGTargetNode.getClassName(),
                                            currSGTargetNode.getMethodName());
                                    if (currSGTargetNode instanceof SGEntryNode) {
                                        ProgramVariable targetPVar = findValue(currSGCallNode.getPVarMap(), pVar);
                                        if (targetPVar != null) {
                                            // match variable of caller and with variable of callee
                                            esgEdges.put(currSGNodeIdx, new ESGEdge(
                                                    currSGNodeIdx,
                                                    currSGNodeTargetIdx,
                                                    currMethodIdentifier,
                                                    calledMethodIdentifier,
                                                    pVar,
                                                    targetPVar)
                                            );
                                            updateLiveVariables(liveVariableMap, targetPVar);
                                        } else {
                                            if(currSGTargetNode.getDefinitions().contains(pVar)) {
                                                // initialize variable of callee
                                                esgEdges.put(currSGNodeIdx, new ESGEdge(
                                                        currSGNodeIdx,
                                                        currSGNodeTargetIdx,
                                                        mainMethodIdentifier,
                                                        currMethodIdentifier,
                                                        ZERO,
                                                        pVar)
                                                );
                                                updateLiveVariables(liveVariableMap, pVar);
                                            }
                                        }



                                        // Todo: figure out if this is just drawn for fields
//                                        //  edge to next node
//                                        esgEdges.put(currSGNodeIdx, new ESGEdge(
//                                                currSGNodeIdx,
//                                                currSGNodeTargetIdx,
//                                                currMethodIdentifier,
//                                                currMethodIdentifier,
//                                                pVar,
//                                                pVar)
//                                        );
                                    }
                                }
//                                else {
//                                    // if SG is not present
//                                    // draw straight line for own var
//                                    esgEdges.put(currSGNodeIdx, new ESGEdge(
//                                            currSGNodeIdx,
//                                            currSGNodeTargetIdx,
//                                            currMethodIdentifier,
//                                            currMethodIdentifier,
//                                            pVar,
//                                            pVar));
//                                }
                            }
                            else {
                                if (currSGNode.getIndex() == 0) {
                                    // initialize variable of main domain
                                    // needs to be a special case, because 0 is not a call node, but needs to
                                    // the variable needs to be initialized
                                    esgEdges.put(currSGNodeIdx, new ESGEdge(
                                            currSGNodeIdx,
                                            currSGNodeTargetIdx,
                                            currMethodIdentifier,
                                            currMethodIdentifier,
                                            ZERO,
                                            pVar));
                                    updateLiveVariables(liveVariableMap, pVar);

                                }
                                else {
                                    SGNode targetNode = sg.getNodes().get(currSGNodeTargetIdx);
                                    if (!targetNode.getDefinitions().contains(pVar)
                                            && liveVariableMap.get(pVar)) {
                                        esgEdges.put(currSGNodeIdx, new ESGEdge(
                                                currSGNodeIdx,
                                                currSGNodeTargetIdx,
                                                currMethodIdentifier,
                                                currMethodIdentifier,
                                                pVar,
                                                pVar));
                                    } else {
                                        esgNodes.get(currSGNodeIdx).get(currMethodIdentifier).get(pVarId).setPossiblyNotRedefined(false);
                                    }
                                }
                            }
                        }
                    }
//                }
//                else {
//                    // subroutine dVars
//                    for (ProgramVariable pVar : programVariables.values()) {
//                        UUID pVarId = pVar.getId();
//
//                        Collection<Integer> currSGNodeTargets = sg.getEdges().get(currSGNodeIdx);
//                        for (Integer currSGNodeTargetIdx : currSGNodeTargets) {
//                            if (currSGNode instanceof SGCallNode) {
//                                SGCallNode currSGCallNode = (SGCallNode) currSGNode;
//                                SGNode currSGTargetNode = sg.getNodes().get(currSGNodeTargetIdx);
//
//                                if (currSGCallNode.isCalledSGPresent()) {
//                                    String calledMethodIdentifier = ESGCreator.buildMethodIdentifier(
//                                            currSGCallNode.getCalledClassName(),
//                                            currSGCallNode.getCalledMethodName());
//
//                                    if(currSGTargetNode instanceof SGEntryNode) {
//                                        ProgramVariable targetPVar = currSGCallNode.getPVarMap().get(pVar);
//                                        if (targetPVar != null) {
//                                            // Match found
//                                            esgEdges.put(currSGNodeIdx, new ESGEdge(
//                                                    currSGNodeIdx,
//                                                    currSGNodeTargetIdx,
//                                                    currMethodIdentifier,
//                                                    calledMethodIdentifier,
//                                                    pVar,
//                                                    targetPVar)
//                                            );
//                                        } else {
//                                            if(currMethodIdentifier.equals(calledMethodIdentifier)) {
//                                                if(currSGNode.getDefinitions().contains(pVar)) {
//                                                    // initialize variables of called method
//                                                    esgEdges.put(currSGNodeIdx, new ESGEdge(
//                                                            currSGNodeIdx,
//                                                            currSGNodeTargetIdx,
//                                                            mainMethodIdentifier,
//                                                            currMethodIdentifier,
//                                                            ZERO,
//                                                            pVar)
//                                                    );
//                                                }
//                                            }
//                                        }
//                                    } else {
//                                        // connect call and return sites
//                                        SGReturnSiteNode sgReturnSiteNode =
//                                                (SGReturnSiteNode) sg.getNodes().get(currSGNodeTargetIdx);
//                                        String sgRSNMethodIdentifier = ESGCreator.buildMethodIdentifier(
//                                                sgReturnSiteNode.getClassName(),
//                                                sgReturnSiteNode.getMethodName()
//                                        );
//
//                                        if(currMethodIdentifier.equals(sgRSNMethodIdentifier)) {
//                                            esgEdges.put(currSGNodeIdx, new ESGEdge(
//                                                    currSGNodeIdx,
//                                                    currSGNodeTargetIdx,
//                                                    sgRSNMethodIdentifier,
//                                                    sgRSNMethodIdentifier,
//                                                    pVar,
//                                                    pVar)
//                                            );
//                                        }
//                                    }
//                                } else {
//                                    // if SG is not present
//                                    // draw straight line for own var
//                                    esgEdges.put(currSGNodeIdx, new ESGEdge(
//                                            currSGNodeIdx,
//                                            currSGNodeTargetIdx,
//                                            currMethodIdentifier,
//                                            currMethodIdentifier,
//                                            pVar,
//                                            pVar));
//                                }
//                            }
//                            else if(currSGNode instanceof SGExitNode) {
//                                SGExitNode currSGExitNode = (SGExitNode) currSGNode;
//                                ProgramVariable targetPVar = currSGExitNode.getPVarMap().get(pVar);
//
//                                if (targetPVar != null) {
//                                    // Match found
//                                    esgEdges.put(currSGNodeIdx, new ESGEdge(
//                                            currSGNodeIdx,
//                                            currSGNodeTargetIdx,
//                                            currMethodIdentifier,
//                                            buildMethodIdentifier(targetPVar.getClassName(), targetPVar.getMethodName()),
//                                            pVar,
//                                            targetPVar));
//                                }
//
//                            }
//                            else {
//                                // create edge if variable is not redefined
//                                SGNode targetNode = sg.getNodes().get(currSGNodeTargetIdx);
//                                if (targetNode.getDefinitions().contains(pVar)) {
//                                    esgEdges.put(currSGNodeIdx, new ESGEdge(
//                                            currSGNodeIdx,
//                                            currSGNodeTargetIdx,
//                                            currMethodIdentifier,
//                                            currMethodIdentifier,
//                                            pVar,
//                                            pVar));
//                                } else {
//                                    esgNodes.get(currSGNodeIdx).get(currMethodIdentifier).get(pVarId).setPossiblyNotRedefined(false);
//                                }
//                            }
//                        }
//                    }
//                }

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
            ProgramVariable sourceDVar = esgEdge.getSourcePVar();
            ProgramVariable targetDVar = esgEdge.getTargetPVar();

            String debug = String.format("%d %s %s %d %s %s",
                    sgnSourceIdx, sourceMethodName, sourceDVar, sgnTargetIdx, targetMethodName, targetDVar);
            JDFCUtils.logThis(debug, "debug");

            ESGNode first = esgNodes.get(sgnSourceIdx).get(sourceMethodName).get(sourceDVar.getId());
            ESGNode second = esgNodes.get(sgnTargetIdx).get(targetMethodName).get(targetDVar.getId());
            first.getSucc().add(second);
            second.getPred().add(first);

            second.setPossiblyNotRedefined(true);
        }

        // --- DEBUG NODES ---------------------------------------------------------------------------------------------
        if(log.isDebugEnabled()) {
            StringBuilder sb = new StringBuilder();
            sb.append(mainMethodClassName).append(" ");
            sb.append(mainMethodName).append("\n");

            for(Map.Entry<Integer, Map<String, Map<UUID, ESGNode>>> esgSGNodeEntry : esgNodes.entrySet()) {
                for(Map.Entry<String, Map<UUID, ESGNode>> esgNodesMethodEntry : esgSGNodeEntry.getValue().entrySet()) {
                    for(Map.Entry<UUID, ESGNode> esgNodeEntry : esgNodesMethodEntry.getValue().entrySet()) {
                        sb.append(esgNodeEntry.getValue()).append("  ");
                    }
                }
                sb.append("\n");
            }

            JDFCUtils.logThis(sb.toString(), "exploded_nodes");
        }

//        //--- DEGUG EDGES ----------------------------------------------------------------------------------------------
//        if(log.isDebugEnabled()) {
//            StringBuilder sb = new StringBuilder();
//            sb.append(cData.getRelativePath()).append(" ");
//            sb.append(mData.buildInternalMethodName()).append("\n");
//            sb.append(JDFCUtils.prettyPrintMultimap(esgEdges));
//            JDFCUtils.logThis(sb.toString(), "exploded_edges");
//        }

        //--- CREATE ESG -----------------------------------------------------------------------------------------------
        return new ESG(sg, esgNodes, esgEdges, domain);
    }

    private static String buildMethodIdentifier(String className, String methodName) {
        return String.format("%s :: %s", className, methodName);
    }

    private static ProgramVariable findValue(Map<ProgramVariable, ProgramVariable> map, ProgramVariable pVar) {
        for(Map.Entry<ProgramVariable, ProgramVariable> entry : map.entrySet()) {
            ProgramVariable key = entry.getKey();
            if(Objects.equals(key.getLocalVarIdx(), pVar.getLocalVarIdx())
                    && Objects.equals(key.getClassName(), pVar.getClassName())
                    && Objects.equals(key.getMethodName(), pVar.getMethodName())
                    && Objects.equals(key.getName(), pVar.getName())
                    && Objects.equals(key.getDescriptor(), pVar.getDescriptor())
                    && Objects.equals(key.getIsField(), pVar.getIsField())){
                return entry.getValue();
            }
        }

        return null;
    }

    private static ProgramVariable findKey(Map<ProgramVariable, ?> map, ProgramVariable pVar) {
        for(Map.Entry<ProgramVariable, ?> entry : map.entrySet()) {
            ProgramVariable key = entry.getKey();
            if(Objects.equals(key.getLocalVarIdx(), pVar.getLocalVarIdx())
                    && Objects.equals(key.getClassName(), pVar.getClassName())
                    && Objects.equals(key.getMethodName(), pVar.getMethodName())
                    && Objects.equals(key.getName(), pVar.getName())
                    && Objects.equals(key.getDescriptor(), pVar.getDescriptor())
                    && Objects.equals(key.getIsField(), pVar.getIsField())){
                return entry.getKey();
            }
        }

        return null;
    }

    private static void updateLiveVariables(Map<ProgramVariable, Boolean> liveVariableMap, ProgramVariable pVar) {
        ProgramVariable match = findKey(liveVariableMap, pVar);
        if(match != null) {
            liveVariableMap.put(match, false);
        }
        liveVariableMap.put(pVar, true);
    }
}
