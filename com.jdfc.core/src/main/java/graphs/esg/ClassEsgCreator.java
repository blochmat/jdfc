package graphs.esg;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;
import data.ClassData;
import data.MethodData;
import data.ProgramVariable;
import data.ProjectData;
import graphs.esg.nodes.ESGNode;
import graphs.sg.SG;
import graphs.sg.nodes.*;
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
            this.liveVariables = new HashMap<>();
            this.callerToCalleeDefMap = new TreeMap<>();
            this.calleeToCallerDefMap = new TreeMap<>();
            this.ZERO = new ProgramVariable.ZeroVariable(this.className, mainMethodName);
        }

        public ESG createESG() {
            // Add ZERO "method" and var entry
            Map<String, Map<UUID, ProgramVariable>> methodDefinitionsMap = createMethodDefinitonsMap();
            debugMethodDefinitionsMap(ImmutableMap.copyOf(methodDefinitionsMap));

            // <ESGNodeIdx <CallSequenceIdx, MethodIdentifier>>
            NavigableMap<Integer, ESGNode> esgNodes = new TreeMap<>();
            for (SGNode iNode : superGraph.getNodes().values()) {
                // Create call sequence map for every esg node
                int i = iNode.getIndex();

                // ZERO
                // IMPORTANT: CallSequenceIdx of ZERO = 0
                if (i == 0) {
                    esgNodes.put(0, new ESGNode());
                    esgNodes.get(0).getCallSeqIdxMethodIdMap().put(0, "ZERO");
                    esgNodes.get(0).getCallSeqIdxVarMap().put(0, new TreeMap<>());
                    esgNodes.get(0).getCallSeqIdxVarMap().get(0).put(this.ZERO.getId(), this.ZERO);
                    esgNodes.get(0).getCallSeqIdxLiveVarMap().put(0, new TreeMap<>());
                    esgNodes.get(0).getCallSeqIdxLiveVarMap().get(0).put(this.ZERO.getId(), true);
                    esgNodes.get(0).getCallSeqIdxPosNotReMap().put(0, new TreeMap<>());
                    esgNodes.get(0).getCallSeqIdxPosNotReMap().get(0).put(this.ZERO.getId(), false);
                }

                int esgIdx = i + 1;
                ESGNode esgNode = new ESGNode(esgIdx);
                ESGNode pred = esgNodes.get(esgIdx - 1);
                Map<Integer, String> predMap = new HashMap<>(pred.getCallSeqIdxMethodIdMap());

                // main method
                // IMPORTANT: CallSequenceIdx of main method = 1
                if (i == 0) {
                    int callSeqIdx = predMap.keySet().size();
                    String mId = this.buildMethodIdentifier(iNode.getClassName(), iNode.getMethodName());
                    predMap.put(callSeqIdx, mId);
                }

                if (iNode instanceof SGCallNode) {
                    // Add a new mId to predMap
                    SGCallNode iCallNode = (SGCallNode) iNode;
                    int callSeqIdx = predMap.keySet().size();
                    String mId = this.buildMethodIdentifier(iCallNode.getCalledClassName(), iCallNode.getCalledMethodName());
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
            if(log.isDebugEnabled() && !mainMethodName.contains("defineAStatic") && mainMethodName.contains("defineA")) {
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
            return new ESG(superGraph, esgNodes, esgEdges, methodDefinitionsMap, callerToCalleeDefMap, calleeToCallerDefMap);
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

            if(!mainMethodName.contains("defineAStatic") && mainMethodName.contains("defineA")) {
                System.out.println();
            }
            // TODO: imagine the positioning of esg nodes like this
            //       0: esg
            //       1: esg = 0: sg
            //       2: esg = 1: sg
            //       ...

            // src
            int currEsgIdx = esgCurr.getIdx();
            Map<Integer, String> srcMethodNamesMap = esgCurr.getCallSeqIdxMethodIdMap();
            Map<Integer, Map<UUID, ProgramVariable>> srcVarsMaps = esgCurr.getCallSeqIdxVarMap();
            Map<Integer, Map<UUID, Boolean>> srcLiveVarsMaps = esgCurr.getCallSeqIdxLiveVarMap();

            // sgNode
            SGNode sgNode = superGraph.getNodes().get(currEsgIdx);

            // target
            int nextEsgIdx = esgNext.getIdx();
            Map<Integer, String> trgtMethodNamesMap = esgNext.getCallSeqIdxMethodIdMap();
            Map<Integer, Map<UUID, ProgramVariable>> trgtVarsMaps = esgNext.getCallSeqIdxVarMap();
            Map<Integer, Map<UUID, Boolean>> trgtLiveVarsMaps = esgNext.getCallSeqIdxLiveVarMap();

            // 1 Determine callIdx and domain of active method
            String sgMid = this.buildMethodIdentifier(sgNode.getClassName(), sgNode.getMethodName());
            Map.Entry<Integer, String> activeMethodEntry = trgtMethodNamesMap.entrySet().stream()
                    .filter(entry -> entry.getValue().equals(sgMid))
                    .reduce((a, b) -> {
                        if (a.getKey() > b.getKey()) {
                            return a;
                        } else {
                            return b;
                        }
                    })
                    .orElse(null);
            if(activeMethodEntry == null) {
                throw new RuntimeException("activeMethodEntry is null");
            }

            final int activeMethodCallIdx = activeMethodEntry.getKey();
            Map<UUID, ProgramVariable> activeMethodVarsMap = trgtVarsMaps.get(activeMethodEntry.getKey());

            // 2 Iterate over all source vars and draw edges to target vars
            for (Map.Entry<Integer, Map<UUID, ProgramVariable>> srcCallIdxVarsEntry : srcVarsMaps.entrySet()) {
                // src: for every method
                for (Map.Entry<UUID, ProgramVariable> srcVarEntry : srcCallIdxVarsEntry.getValue().entrySet()) {
                    // src: for every variable
                    final int srcCallIdx = srcCallIdxVarsEntry.getKey();
                    final UUID srcVarId = srcVarEntry.getKey();
                    final ProgramVariable srcVar = srcVarEntry.getValue();

                    if (srcVarId.equals(this.ZERO.getId())) {
                        ESGEdge zeroEdge = new ESGEdge(currEsgIdx, nextEsgIdx, srcCallIdx, srcCallIdx, srcVarId, srcVarId);
                        esgEdges.put(currEsgIdx, zeroEdge);
                        trgtLiveVarsMaps.get(srcCallIdx).put(srcVarId, true);

                        for (Map.Entry<UUID, ProgramVariable> trgtVarEntry : activeMethodVarsMap.entrySet()) {
                            final UUID trgtVarId = trgtVarEntry.getKey();
                            final ProgramVariable trgtVar = trgtVarEntry.getValue();
                            final boolean isAlive = srcLiveVarsMaps.get(activeMethodCallIdx) != null
                                    && srcLiveVarsMaps.get(activeMethodCallIdx).get(trgtVarId);
                            if (sgNode.getDefinitions().contains(trgtVar) && !isAlive) {
                                // TODO: check for assignments, returns and matches
                                this.addEdge(esgEdges, trgtLiveVarsMaps, currEsgIdx, nextEsgIdx,
                                        srcCallIdx, activeMethodCallIdx, srcVarId, trgtVarId);
                            }
                        }
                    } else if (srcVar.getName().equals("this")) {
                        final boolean isAlive = srcLiveVarsMaps.get(srcCallIdx).get(srcVarId);
                        if (isAlive) {
                            if (sgNode instanceof SGCallNode) {
                                SGCallNode sgCallNode = (SGCallNode) sgNode;
                                boolean hasNoMatch = sgCallNode.getDefinitionsMap()
                                        .values()
                                        .stream()
                                        .noneMatch(var -> var.equals(srcVar));
                                if (hasNoMatch) {
                                    // Connect to next self
                                    this.addEdge(esgEdges, trgtLiveVarsMaps, currEsgIdx, nextEsgIdx,
                                            srcCallIdx, srcCallIdx, srcVarId, srcVarId);
                                } else {
                                    // Connect to all matches
                                    for (ProgramVariable match : sgCallNode.getDefinitionsMap().keySet()) {
                                        ProgramVariable matchedSrc = sgCallNode.getDefinitionsMap().get(match);
                                        if (matchedSrc.equals(srcVar)) {
                                            this.addEdge(esgEdges, trgtLiveVarsMaps, currEsgIdx, nextEsgIdx,
                                                    activeMethodCallIdx, activeMethodCallIdx + 1, srcVarId, match.getId());
                                            break;
                                        }
                                    }
                                }
                            } else if (sgNode instanceof SGEntryNode) {
                                // Connect to next self
                                this.addEdge(esgEdges, trgtLiveVarsMaps, currEsgIdx, nextEsgIdx,
                                        srcCallIdx, srcCallIdx, srcVarId, srcVarId);
                            } else if (sgNode instanceof SGExitNode) {
                                SGExitNode sgExitNode = (SGExitNode) sgNode;
                                ProgramVariable match = sgExitNode.getDefinitionsMap().get(srcVar);
                                if (match != null) {
                                    this.addEdge(esgEdges, trgtLiveVarsMaps, currEsgIdx, nextEsgIdx,
                                            srcCallIdx, srcCallIdx -1, srcVarId, match.getId());
                                } else {
                                    this.addEdge(esgEdges, trgtLiveVarsMaps, currEsgIdx, nextEsgIdx,
                                            srcCallIdx, srcCallIdx, srcVarId, srcVarId);
                                }
                            } else if (sgNode instanceof SGReturnSiteNode) {
                                this.addEdge(esgEdges, trgtLiveVarsMaps, currEsgIdx, nextEsgIdx,
                                        srcCallIdx, srcCallIdx, srcVarId, srcVarId);
                            } else {
                                if (activeMethodCallIdx == srcCallIdx) {
                                    // Inner scope
                                    this.addEdge(esgEdges, trgtLiveVarsMaps, currEsgIdx, nextEsgIdx,
                                            srcCallIdx, srcCallIdx, srcVarId, srcVarId);
                                } else {
                                    // Outer scope
                                    if (this.asmHelper.isStatic(sgNode.getMethodAccess())) {
                                        this.addEdge(esgEdges, trgtLiveVarsMaps, currEsgIdx, nextEsgIdx,
                                                srcCallIdx, srcCallIdx, srcVarId, srcVarId);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        private void addEdge(Multimap<Integer, ESGEdge> esgEdges,
                             Map<Integer, Map<UUID, Boolean>> trgLiveVarsMap,
                             int currEsgIdx,
                             int nextEsgIdx,
                             int srcCallIdx,
                             int trgtCallIdx,
                             UUID srcVarId,
                             UUID trgtVarId) {
            ESGEdge newEdge = new ESGEdge(currEsgIdx, nextEsgIdx, srcCallIdx, trgtCallIdx, srcVarId, trgtVarId);
            esgEdges.put(currEsgIdx, newEdge);
            trgLiveVarsMap.get(trgtCallIdx).put(trgtVarId, true);
        }

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
