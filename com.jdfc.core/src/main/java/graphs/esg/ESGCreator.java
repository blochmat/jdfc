package graphs.esg;

import algos.TabulationAlgorithm;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import data.ClassExecutionData;
import data.MethodData;
import data.ProgramVariable;
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
        Set<ProgramVariable> liveVariableMap = new HashSet<>();


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
                liveVariableMap.add(ZERO);
            }
            for(ProgramVariable def : sgNode.getDefinitions()) {
                if(!(Objects.equals(def.getName(), "this") && !Objects.equals(sgNodeMethodIdentifier, mainMethodIdentifier))) {
                    domain.get(sgNodeMethodIdentifier).put(def.getId(), def);
//                    liveVariableMap.put(def, false);
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

        // create nodes for sg nodes
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

            String reach = String.format("%s %d\n%s",
                    currSGNodeMethodIdentifier,
                    currSGNodeIdx,
                    JDFCUtils.prettyPrintSet(currSGNode.getCfgReach()));
            JDFCUtils.logThis(reach, "ESGCreator_reach");

            String reachOut = String.format("%s %d\n%s",
                    currSGNodeMethodIdentifier,
                    currSGNodeIdx,
                    JDFCUtils.prettyPrintSet(currSGNode.getCfgReachOut()));
            JDFCUtils.logThis(reachOut, "ESGCreator_reachOut");

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
                String currVariableMethodIdentifier = reachablePVarMethodEntry.getKey();
                Map<UUID, ProgramVariable> programVariables = reachablePVarMethodEntry.getValue();

                // main method dVars
                for (ProgramVariable pVar : programVariables.values()) {
                    Collection<Integer> currSGNodeTargets = sg.getEdges().get(currSGNodeIdx);
                    for (Integer currSGNodeTargetIdx : currSGNodeTargets) {

                        // create edges to artificial starting nodes
                        if(currSGNodeIdx == 0) {
                            if(Objects.equals(pVar, ZERO)) {
                                // draw edge for ZERO
                                esgEdges.put(currSGNodeIdx, new ESGEdge(
                                        currSGNodeIdx,
                                        currSGNodeTargetIdx,
                                        currVariableMethodIdentifier,
                                        currVariableMethodIdentifier,
                                        pVar,
                                        pVar)
                                );
                            }
                            if(currSGNode.getDefinitions().contains(pVar)) {
                                // initialize new definition
                                // kill old definition
                                esgEdges.put(currSGNodeIdx, new ESGEdge(
                                        currSGNodeIdx,
                                        currSGNodeTargetIdx,
                                        mainMethodIdentifier,
                                        currVariableMethodIdentifier,
                                        ZERO,
                                        pVar));

                                // Update live variable
                                liveVariableMap.add(pVar);

                                String debug = String.format("%s, A %d %s %s",
                                        mainMethodName,
                                        currSGNodeIdx,
                                        pVar,
                                        JDFCUtils.prettyPrintSet(liveVariableMap)
                                );
                                JDFCUtils.logThis(debug, "test");
                            }
                        }
                        else {
                            if(Objects.equals(pVar, ZERO)) {
                                // draw edge for ZERO
                                esgEdges.put(currSGNodeIdx, new ESGEdge(
                                        currSGNodeIdx,
                                        currSGNodeTargetIdx,
                                        currVariableMethodIdentifier,
                                        currVariableMethodIdentifier,
                                        pVar,
                                        pVar)
                                );
                            }

                            SGNode currSGTargetNode = sg.getNodes().get(currSGNodeTargetIdx);
                            if(!currSGNode.getCfgReachOut().contains(pVar)
                                    && !currSGTargetNode.getDefinitions().contains(pVar)
                                    && !Objects.equals(pVar, ZERO)
                                    && liveVariableMap.contains(pVar)) {
                                // draw edge for alive variables from outer scope
                                esgEdges.put(currSGNodeIdx, new ESGEdge(
                                        currSGNodeIdx,
                                        currSGNodeTargetIdx,
                                        currVariableMethodIdentifier,
                                        currVariableMethodIdentifier,
                                        pVar,
                                        pVar)
                                );
                            }

                            JDFCUtils.logThis(currSGNodeMethodName + " " + currSGNodeIdx + "\n" + pVar, "ddddddd");
                            JDFCUtils.logThis(JDFCUtils.prettyPrintSet(liveVariableMap), "ddddddd");
                            JDFCUtils.logThis(String.valueOf(!liveVariableMap.contains(pVar)), "ddddddd");
                            JDFCUtils.logThis(JDFCUtils.prettyPrintSet(currSGTargetNode.getCfgReachOut()), "ddddddd");
                            JDFCUtils.logThis(String.valueOf(currSGTargetNode.getCfgReachOut().contains(pVar)), "ddddddd");
                            JDFCUtils.logThis("\n", "ddddddd");

                            if(currSGTargetNode.getDefinitions().contains(pVar)) {
                                // initialize new definition
                                esgEdges.put(currSGNodeIdx, new ESGEdge(
                                        currSGNodeIdx,
                                        currSGNodeTargetIdx,
                                        mainMethodIdentifier,
                                        currVariableMethodIdentifier,
                                        ZERO,
                                        pVar));
                                liveVariableMap.add(pVar);

                                // kill old definition
                                liveVariableMap.remove(findMatch(ImmutableSet.copyOf(liveVariableMap), pVar));

                                String debug = String.format("%s, B %d %s %s",
                                        mainMethodName,
                                        currSGNodeIdx,
                                        pVar,
                                        JDFCUtils.prettyPrintSet(liveVariableMap)
                                );
                                JDFCUtils.logThis(debug, "test");
                            }

                            if(currSGNode.getCfgReachOut().contains(pVar)) {
                                esgEdges.put(currSGNodeIdx, new ESGEdge(
                                        currSGNodeIdx,
                                        currSGNodeTargetIdx,
                                        currVariableMethodIdentifier,
                                        currVariableMethodIdentifier,
                                        pVar,
                                        pVar)
                                );
                                String debug = String.format("%s C %d %s %s",
                                        mainMethodName,
                                        currSGNodeIdx,
                                        pVar,
                                        JDFCUtils.prettyPrintSet(liveVariableMap)
                                );
                                JDFCUtils.logThis(debug, "test");
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

    private static ProgramVariable findMatch(Set<ProgramVariable> set, ProgramVariable pVar) {
        for(ProgramVariable p : set) {
            if(Objects.equals(p.getLocalVarIdx(), pVar.getLocalVarIdx())
                    && Objects.equals(p.getClassName(), pVar.getClassName())
                    && Objects.equals(p.getMethodName(), pVar.getMethodName())
                    && Objects.equals(p.getName(), pVar.getName())
                    && Objects.equals(p.getDescriptor(), pVar.getDescriptor())
                    && Objects.equals(p.getIsField(), pVar.getIsField())){
                return p;
            }
        }
        return null;
    }
}
