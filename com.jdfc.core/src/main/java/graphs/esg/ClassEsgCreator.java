package graphs.esg;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;
import data.ClassData;
import data.MethodData;
import data.ProgramVariable;
import graphs.esg.nodes.ESGNode;
import graphs.sg.SG;
import graphs.sg.nodes.SGCallNode;
import graphs.sg.nodes.SGEntryNode;
import graphs.sg.nodes.SGNode;
import graphs.sg.nodes.SGReturnSiteNode;
import lombok.extern.slf4j.Slf4j;
import utils.ASMHelper;
import utils.JDFCUtils;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class ClassEsgCreator {

    public void createESGsForClass(ClassData classData) {
        String className = classData.getClassMetaData().getClassNodeName();
        for(MethodData methodData : classData.getMethodDataFromStore().values()) {
            MethodEsgCreator methodESGCreator = new MethodEsgCreator(className, methodData);
            ESG esg = methodESGCreator.createESG();
            if (methodData.buildInternalMethodName().contains("addOne") || methodData.buildInternalMethodName().contains("defineA")) {
                System.out.println();
            }
            methodData.setEsg(esg);
        }
    }

    private static class MethodEsgCreator {

        private final ASMHelper asmHelper = new ASMHelper();

        private final ProgramVariable ZERO;

        private final String className;

        private final MethodData methodData;

        private final SG superGraph;

        private final String mainMethodName;

        private final String mainMethodId;

//        private final List<String> callSequence;

        private Map<ProgramVariable, Boolean> liveVariables;

        private final NavigableMap<Integer, Map<ProgramVariable, ProgramVariable>> callerToCalleeDefMap;

        private final NavigableMap<Integer, Map<ProgramVariable, ProgramVariable>> calleeToCallerDefMap;

        public MethodEsgCreator(String className, MethodData methodData) {
            this.className = className;
            this.methodData = methodData;
            this.superGraph = methodData.getSg();
            this.mainMethodName = methodData.buildInternalMethodName();
            this.mainMethodId = this.buildMethodIdentifier(this.className, mainMethodName);
//            this.callSequence = new ArrayList<>();
//            this.callSequence.add("ZERO");
            this.liveVariables = new HashMap<>();
            this.callerToCalleeDefMap = new TreeMap<>();
            this.calleeToCallerDefMap = new TreeMap<>();
            this.ZERO = new ProgramVariable.ZeroVariable(this.className, mainMethodName);
        }

        public ESG createESG() {
            //--- Create map containing all definitions per method -----------------------------------------------------

            // One entry is ZERO containing only Zero
            Map<String, Map<UUID, ProgramVariable>> methodDefinitionsMap = createMethodDefinitonsMap();
            debugMethodDefinitionsMap(ImmutableMap.copyOf(methodDefinitionsMap));

            // <ESGNodeIdx <CallSequenceIdx, MethodIdentifier>>
            Map<Integer, ESGNode> esgNodes = new TreeMap<>();
            for (SGNode iNode : superGraph.getNodes().values()) {
                // Create call sequence map for every esg node
                int i = iNode.getIndex();
                if (i == 0) {
                    // for first node only put ZERO
                    // IMPORTANT: CallSequenceIdx of ZERO = 0
                    //            CallSequenceIdx of main method = 1
                    esgNodes.put(0, new ESGNode());
                    esgNodes.get(0).getCallSeqIdxMethodIdMap().put(0, "ZERO");
                    esgNodes.get(0).getCallSeqIdxVarMap().put(0, new TreeMap<>());
                    esgNodes.get(0).getCallSeqIdxVarMap().get(0).put(this.ZERO.getId(), this.ZERO);
                    esgNodes.get(0).getCallSeqIdxLiveVarMap().put(0, new TreeMap<>());
                    esgNodes.get(0).getCallSeqIdxLiveVarMap().get(0).put(this.ZERO.getId(), true);
                    esgNodes.get(0).getCallSeqIdxPosNotReMap().put(0, new TreeMap<>());
                    esgNodes.get(0).getCallSeqIdxPosNotReMap().get(0).put(this.ZERO.getId(), false);
                }

                // IMPORTANT: SGNodeIdx + 1 = ESGNodeIdx
                int esgIdx = i + 1;
                ESGNode esgNode = new ESGNode(esgIdx);
                ESGNode pred = esgNodes.get(esgIdx - 1);
                Map<Integer, String> predMap = new HashMap<>(pred.getCallSeqIdxMethodIdMap());

                if (iNode instanceof SGEntryNode) {
                    // Add a new mId to predMap
                    int callSeqIdx = predMap.keySet().size();
                    String mId = this.buildMethodIdentifier(iNode.getClassName(), iNode.getMethodName());
                    predMap.put(callSeqIdx, mId);
                }

                if (iNode instanceof SGReturnSiteNode) {
                    // Remove an mId from predMap
                    int callSeqIdx = predMap.keySet().size() - 1;
                    predMap.remove(callSeqIdx);
                }

                esgNode.setCallSeqIdxMethodIdMap(predMap);

                // Add variables to esgNode based on call sequence
                Map<Integer, Map<UUID, ProgramVariable>> callSeqIdxVarMap = new TreeMap<>();
                for (Map.Entry<Integer, String> callSeqIdxMethodEntry : esgNode.getCallSeqIdxMethodIdMap().entrySet()) {
                    int callSeqIdx = callSeqIdxMethodEntry.getKey();
                    String mId = callSeqIdxMethodEntry.getValue();
                    callSeqIdxVarMap.put(callSeqIdx, new HashMap<>(methodDefinitionsMap.get(mId)));
                }
                esgNode.setCallSeqIdxVarMap(callSeqIdxVarMap);

                // Add liveliness of variables to esgNode
                Map<Integer, Map<UUID, Boolean>> callSeqLiveVarMap = new TreeMap<>();
                for (Map.Entry<Integer, Map<UUID, ProgramVariable>> callSeqIdxVarEntry : esgNode.getCallSeqIdxVarMap().entrySet()) {
                    Map<UUID, Boolean> liveVarMap = new HashMap<>();
                    for (Map.Entry<UUID, ProgramVariable> pVarEntry : callSeqIdxVarEntry.getValue().entrySet()) {
                        liveVarMap.put(pVarEntry.getKey(), false);
                    }
                    callSeqLiveVarMap.put(callSeqIdxVarEntry.getKey(), liveVarMap);
                }
                esgNode.setCallSeqIdxLiveVarMap(callSeqLiveVarMap);

                // Add possiblyNotRedefined to esgNode based on variables
                Map<Integer, Map<UUID, Boolean>> callSeqPosNotReMap = new TreeMap<>();
                for (Map.Entry<Integer, Map<UUID, ProgramVariable>> callSeqIdxVarEntry : esgNode.getCallSeqIdxVarMap().entrySet()) {
                    Map<UUID, Boolean> posNotReMap = new HashMap<>();
                    for (Map.Entry<UUID, ProgramVariable> pVarEntry : callSeqIdxVarEntry.getValue().entrySet()) {
                        posNotReMap.put(pVarEntry.getKey(), false);
                    }
                    callSeqPosNotReMap.put(callSeqIdxVarEntry.getKey(), posNotReMap);
                }
                esgNode.setCallSeqIdxPosNotReMap(callSeqPosNotReMap);
                esgNodes.put(esgIdx, esgNode);
            }

            if (mainMethodName.contains("defineA")) {
                System.out.println();
            }

            //--- Create esgNodeActiveVarMap for every SGNode
            // <ESGNodeIdx <CallSequenceIdx <PVarUUID, PVar>>>
//            Map<Integer, Map<Integer, Map<UUID, ProgramVariable>>> esgNodeActiveVarMap = new HashMap<>();
//            for (Map.Entry<Integer, Map<Integer, String>> esgNodeEntry : esgNodeActiveMethodMap.entrySet()) {
//                int esgIdx = esgNodeEntry.getKey();
//                Map<Integer, String> activeMethods = esgNodeEntry.getValue();
//
//                esgNodeActiveVarMap.put(esgIdx, new HashMap<>());
//                for (Map.Entry<Integer, String> activeMethod : activeMethods.entrySet()) {
//                    int callSequenceIdx = activeMethod.getKey();
//                    String mId = activeMethod.getValue();
//                    esgNodeActiveVarMap.get(esgIdx).put(callSequenceIdx, methodDefinitionsMap.get(mId));
//                }
//            }


//            //--- CREATE NODES ---------------------------------------------------------------------------------------------
//            NavigableMap<Integer, Map<String, Map<UUID, ESGNode>>> esgNodes = createESGNodes(ImmutableMap.copyOf(esgNodeActiveVarMap));
//
//            // --- DEBUG NODES ---------------------------------------------------------------------------------------------
//            debugNodes(esgNodes);

            //--- CREATE EDGES ---------------------------------------------------------------------------------------------
            Multimap<Integer, ESGEdge> esgEdges = ArrayListMultimap.create();
            for(Map.Entry<Integer, ESGNode> esgNodeEntry : esgNodes.entrySet()) {
                if (esgNodeEntry.getKey() + 1 == esgNodes.size()) {
                    continue;
                }
                ESGNode esgCurr = esgNodeEntry.getValue();
                ESGNode esgNext = esgNodes.get(esgNodeEntry.getKey() + 1);
                createEdges(esgEdges, esgCurr, esgNext);
            }

            //--- DEGUG EDGES ----------------------------------------------------------------------------------------------
            if(log.isDebugEnabled()) {
                StringBuilder sb = new StringBuilder();
                sb.append(className).append(" ");
                sb.append(mainMethodName).append("\n");
                sb.append(JDFCUtils.prettyPrintMultimap(esgEdges));
                JDFCUtils.logThis(sb.toString(), "exploded_edges");
            }

            //--- PRED & SUCC
//            for(ESGEdge esgEdge : esgEdges.values()) {
//                int sgnSourceIdx = esgEdge.getSgnSourceIdx();
//                int sgnTargetIdx = esgEdge.getSgnTargetIdx();
//                String sourceMethodName = esgEdge.getSourceMethodId();
//                String targetMethodName = esgEdge.getTargetMethodId();
//                ProgramVariable sourceDVar = esgEdge.getSourceVar();
//                ProgramVariable targetDVar = esgEdge.getTargetVar();
//
//                String debug = String.format("%d %s %s %d %s %s",
//                        sgnSourceIdx, sourceMethodName, sourceDVar, sgnTargetIdx, targetMethodName, targetDVar);
//                JDFCUtils.logThis(debug, "debug");
//
//                ESGNode first = esgNodes.get(sgnSourceIdx).get(sourceMethodName).get(sourceDVar.getId());
//                ESGNode second = esgNodes.get(sgnTargetIdx).get(targetMethodName).get(targetDVar.getId());
//                first.getSucc().add(second);
//                second.getPred().add(first);
//
//                second.setPossiblyNotRedefined(true);
//            }

            // --- DEBUG NODES ---------------------------------------------------------------------------------------------
//        if(log.isDebugEnabled()) {
//            StringBuilder sb = new StringBuilder();
//            sb.append(MAIN_METHOD_CLASS_NAME).append(" ");
//            sb.append(MAIN_METHOD_NAME).append("\n");
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

//        //--- DEGUG EDGES ----------------------------------------------------------------------------------------------
//        if(log.isDebugEnabled()) {
//            StringBuilder sb = new StringBuilder();
//            sb.append(cData.getRelativePath()).append(" ");
//            sb.append(mData.buildInternalMethodName()).append("\n");
//            sb.append(JDFCUtils.prettyPrintMultimap(esgEdges));
//            JDFCUtils.logThis(sb.toString(), "exploded_edges");
//        }

            //--- CREATE ESG -----------------------------------------------------------------------------------------------
            // TODO
            return new ESG(superGraph, null, esgEdges, methodDefinitionsMap, callerToCalleeDefMap, calleeToCallerDefMap);
        }

        private ESGEdge createEdge(int srcIdx,
                                   int srcCallSeqIdx,
                                   UUID srcVarId,
                                   int trgtIdx,
                                   int trgtCallSeqIdx,
                                   UUID trgtVarId) {
            return new ESGEdge(srcIdx, trgtIdx, srcCallSeqIdx, trgtCallSeqIdx, srcVarId, trgtVarId);
        }

        private int getCallSeqIdxByMId(Map<Integer, String> callSeqMidMap, String mId) {
            Set<Map.Entry<Integer, String>> calls = callSeqMidMap.entrySet().stream()
                    .filter(entry -> entry.getValue().equals(mId))
                    .collect(Collectors.toSet());
            Optional<Map.Entry<Integer, String>> callSeqIdxOpt = calls.stream().reduce((a, b) -> {
                if (a.getKey() < b.getKey()) {
                    return b;
                } else {
                    return a;
                }
            });

            if (callSeqIdxOpt.isPresent()) {
                Map.Entry<Integer, String> callSeqIdxEntry = callSeqIdxOpt.get();
                return callSeqIdxEntry.getKey();
            } else {
                throw new IllegalArgumentException("getCallSeqIdxByMId");
            }
        }

        // todo: Get the current esg node
        // todo: Get the next esg node
        private void createEdges(Multimap<Integer, ESGEdge> esgEdges, ESGNode esgCurr, ESGNode esgNext) {
            // TODO: imagine the positioning of esg nodes like this
            //       0: esg
            //       1: esg = 0: sg
            //       2: esg = 1: sg
            //       ...

            int esgIdxCurr = esgCurr.getIdx();
            int esgIdxNext = esgNext.getIdx();

            // Methods
            // todo: get current esg nodes method call sequence
            Map<Integer, String> esgMidCurr = esgCurr.getCallSeqIdxMethodIdMap();
            // todo: get next esg nodes method call sequence
            Map<Integer, String> esgMidNext = esgNext.getCallSeqIdxMethodIdMap();

            // Vars
            // todo: get current esg nodes variables per method
            Map<Integer, Map<UUID, ProgramVariable>> esgVarCurr = esgCurr.getCallSeqIdxVarMap();
            // todo: get next esg nodes variables per method
            Map<Integer, Map<UUID, ProgramVariable>> esgVarNext = esgNext.getCallSeqIdxVarMap();

            // Liveness
            // todo: get current esg nodes live variables indicator per method
            Map<Integer, Map<UUID, Boolean>> esgLiveVarCurr = esgCurr.getCallSeqIdxLiveVarMap();
            // todo: get next esg nodes live variables indicator per method
            Map<Integer, Map<UUID, Boolean>> esgLiveVarNext = esgNext.getCallSeqIdxLiveVarMap();

            // todo: get next sg node
            //      - connect live variables from the current esg node to the next esg node
            //      - check the next sg node for new definition and connect them to ZERO
            SGNode sgNext = superGraph.getNodes().get(esgIdxCurr);
            // todo: get call sequence idx of method of current sg node
            String sgMid = this.buildMethodIdentifier(sgNext.getClassName(), sgNext.getMethodName());
            Map.Entry<Integer, String> sgCallEntry = esgMidCurr.entrySet().stream()
                    .filter(entry -> entry.getValue().equals(sgMid))
                    .reduce((a, b) -> {
                        if (a.getKey() > b.getKey()) {
                            return a;
                        } else {
                            return b;
                        }
                    })
                    .orElse(null);

            if (sgNext instanceof SGCallNode) {
                return;
            }

            // todo: The callSeqIndex is important to know which method is currently active.
            //          This also means that for other methods we only want to connect the variables that need to be
            //          connected even if they are in a outer scope
            // todo: For the active (inner) and inactive (outer) scope we still only want to connect live variables.
            // todo: inner scope test: callSeqIdx fits AND var is alive
            // todo: outer scope test: var is alive AND (var is local, live OR (method is static AND (var is this OR field))
            // todo: connect the live variables from esgCurr to esgNext

            // For all methods in esgCurr
            for (Map.Entry<Integer, Map<UUID, ProgramVariable>> callEntry : esgVarCurr.entrySet()) {
                int callIdx = callEntry.getKey();
                // For all variables in method in esgCurr
                for (Map.Entry<UUID, ProgramVariable> varEntry : callEntry.getValue().entrySet()) {
                    UUID varId = varEntry.getKey();

                    boolean isAlive = esgCurr.getCallSeqIdxLiveVarMap().get(callIdx).get(varId);
                    if (isAlive) {
                        // todo: create edge
                        ESGEdge newEdge = this.createEdge(
                                esgIdxCurr, callIdx, varId, esgIdxNext, callIdx, varId
                        );
                        esgEdges.put(esgIdxCurr, newEdge);
                        // todo: set variable live in next esg
                        esgNext.getCallSeqIdxLiveVarMap().get(callIdx).put(varId, true);
                    }

                    boolean isNewDefinition = sgCallEntry != null
                            && sgCallEntry.getKey() == callIdx
                            && sgNext.getDefinitions().contains(varEntry.getValue());
                    if (isNewDefinition) {
                        // todo: draw edge ZERO -> new definition
                        ESGEdge newEdge = this.createEdge(
                                esgIdxCurr, 0, this.ZERO.getId(), esgIdxNext, callIdx, varId
                        );
                        esgEdges.put(esgIdxCurr, newEdge);
                        // todo: set variable live in next esg
                        esgNext.getCallSeqIdxLiveVarMap().get(callIdx).put(varId, true);
                    }

                    // todo: Test if live variables and new definitions are connected correctly
                }

            }
//            for(Map.Entry<Integer, Map<UUID, ProgramVariable>> activeDomainMethodSection : callSeqIdxActiveVarMap.entrySet()) {
//                String currVariableMethodIdentifier = activeDomainMethodSection.getKey();
//                Map<UUID, ProgramVariable> programVariables = activeDomainMethodSection.getValue();
//
//                if (mainMethodId.contains("defineA") && currSGNodeIdx == 4) {
//                    System.out.println();
//                }
//
//                for (ProgramVariable pVar : programVariables.values()) {
//                    Collection<Integer> currSGNodeTargets = superGraph.getEdges().get(currSGNodeIdx);
//                    for (Integer currSGNodeTargetIdx : currSGNodeTargets) {
//                        SGNode sgTargetNode = superGraph.getNodes().get(currSGNodeTargetIdx);
//                        if(Objects.equals(pVar, ZERO)) {
//                            // Special case ZERO
//                            ESGEdge edge = handleZero(currSGNode, sgTargetNode, pVar);
//                            esgEdges.put(currSGNodeIdx, edge);
//                        } else if ((Objects.equals(pVar.getName(), "this") || pVar.getIsField())) {
//                            if (!asmHelper.isStatic(currSGNode.getMethodAccess())
//                                && Objects.equals(currSGNodeMethodIdentifier, currVariableMethodIdentifier)) {
//                                Set<ESGEdge> edges = handleGlobal(currSGNode, sgTargetNode, pVar);
//                                esgEdges.putAll(currSGNodeIdx, edges);
//                            } else {
//                                // Todo: connect outer this of call site
//                                // 1. find call site
//                                // 2. connect outer this
//                                System.out.println();
//                            }
//                        } else {
//                            // Local variables
//                            if(Objects.equals(currSGNodeMethodIdentifier, currVariableMethodIdentifier)) {
//                                Set<ESGEdge> edges = handleLocal(currSGNode, sgTargetNode, pVar);
//                                esgEdges.putAll(currSGNodeIdx, edges);
//                            } else {
//                                // TODO:
//                                if (!(currSGNode instanceof SGCallNode) && asmHelper.isCallByValue(pVar.getDescriptor())) {
//                                    // we basically want to create an edge that connects the variable in its own method
//                                    String sgNodeMId = this.buildMethodIdentifier(pVar.getClassName(), pVar.getMethodName());
//                                    String sgTargetNodeMId = this.buildMethodIdentifier(pVar.getClassName(), pVar.getMethodName());
//                                    ESGEdge edge = new ESGEdge(
//                                            currSGNodeIdx,
//                                            currSGNodeTargetIdx,
//                                            sgNodeMId,
//                                            sgTargetNodeMId,
//                                            pVar,
//                                            pVar
//                                    );
//                                    esgEdges.put(currSGNodeIdx, edge);
//                                }
//                            }
//                        }
//                    }
//                }
//            }
        }

//        public Map<String, Map<UUID, ProgramVariable>> updateActiveScope(
//                Map<String, Map<UUID, ProgramVariable>> domain,
//                SGNode currSGNode
//        ) {
//            Map<String, Map<UUID, ProgramVariable>> newActiveScope = new HashMap<>();
//            if(currSGNode instanceof SGCallNode) {
//                SGCallNode sgCallNode = (SGCallNode) currSGNode;
//                String calledSGNodeMethodId = this.buildMethodIdentifier(
//                        sgCallNode.getCalledClassName(),
//                        sgCallNode.getCalledMethodName()
//                );
//                callSequence.add(calledSGNodeMethodId);
//            }
//
//            if(currSGNode instanceof SGReturnSiteNode) {
//                callSequence.remove(callSequence.size() - 1);
//            }
//
//            for(String methodIdentifier : callSequence) {
//                newActiveScope.computeIfAbsent(methodIdentifier, k -> domain.get(methodIdentifier));
//            }
//
//            return newActiveScope;
//        }
//
//        public void debugActiveScope(
//                Map<String, Map<UUID, ProgramVariable>> activeScope,
//                Integer currSGNodeIdx
//        ){
//            if(log.isDebugEnabled()) {
//                StringBuilder sb = new StringBuilder();
//                sb.append("\nCALL SEQUENCE: ").append(callSequence).append("\n");
//                sb.append("\nACTIVE SCOPE: ").append(mainMethodId).append("\n");
//
//                for(Map.Entry<String, Map<UUID, ProgramVariable>> domainMethodEntry : activeScope.entrySet()) {
//                    sb.append(domainMethodEntry.getKey()).append("\n");
//                    sb.append(JDFCUtils.prettyPrintMap(domainMethodEntry.getValue()));
//                }
//
//                JDFCUtils.logThis(sb.toString(), String.valueOf(currSGNodeIdx));
//            }
//        }
//
//        public Set<ESGEdge> handleGlobal(SGNode sgNode, SGNode sgTargetNode, ProgramVariable pVar) {
//            Set<ESGEdge> edges = new HashSet<>();
//            String sgNodeMId = this.buildMethodIdentifier(sgNode.getClassName(), sgNode.getMethodName());
//            String sgTargetNodeMId = this.buildMethodIdentifier(sgTargetNode.getClassName(), sgTargetNode.getMethodName());
//            if(sgNode instanceof SGCallNode) {
//                ProgramVariable match = ((SGCallNode) sgNode).getDefinitionsMap().get(pVar);
//                if(match != null) {
//                    // if match exists connect to subroutine
//                    callerToCalleeDefMap.computeIfAbsent(sgNode.getIndex(), k -> new HashMap<>());
//                    callerToCalleeDefMap.get(sgNode.getIndex()).put(pVar, match);
//                    calleeToCallerDefMap.computeIfAbsent(((SGCallNode) sgNode).getExitNodeIdx(), k -> new HashMap<>());
//                    calleeToCallerDefMap.get(((SGCallNode) sgNode).getExitNodeIdx()).put(match, pVar);
//                    edges.add(new ESGEdge(
//                            sgNode.getIndex(),
//                            sgTargetNode.getIndex(),
//                            sgNodeMId,
//                            sgTargetNodeMId,
//                            pVar,
//                            match
//                    ));
//                } else {
//                    // field that has no match
//                    // "this" when a static method is invoked
//                    String pVarMId = this.buildMethodIdentifier(pVar.getClassName(), pVar.getMethodName());
//                    if (pVarMId.equals(mainMethodId)) {
//                        // similar to ZERO
//                        edges.add(new ESGEdge(
//                                sgNode.getIndex(),
//                                sgTargetNode.getIndex(),
//                                sgNodeMId,
//                                sgNodeMId,
//                                pVar,
//                                pVar
//                        ));
//                    }
//                }
//            } else if(sgNode instanceof SGEntryNode) {
//                if(!liveVariables.get(pVar) || sgNode.getIndex() == 0) {
//                    edges.add(new ESGEdge(
//                            sgNode.getIndex(),
//                            sgTargetNode.getIndex(),
//                            mainMethodId,
//                            sgTargetNodeMId,
//                            ZERO,
//                            pVar
//                    ));
//                } else {
//                    edges.add(new ESGEdge(
//                            sgNode.getIndex(),
//                            sgTargetNode.getIndex(),
//                            sgNodeMId,
//                            sgTargetNodeMId,
//                            pVar,
//                            pVar
//                    ));
//                }
//            } else if (sgNode instanceof SGExitNode) {
//                if(calleeToCallerDefMap.get(sgNode.getIndex()) != null) {
//                    ProgramVariable m = calleeToCallerDefMap.get(sgNode.getIndex()).get(pVar);
//                    if(m != null) {
//                        edges.add(new ESGEdge(
//                                sgNode.getIndex(),
//                                sgTargetNode.getIndex(),
//                                sgNodeMId,
//                                sgTargetNodeMId,
//                                pVar,
//                                m
//                        ));
//                    }
//                }
//            } else {
//                ProgramVariable newDef = findMatch(sgNode.getDefinitions(), pVar);
//                if(newDef == null) {
//                    edges.add(new ESGEdge(
//                            sgNode.getIndex(),
//                            sgTargetNode.getIndex(),
//                            sgNodeMId,
//                            sgTargetNodeMId,
//                            pVar,
//                            pVar
//                    ));
//                } else {
//                    edges.add(new ESGEdge(
//                            sgNode.getIndex(),
//                            sgTargetNode.getIndex(),
//                            sgNodeMId,
//                            sgTargetNodeMId,
//                            newDef,
//                            newDef
//                    ));
//                }
//            }
//
//            return edges;
//        }
//
//        private Set<ESGEdge> handleLocal(SGNode sgNode, SGNode sgTargetNode, ProgramVariable pVar) {
//            Set<ESGEdge> edges = new HashSet<>();
//            String sgNodeMId = this.buildMethodIdentifier(sgNode.getClassName(), sgNode.getMethodName());
//            String sgTargetNodeMId = this.buildMethodIdentifier(sgTargetNode.getClassName(), sgTargetNode.getMethodName());
//            if(sgNode instanceof SGCallNode) {
//                ProgramVariable match = ((SGCallNode) sgNode).getDefinitionsMap().get(pVar);
//                if(match != null) {
//                    // match caller var to callee var
//                    callerToCalleeDefMap.computeIfAbsent(sgNode.getIndex(), k -> new HashMap<>());
//                    callerToCalleeDefMap.get(sgNode.getIndex()).put(pVar, match);
//                    calleeToCallerDefMap.computeIfAbsent(((SGCallNode) sgNode).getExitNodeIdx(), k -> new HashMap<>());
//                    calleeToCallerDefMap.get(((SGCallNode) sgNode).getExitNodeIdx()).put(match, pVar);
//                    edges.add(new ESGEdge(
//                            sgNode.getIndex(),
//                            sgTargetNode.getIndex(),
//                            sgNodeMId,
//                            sgTargetNodeMId,
//                            pVar,
//                            match
//                    ));
//                }
//            } else if(sgNode instanceof SGEntryNode) {
//                if(!liveVariables.get(pVar) || sgNode.getIndex() == 0) {
//                    if(sgNode.getDefinitions().contains(pVar)) {
//                        // add definition if present
//                        edges.add(new ESGEdge(
//                                sgNode.getIndex(),
//                                sgTargetNode.getIndex(),
//                                mainMethodId,
//                                sgTargetNodeMId,
//                                ZERO,
//                                pVar
//                        ));
//                    }
//                } else { // matched variable already is defined
//                    edges.add(new ESGEdge(
//                            sgNode.getIndex(),
//                            sgTargetNode.getIndex(),
//                            sgNodeMId,
//                            sgTargetNodeMId,
//                            pVar,
//                            pVar
//                    ));
//                }
//            } else if (sgNode instanceof SGExitNode) {
//                if(!asmHelper.isCallByValue(pVar.getDescriptor())) {
//                    // if var is object
//                    Collection<ProgramVariable> matches = ((SGExitNode) sgNode).getDefinitionsMap().get(pVar);
//                    for (ProgramVariable match : matches) {
//                        // match back do definition on caller site
//                        edges.add(new ESGEdge(
//                                sgNode.getIndex(),
//                                sgTargetNode.getIndex(),
//                                sgNodeMId,
//                                sgTargetNodeMId,
//                                pVar,
//                                match
//                        ));
//                    }
//                }
//            } else {
//                ProgramVariable newDef = findMatch(sgTargetNode.getDefinitions(), pVar);
//                if(newDef == null) {
//                    if(liveVariables.get(pVar)) {
//                        edges.add(new ESGEdge(
//                                sgNode.getIndex(),
//                                sgTargetNode.getIndex(),
//                                sgNodeMId,
//                                sgTargetNodeMId,
//                                pVar,
//                                pVar
//                        ));
//                    }
//                    else if (sgNode.getDefinitions().contains(pVar)) {
//                        edges.add(new ESGEdge(
//                                sgNode.getIndex(),
//                                sgTargetNode.getIndex(),
//                                sgNodeMId,
//                                sgTargetNodeMId,
//                                pVar,
//                                pVar
//                        ));
//                    }
//                } else {
//                    if(liveVariables.get(pVar)) { // connect already existing variable definition
//                        edges.add(new ESGEdge(
//                                sgNode.getIndex(),
//                                sgTargetNode.getIndex(),
//                                sgNodeMId,
//                                sgTargetNodeMId,
//                                pVar,
//                                pVar
//                        ));
//                    } else { // add new variable definition
//                        edges.add(new ESGEdge(
//                                sgNode.getIndex(),
//                                sgTargetNode.getIndex(),
//                                mainMethodId,
//                                sgTargetNodeMId,
//                                ZERO,
//                                newDef
//                        ));
//                    }
//                }
//            }
//
//            return edges;
//        }
//
//        private ESGEdge handleLiveOuterScopeLocals(SGNode sgNode, SGNode sgTargetNode, ProgramVariable pVar) {
//            String pVarMId = this.buildMethodIdentifier(pVar.getClassName(), pVar.getMethodName());
//            return new ESGEdge(
//                    sgNode.getIndex(),
//                    sgTargetNode.getIndex(),
//                    pVarMId,
//                    pVarMId,
//                    pVar,
//                    pVar
//            );
//        }
//
//        private ESGEdge handleZero(SGNode sgNode, SGNode sgTargetNode, ProgramVariable pVar) {
//            return new ESGEdge(
//                    sgNode.getIndex(),
//                    sgTargetNode.getIndex(),
//                    mainMethodId,
//                    mainMethodId,
//                    pVar,
//                    pVar
//            );
//        }
//
//        private ProgramVariable findLastActiveDef(SGExitNode sgNode, ProgramVariable pVar) {
//            // TODO: Can there be multiple last active defs present?
//            return null;
//        }
//
//        private Set<ESGEdge> handleOtherScope(SGNode sgNode, SGNode sgTargetNode, ProgramVariable pVar) {
//            Set<ESGEdge> edges = new HashSet<>();
//            if(sgNode instanceof SGCallNode) {
//                SGCallNode sgCallNode = (SGCallNode) sgNode;
//                String calledMethodId = this.buildMethodIdentifier(sgCallNode.getCalledClassName(), sgCallNode.getCalledMethodName());
//                String pVarMId = this.buildMethodIdentifier(pVar.buildClassNodeName(), pVar.getMethodName());
//                if(Objects.equals(calledMethodId, pVarMId)) {
//                    if(!sgCallNode.getDefinitionsMap().containsValue(pVar)
//                            && sgTargetNode.getDefinitions().contains(pVar)) {
//                        edges.add(new ESGEdge(
//                                sgNode.getIndex(),
//                                sgTargetNode.getIndex(),
//                                mainMethodId,
//                                calledMethodId,
//                                ZERO,
//                                pVar
//                        ));
//                    }
//                } else {
//                    if(liveVariables.get(pVar)) {
//                        edges.add(new ESGEdge(
//                                sgNode.getIndex(),
//                                sgTargetNode.getIndex(),
//                                pVarMId,
//                                pVarMId,
//                                pVar,
//                                pVar
//                        ));
//                    }
//                }
//            }
//            else if (sgNode instanceof SGEntryNode) {
//                if(liveVariables.get(pVar)) {
//                    String pVarMId = this.buildMethodIdentifier(pVar.buildClassNodeName(), pVar.getMethodName());
//                    edges.add(new ESGEdge(
//                            sgNode.getIndex(),
//                            sgTargetNode.getIndex(),
//                            pVarMId,
//                            pVarMId,
//                            pVar,
//                            pVar
//                    ));
//                }
//            }
//            else if (sgNode instanceof SGExitNode) {
//                if(liveVariables.get(pVar)) {
//                    String pVarMId = this.buildMethodIdentifier(pVar.buildClassNodeName(), pVar.getMethodName());
//                    edges.add(new ESGEdge(
//                            sgNode.getIndex(),
//                            sgTargetNode.getIndex(),
//                            pVarMId,
//                            pVarMId,
//                            pVar,
//                            pVar
//                    ));
//                }
//            }
//            else if (sgNode instanceof SGReturnSiteNode) {
//                String pVarMId = this.buildMethodIdentifier(pVar.buildClassNodeName(), pVar.getMethodName());
//                edges.add(new ESGEdge(
//                        sgNode.getIndex(),
//                        sgTargetNode.getIndex(),
//                        pVarMId,
//                        pVarMId,
//                        pVar,
//                        pVar
//                ));
//            }
//            else {
//                if(liveVariables.get(pVar)) {
//                    String pVarMId = this.buildMethodIdentifier(pVar.buildClassNodeName(), pVar.getMethodName());
//                    edges.add(new ESGEdge(
//                            sgNode.getIndex(),
//                            sgTargetNode.getIndex(),
//                            pVarMId,
//                            pVarMId,
//                            pVar,
//                            pVar
//                    ));
//                }
//            }
//            return edges;
//        }

//        private Map<ProgramVariable, Boolean> updateLiveVariables(Map<String, Map<UUID, ProgramVariable>> activeScope, SGNode sgNode) {
//            Map<ProgramVariable, Boolean> updated = new HashMap<>();
//            String sgNodeMId = this.buildMethodIdentifier(sgNode.getClassName(), sgNode.getMethodName());
//
//            for(Map.Entry<String, Map<UUID, ProgramVariable>> mEntry : activeScope.entrySet()) {
//                for(ProgramVariable p : activeScope.get(mEntry.getKey()).values()) {
//                    if(callSequence.contains(mEntry.getKey())) {
//                        if(Objects.equals(mEntry.getKey(), sgNodeMId)) {
//                            if(sgNode.getCfgReachOut().contains(p)) {
//                                updated.put(p, true);
//                            } else {
//                                updated.put(p, false);
//                            }
//                        } else {
//                            updated.put(p, liveVariables.getOrDefault(p, false));
//                        }
//                    } else {
//                        updated.put(p, false);
//                    }
//                }
//            }
//
//            return updated;
//        }

        public void debugLiveVariables(int currSgIdx) {
            if(log.isDebugEnabled()) {
                StringBuilder sb = new StringBuilder();
                sb.append("\nLIVE VARIABLES: ").append(mainMethodId).append("\n");

                for(Map.Entry<ProgramVariable, Boolean> liveEntry : liveVariables.entrySet()) {
                    sb.append(liveEntry.getKey()).append(" => ").append(liveEntry.getValue()).append("\n");
                }

                JDFCUtils.logThis(sb.toString(), String.valueOf(currSgIdx));
            }
        }


        public Map<String, Map<UUID, ProgramVariable>> createMethodDefinitonsMap() {
            Map<String, Map<UUID, ProgramVariable>> domain = new HashMap<>();
            domain.put("ZERO", new HashMap<>());
            domain.get("ZERO").put(ZERO.getId(), ZERO);
            liveVariables.put(ZERO, false);

            for(SGNode sgNode : superGraph.getNodes().values()) {
                String sgNodeMethodIdentifier = this.buildMethodIdentifier(sgNode.getClassName(), sgNode.getMethodName());
                domain.computeIfAbsent(sgNodeMethodIdentifier, k -> new HashMap<>());
                for(ProgramVariable def : sgNode.getDefinitions()) {
                    domain.get(sgNodeMethodIdentifier).put(def.getId(), def);
                    liveVariables.put(def, false);
                }
            }
            return domain;
        }

        public void debugMethodDefinitionsMap(Map<String, Map<UUID, ProgramVariable>> domain) {
            if(log.isDebugEnabled()) {
                StringBuilder sb = new StringBuilder();
                sb.append(mainMethodId).append("\n");

                for(Map.Entry<String, Map<UUID, ProgramVariable>> domainMethodEntry : domain.entrySet()) {
                    sb.append(domainMethodEntry.getKey()).append("\n");
                    sb.append(JDFCUtils.prettyPrintMap(domainMethodEntry.getValue()));
                    sb.append("\n");
                }

                JDFCUtils.logThis(sb.toString(), "ESGCreator_domain");
            }
        }

//        public NavigableMap<Integer, Map<Integer, Map<UUID, ESGNode>>> createESGNodes(Map<String, Map<UUID, ProgramVariable>> domain) {
//            NavigableMap<Integer, Map<String, Map<UUID, ESGNode>>> esgNodes = Maps.newTreeMap();
//
//            // create nodes for SG nodes
//            for(SGNode sgNode : superGraph.getNodes().values()) {
//                int sgNodeIdx = sgNode.getIndex();
//
//                esgNodes.computeIfAbsent(sgNodeIdx, k -> Maps.newTreeMap());
//                esgNodes.get(sgNodeIdx).computeIfAbsent(mainMethodId, k -> Maps.newTreeMap());
//                esgNodes.get(sgNodeIdx)
//                        .get(mainMethodId)
//                        .put(UUID.fromString("00000000-0000-0000-0000-000000000000"), new ESGNode.ESGZeroNode(sgNodeIdx, className, mainMethodName));
//
//                for(Map.Entry<String, Map<UUID, ProgramVariable>> domainMethodEntry : domain.entrySet()) {
//                    for(Map.Entry<UUID, ProgramVariable> pVarEntry : domainMethodEntry.getValue().entrySet()) {
//                        esgNodes.get(sgNodeIdx).computeIfAbsent(domainMethodEntry.getKey(), k -> Maps.newTreeMap());
//
//                        if(pVarEntry.getValue() instanceof ProgramVariable.ZeroVariable) {
//                            esgNodes.get(sgNodeIdx)
//                                    .get(mainMethodId)
//                                    .put(UUID.fromString("00000000-0000-0000-0000-000000000000"), new ESGNode.ESGZeroNode(sgNodeIdx, className, mainMethodName));
//                        } else {
//                            esgNodes.get(sgNodeIdx)
//                                    .get(domainMethodEntry.getKey())
//                                    .put(pVarEntry.getKey(), new ESGNode(sgNodeIdx, pVarEntry.getValue()));
//                        }
//                    }
//                }
//            }
//
//            return esgNodes;
//        }

        public void debugNodes(
                NavigableMap<Integer, Map<String, Map<UUID, ESGNode>>> esgNodes) {
            if(log.isDebugEnabled()) {
                StringBuilder sb = new StringBuilder();
                sb.append(className).append(" ");
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
        }

        private String buildMethodIdentifier(String className, String methodName) {
            return String.format("%s :: %s", className.replace("/", "."), methodName);
        }

        private ProgramVariable findMatch(Set<ProgramVariable> set, ProgramVariable pVar) {
            for (ProgramVariable p : set) {
                if (Objects.equals(p.getLocalVarIdx(), pVar.getLocalVarIdx())
                        && Objects.equals(p.getClassName(), pVar.getClassName())
                        && Objects.equals(p.getMethodName(), pVar.getMethodName())
                        && Objects.equals(p.getName(), pVar.getName())
                        && Objects.equals(p.getDescriptor(), pVar.getDescriptor())
                        && Objects.equals(p.getIsField(), pVar.getIsField())) {
                    return p;
                }
            }

            return null;
        }
    }
}
