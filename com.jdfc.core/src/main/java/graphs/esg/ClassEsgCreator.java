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

@Slf4j
public class ClassEsgCreator {

    public void createESGsForClass(ClassData classData) {
        String className = classData.getClassMetaData().getClassNodeName();
        for(MethodData methodData : classData.getMethodDataFromStore().values()) {
            MethodEsgCreator methodESGCreator = new MethodEsgCreator(className, methodData);
            ESG esg = methodESGCreator.createESG();
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

        private Map<ProgramVariable, Boolean> liveVariables;

        private final NavigableMap<Integer, Map<UUID, UUID>> callerToCalleeDefMap;

        private final NavigableMap<Integer, Map<UUID, UUID>> calleeToCallerDefMap;

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
                    esgNodes.get(0).getCallIdxMethodIdMap().put(0, "ZERO");
                    esgNodes.get(0).getCallIdxVarMaps().put(0, new TreeMap<>());
                    esgNodes.get(0).getCallIdxVarMaps().get(0).put(this.ZERO.getId(), this.ZERO);
                    esgNodes.get(0).getCallIdxLiveVarMap().put(0, new TreeMap<>());
                    esgNodes.get(0).getCallIdxLiveVarMap().get(0).put(this.ZERO.getId(), true);
                    esgNodes.get(0).getCallIdxPosNotReMap().put(0, new TreeMap<>());
                    esgNodes.get(0).getCallIdxPosNotReMap().get(0).put(this.ZERO.getId(), false);
                }

                int esgIdx = i + 1;
                ESGNode esgNode = new ESGNode(esgIdx);
                ESGNode pred = esgNodes.get(esgIdx - 1);
                Map<Integer, String> predMap = new HashMap<>(pred.getCallIdxMethodIdMap());

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

                esgNode.setCallIdxMethodIdMap(predMap);

                // Add variables to esgNode based on call sequence
                Map<Integer, Map<UUID, ProgramVariable>> callSeqIdxVarMap = new TreeMap<>();
                for (Map.Entry<Integer, String> callSeqIdxMethodEntry : esgNode.getCallIdxMethodIdMap().entrySet()) {
                    int callSeqIdx = callSeqIdxMethodEntry.getKey();
                    String mId = callSeqIdxMethodEntry.getValue();
                    callSeqIdxVarMap.put(callSeqIdx, new HashMap<>(methodDefinitionsMap.get(mId)));
                }
                esgNode.setCallIdxVarMaps(callSeqIdxVarMap);

                // Add liveliness of variables to esgNode
                Map<Integer, Map<UUID, Boolean>> callSeqLiveVarMap = new TreeMap<>();
                for (Map.Entry<Integer, Map<UUID, ProgramVariable>> callSeqIdxVarEntry : esgNode.getCallIdxVarMaps().entrySet()) {
                    Map<UUID, Boolean> liveVarMap = new HashMap<>();
                    for (Map.Entry<UUID, ProgramVariable> pVarEntry : callSeqIdxVarEntry.getValue().entrySet()) {
                        liveVarMap.put(pVarEntry.getKey(), false);
                    }
                    callSeqLiveVarMap.put(callSeqIdxVarEntry.getKey(), liveVarMap);
                }
                esgNode.setCallIdxLiveVarMap(callSeqLiveVarMap);

                // Add possiblyNotRedefined to esgNode based on variables
                Map<Integer, Map<UUID, Boolean>> callSeqPosNotReMap = new TreeMap<>();
                for (Map.Entry<Integer, Map<UUID, ProgramVariable>> callSeqIdxVarEntry : esgNode.getCallIdxVarMaps().entrySet()) {
                    Map<UUID, Boolean> posNotReMap = new HashMap<>();
                    for (Map.Entry<UUID, ProgramVariable> pVarEntry : callSeqIdxVarEntry.getValue().entrySet()) {
                        posNotReMap.put(pVarEntry.getKey(), false);
                    }
                    callSeqPosNotReMap.put(callSeqIdxVarEntry.getKey(), posNotReMap);
                }
                esgNode.setCallIdxPosNotReMap(callSeqPosNotReMap);
                esgNodes.put(esgIdx, esgNode);
            }

            //--- CREATE EDGES ---------------------------------------------------------------------------------------------
            Multimap<Integer, ESGEdge> esgEdges = ArrayListMultimap.create();
            for(Map.Entry<Integer, ESGNode> esgNodeEntry : esgNodes.entrySet()) {
                if (esgNodeEntry.getKey() + 1 == esgNodes.size()) {
                    continue;
                }
                ESGNode esgCurr = esgNodeEntry.getValue();
                SGNode sgCurr = superGraph.getNodes().get(esgCurr.getIdx());
                createEdges(esgNodes, esgEdges, esgCurr, sgCurr);
            }

            //--- DEGUG EDGES ----------------------------------------------------------------------------------------------
            if(log.isDebugEnabled() && !mainMethodName.contains("defineAStatic") && mainMethodName.contains("defineA")) {
                String sb = className + " " +
                        mainMethodName + "\n" +
                        JDFCUtils.prettyPrintMultimap(esgEdges);
                JDFCUtils.logThis(sb, "exploded_edges");
            }

            //--- CREATE ESG -----------------------------------------------------------------------------------------------
            return new ESG(superGraph, esgNodes, esgEdges, methodDefinitionsMap, callerToCalleeDefMap, calleeToCallerDefMap);
        }

        private void createEdges(Map<Integer, ESGNode> esgNodes, Multimap<Integer, ESGEdge> esgEdges, ESGNode esgCurr, SGNode sgCurr) {
            if(log.isDebugEnabled()
                    && !mainMethodName.contains("defineAStatic")
                    && mainMethodName.contains("defineA")
                    && esgCurr.getIdx() == 26) {
//                System.out.println();
            }
            // curr
            int currEsgIdx = esgCurr.getIdx();
            Map<Integer, String> currMethodNamesMap = esgCurr.getCallIdxMethodIdMap();
            Map<Integer, Map<UUID, ProgramVariable>> currVarsMap = esgCurr.getCallIdxVarMaps();
            Map<Integer, Map<UUID, Boolean>> currLiveVarsMaps = esgCurr.getCallIdxLiveVarMap();

            // next
            int nextEsgIdx = currEsgIdx + 1;
            ESGNode esgNext = esgNodes.get(nextEsgIdx);
            Map<Integer, String> esgNextMethodNamesMap = esgNext.getCallIdxMethodIdMap();
            Map<Integer, Map<UUID, ProgramVariable>> esgNextVarsMaps = esgNext.getCallIdxVarMaps();
            Map<Integer, Map<UUID, Boolean>> esgNextLiveVarsMaps = esgNext.getCallIdxLiveVarMap();

            // 1 Get callIdx and vars of active method
            String sgMid = this.buildMethodIdentifier(sgCurr.getClassName(), sgCurr.getMethodName());
            Map.Entry<Integer, String> activeMethodEntry = esgNextMethodNamesMap.entrySet().stream()
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
            Map<UUID, ProgramVariable> activeMethodVarsMap = esgNextVarsMaps.get(activeMethodEntry.getKey());

            // 2 Iterate over all source vars
            for (Map.Entry<Integer, Map<UUID, ProgramVariable>> srcCallIdxVarsEntry : currVarsMap.entrySet()) {
                // src: for every method
                for (Map.Entry<UUID, ProgramVariable> srcVarEntry : srcCallIdxVarsEntry.getValue().entrySet()) {
                    // src: for every variable
                    final int srcCallIdx = srcCallIdxVarsEntry.getKey();
                    final UUID srcVarId = srcVarEntry.getKey();
                    final ProgramVariable srcVar = srcVarEntry.getValue();

                    // 3 Handle special case ZERO
                    if (srcVarId.equals(this.ZERO.getId())) {
                        this.addEdge(esgEdges, esgNextLiveVarsMaps, currEsgIdx, nextEsgIdx,
                                srcCallIdx, srcCallIdx, srcVarId, srcVarId);

                        // Connect new definitions
                        for (Map.Entry<UUID, ProgramVariable> trgtVarEntry : activeMethodVarsMap.entrySet()) {
                            final UUID trgtVarId = trgtVarEntry.getKey();
                            final ProgramVariable trgtVar = trgtVarEntry.getValue();
                            final boolean isAlive = currLiveVarsMaps.get(activeMethodCallIdx) != null
                                    && currLiveVarsMaps.get(activeMethodCallIdx).get(trgtVarId);
                            if (sgCurr.getDefinitions().contains(trgtVar) && !isAlive) {
                                this.addEdge(esgEdges, esgNextLiveVarsMaps, currEsgIdx, nextEsgIdx,
                                        srcCallIdx, activeMethodCallIdx, srcVarId, trgtVarId);
                            }
                        }
                    } else {
                        // 4 Handle variables
                        final boolean isAlive = currLiveVarsMaps.get(srcCallIdx).get(srcVarId);
                        final boolean isInActiveMethod = activeMethodCallIdx == srcCallIdx;
                        final boolean isPrimitive = asmHelper.isPrimitiveTypeVar(srcVar);

                        // Skip
                        if (!isAlive) {
                            continue;
                        }

                        // Draw "Keep-Alive"-edges for non-active methods
                        if (!isInActiveMethod) {
//                            this.addEdge(esgEdges, esgNextLiveVarsMaps, currEsgIdx, nextEsgIdx,
//                                    srcCallIdx, srcCallIdx, srcVarId, srcVarId);
                            continue;
                        }

                        // Skip if var is redefined
                        if (sgCurr.containsRedefinitionOf(srcVar)) {
                            continue;
                        }

                        // 5 Iterate over all sg edge targets
                        final Collection<Integer> trgtEsgIndices = superGraph.getEdges().get(sgCurr.getIndex());
                        for (Integer trgtEsgIdx : trgtEsgIndices) {
                            Map<Integer, Map<UUID, Boolean>> trgtEsgLiveVarsMaps = esgNodes.get(trgtEsgIdx).getCallIdxLiveVarMap();
                            // 6 Handle "this"
                            if (srcVar.getName().equals("this")) {
                                if (sgCurr instanceof SGCallNode) {
                                    SGCallNode sgCallNode = (SGCallNode) sgCurr;
                                    boolean isCallReturnSiteEdge = trgtEsgIdx - currEsgIdx > 1;
                                    if (isCallReturnSiteEdge) {
                                        boolean hasNoMatch = sgCallNode.getDefinitionsMap()
                                                .values()
                                                .stream()
                                                .noneMatch(var -> var.equals(srcVar));
                                        if (hasNoMatch) {
                                            this.addEdge(esgEdges, trgtEsgLiveVarsMaps, currEsgIdx, trgtEsgIdx,
                                                    srcCallIdx, srcCallIdx, srcVarId, srcVarId);
                                        }
                                    } else {
                                        // Connect to all matches
                                        for (ProgramVariable match : sgCallNode.getDefinitionsMap().keySet()) {
                                            ProgramVariable matchedSrc = sgCallNode.getDefinitionsMap().get(match);
                                            if (matchedSrc.equals(srcVar)) {
                                                this.addEdge(esgEdges, trgtEsgLiveVarsMaps, currEsgIdx, trgtEsgIdx,
                                                        srcCallIdx, srcCallIdx + 1, srcVarId, match.getId());
                                                esgCurr.getDefinitionMaps().computeIfAbsent(srcCallIdx, k -> new HashMap<>());
                                                esgCurr.getDefinitionMaps().get(srcCallIdx).put(match.getId(), srcVarId);

                                                Multimap<UUID, UUID> matchesMap = ProjectData.getInstance().getMatchesMap();
                                                Collection<UUID> matches = matchesMap.get(match.getId());
                                                if (matches != null && !matches.contains(srcVarId)) {
                                                    matchesMap.put(match.getId(), srcVarId);
                                                }
                                            }
                                        }
                                    }
                                    esgNext.getDefinitionMaps().putAll(esgCurr.getDefinitionMaps());
                                } else if (sgCurr instanceof SGEntryNode) {
                                    // Connect to next self
                                    this.addEdge(esgEdges, trgtEsgLiveVarsMaps, currEsgIdx, trgtEsgIdx,
                                            srcCallIdx, srcCallIdx, srcVarId, srcVarId);
                                    esgNext.getDefinitionMaps().putAll(esgCurr.getDefinitionMaps());
                                } else if (sgCurr instanceof SGExitNode) {
                                    SGExitNode sgExitNode = (SGExitNode) sgCurr;
                                    ProgramVariable match = sgExitNode.getDefinitionsMap().get(srcVar);
                                    if (match != null) {
                                        this.addEdge(esgEdges, trgtEsgLiveVarsMaps, currEsgIdx, trgtEsgIdx,
                                                srcCallIdx, srcCallIdx - 1, srcVarId, match.getId());
                                    }

                                    esgCurr.getDefinitionMaps().remove(srcCallIdx);
                                    esgNext.getDefinitionMaps().putAll(esgCurr.getDefinitionMaps());
                                } else if (sgCurr instanceof SGReturnSiteNode) {
                                    this.addEdge(esgEdges, trgtEsgLiveVarsMaps, currEsgIdx, trgtEsgIdx,
                                            srcCallIdx, srcCallIdx, srcVarId, srcVarId);
                                    esgNext.getDefinitionMaps().putAll(esgCurr.getDefinitionMaps());
                                } else {
                                    this.addEdge(esgEdges, trgtEsgLiveVarsMaps, currEsgIdx, trgtEsgIdx,
                                            srcCallIdx, srcCallIdx, srcVarId, srcVarId);
                                    esgNext.getDefinitionMaps().putAll(esgCurr.getDefinitionMaps());
                                }
                            }
                            // 7 Handle primitive type variables
                            else if (isPrimitive) {
                                if (sgCurr instanceof SGCallNode) {
                                    SGCallNode sgCallNode = (SGCallNode) sgCurr;
//                                    boolean hasNoMatch = sgCallNode.getDefinitionsMap()
//                                            .values()
//                                            .stream()
//                                            .noneMatch(var -> var.equals(srcVar));
                                    boolean isCallReturnSiteEdge = trgtEsgIdx - currEsgIdx > 1;
                                    if (isCallReturnSiteEdge) {
                                        this.addEdge(esgEdges, trgtEsgLiveVarsMaps, currEsgIdx, trgtEsgIdx,
                                                srcCallIdx, srcCallIdx, srcVarId, srcVarId);
                                    } else {
                                        // Connect to all matches
                                        for (ProgramVariable match : sgCallNode.getDefinitionsMap().keySet()) {
                                            ProgramVariable matchedSrc = sgCallNode.getDefinitionsMap().get(match);
                                            if (matchedSrc.equals(srcVar)) {
                                                this.addEdge(esgEdges, trgtEsgLiveVarsMaps, currEsgIdx, trgtEsgIdx,
                                                        srcCallIdx, srcCallIdx + 1, srcVarId, match.getId());
                                                esgCurr.getDefinitionMaps().computeIfAbsent(srcCallIdx, k -> new HashMap<>());
                                                esgCurr.getDefinitionMaps().get(srcCallIdx).put(match.getId(), srcVarId);

                                                Multimap<UUID, UUID> matchesMap = ProjectData.getInstance().getMatchesMap();
                                                Collection<UUID> matches = matchesMap.get(match.getId());
                                                if (matches != null && !matches.contains(srcVarId)) {
                                                    matchesMap.put(match.getId(), srcVarId);
                                                }
                                            }
                                        }
                                    }
                                    esgNext.getDefinitionMaps().putAll(esgCurr.getDefinitionMaps());
                                } else if (sgCurr instanceof SGEntryNode) {
                                    this.addEdge(esgEdges, trgtEsgLiveVarsMaps, currEsgIdx, trgtEsgIdx,
                                            srcCallIdx, srcCallIdx, srcVarId, srcVarId);
                                    esgNext.getDefinitionMaps().putAll(esgCurr.getDefinitionMaps());
                                } else if (sgCurr instanceof SGExitNode) {
                                    esgCurr.getDefinitionMaps().remove(srcCallIdx);
                                    esgNext.getDefinitionMaps().putAll(esgCurr.getDefinitionMaps());
                                } else if (sgCurr instanceof SGReturnSiteNode) {
                                    this.addEdge(esgEdges, trgtEsgLiveVarsMaps, currEsgIdx, trgtEsgIdx,
                                            srcCallIdx, srcCallIdx, srcVarId, srcVarId);
                                    esgNext.getDefinitionMaps().putAll(esgCurr.getDefinitionMaps());
                                } else {
                                    this.addEdge(esgEdges, trgtEsgLiveVarsMaps, currEsgIdx, trgtEsgIdx,
                                            srcCallIdx, srcCallIdx, srcVarId, srcVarId);
                                    esgNext.getDefinitionMaps().putAll(esgCurr.getDefinitionMaps());
                                }
                            }
                            // 8 Handle non-primitives
                            else if (!isPrimitive) {
                                if (sgCurr instanceof SGCallNode) {
                                    SGCallNode sgCallNode = (SGCallNode) sgCurr;
                                    boolean hasNoMatch = sgCallNode.getDefinitionsMap()
                                            .values()
                                            .stream()
                                            .noneMatch(var -> var.equals(srcVar));
                                    boolean isCallReturnSiteEdge = trgtEsgIdx - currEsgIdx > 1;
                                    if (isCallReturnSiteEdge) {
                                        this.addEdge(esgEdges, trgtEsgLiveVarsMaps, currEsgIdx, trgtEsgIdx,
                                                srcCallIdx, srcCallIdx, srcVarId, srcVarId);
                                    } else {
                                        // Connect to all matches
                                        for (ProgramVariable match : sgCallNode.getDefinitionsMap().keySet()) {
                                            ProgramVariable matchedSrc = sgCallNode.getDefinitionsMap().get(match);
                                            if (matchedSrc.equals(srcVar)) {
                                                this.addEdge(esgEdges, trgtEsgLiveVarsMaps, currEsgIdx, trgtEsgIdx,
                                                        srcCallIdx, srcCallIdx + 1, srcVarId, match.getId());
                                                esgCurr.getDefinitionMaps().computeIfAbsent(srcCallIdx, k -> new HashMap<>());
                                                esgCurr.getDefinitionMaps().get(srcCallIdx).put(match.getId(), srcVarId);

                                                Multimap<UUID, UUID> matchesMap = ProjectData.getInstance().getMatchesMap();
                                                Collection<UUID> matches = matchesMap.get(match.getId());
                                                if (matches != null && !matches.contains(srcVarId)) {
                                                    matchesMap.put(match.getId(), srcVarId);
                                                }
                                            }
                                        }
                                    }
                                    esgNext.getDefinitionMaps().putAll(esgCurr.getDefinitionMaps());
                                } else if (sgCurr instanceof SGEntryNode) {
                                    this.addEdge(esgEdges, trgtEsgLiveVarsMaps, currEsgIdx, trgtEsgIdx,
                                            srcCallIdx, srcCallIdx, srcVarId, srcVarId);
                                    esgNext.getDefinitionMaps().putAll(esgCurr.getDefinitionMaps());
                                } else if (sgCurr instanceof SGExitNode) {
                                    esgCurr.getDefinitionMaps().remove(srcCallIdx);
                                    esgNext.getDefinitionMaps().putAll(esgCurr.getDefinitionMaps());
                                } else if (sgCurr instanceof SGReturnSiteNode) {
                                    this.addEdge(esgEdges, trgtEsgLiveVarsMaps, currEsgIdx, trgtEsgIdx,
                                            srcCallIdx, srcCallIdx, srcVarId, srcVarId);
                                    esgNext.getDefinitionMaps().putAll(esgCurr.getDefinitionMaps());
                                } else {
                                    this.addEdge(esgEdges, trgtEsgLiveVarsMaps, currEsgIdx, trgtEsgIdx,
                                            srcCallIdx, srcCallIdx, srcVarId, srcVarId);
                                    esgNext.getDefinitionMaps().putAll(esgCurr.getDefinitionMaps());
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
            if (!esgEdges.get(currEsgIdx).contains(newEdge)) {
                esgEdges.put(currEsgIdx, newEdge);
                trgLiveVarsMap.get(trgtCallIdx).put(trgtVarId, true);
            }
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
