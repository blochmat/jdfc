package graphs.esg;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import data.ClassData;
import data.MethodData;
import data.ProgramVariable;
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

        private final List<String> callSequence;

        private Map<ProgramVariable, Boolean> liveVariables;

        private final NavigableMap<Integer, Map<ProgramVariable, ProgramVariable>> callerToCalleeDefMap;

        private final NavigableMap<Integer, Map<ProgramVariable, ProgramVariable>> calleeToCallerDefMap;

        public MethodEsgCreator(String className, MethodData methodData) {
            this.className = className;
            this.methodData = methodData;
            this.superGraph = methodData.getSg();
            this.mainMethodName = methodData.buildInternalMethodName();
            this.mainMethodId = this.buildMethodIdentifier(this.className, mainMethodName);
            this.callSequence = new ArrayList<>();
            this.callSequence.add(mainMethodId);
            this.liveVariables = new HashMap<>();
            this.callerToCalleeDefMap = new TreeMap<>();
            this.calleeToCallerDefMap = new TreeMap<>();
            this.ZERO = new ProgramVariable.ZeroVariable(this.className, mainMethodName);
        }

        public ESG createESG() {
            //--- CREATE DOMAIN --------------------------------------------------------------------------------------------
            Map<String, Map<UUID, ProgramVariable>> domain = createDomain();

            //--- DEBUG DOMAIN ---------------------------------------------------------------------------------------------
            debugDomain(ImmutableMap.copyOf(domain));

            //--- CREATE NODES ---------------------------------------------------------------------------------------------
            NavigableMap<Integer, Map<String, Map<UUID, ESGNode>>> esgNodes = createESGNodes(ImmutableMap.copyOf(domain));

            // --- DEBUG NODES ---------------------------------------------------------------------------------------------
            debugNodes(esgNodes);

            //--- CREATE EDGES ---------------------------------------------------------------------------------------------
            Multimap<Integer, ESGEdge> esgEdges = ArrayListMultimap.create();

            for(SGNode currSGNode : superGraph.getNodes().values()) {
                int currSGNodeIdx = currSGNode.getIndex();
                String currSGNodeMethodName = currSGNode.getMethodName();
                String currSGNodeMethodIdentifier = this.buildMethodIdentifier(
                        currSGNode.getClassName(), currSGNode.getMethodName());

                //--- UPDATE ACTIVE SCOPE ----------------------------------------------------------------------------------
                Map<String, Map<UUID, ProgramVariable>> activeScope = updateActiveScope(domain, currSGNode);

                //--- DEBUG ACTIVE DOMAIN ----------------------------------------------------------------------------------
                debugActiveScope(activeScope, currSGNodeIdx);

                //--- UPDATE LIVE VARIABLES --------------------------------------------------------------------------------
                liveVariables = updateLiveVariables(activeScope, currSGNode);

                //--- DEBUG LIVE VARIABLES ---------------------------------------------------------------------------------
                debugLiveVariables(currSGNodeIdx);

                // --- CREATE EDGES ----------------------------------------------------------------------------------------
                createEdges(esgEdges, currSGNode, currSGNodeIdx, currSGNodeMethodIdentifier, activeScope);
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
            for(ESGEdge esgEdge : esgEdges.values()) {
                int sgnSourceIdx = esgEdge.getSgnSourceIdx();
                int sgnTargetIdx = esgEdge.getSgnTargetIdx();
                String sourceMethodName = esgEdge.getSourceMethodId();
                String targetMethodName = esgEdge.getTargetMethodId();
                ProgramVariable sourceDVar = esgEdge.getSourceVar();
                ProgramVariable targetDVar = esgEdge.getTargetVar();

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
            return new ESG(superGraph, esgNodes, esgEdges, domain, callerToCalleeDefMap, calleeToCallerDefMap);
        }

        private void createEdges(Multimap<Integer, ESGEdge> esgEdges, SGNode currSGNode, int currSGNodeIdx, String currSGNodeMethodIdentifier, Map<String, Map<UUID, ProgramVariable>> activeScope) {
            for(Map.Entry<String, Map<UUID, ProgramVariable>> activeDomainMethodSection : activeScope.entrySet()) {
                String currVariableMethodIdentifier = activeDomainMethodSection.getKey();
                Map<UUID, ProgramVariable> programVariables = activeDomainMethodSection.getValue();

                if (mainMethodId.contains("defineA") && currSGNodeIdx == 4) {
                    System.out.println();
                }

                for (ProgramVariable pVar : programVariables.values()) {
                    Collection<Integer> currSGNodeTargets = superGraph.getEdges().get(currSGNodeIdx);
                    for (Integer currSGNodeTargetIdx : currSGNodeTargets) {
                        SGNode sgTargetNode = superGraph.getNodes().get(currSGNodeTargetIdx);
                        if(Objects.equals(pVar, ZERO)) {
                            // Special case ZERO
                            ESGEdge edge = handleZero(currSGNode, sgTargetNode, pVar);
                            esgEdges.put(currSGNodeIdx, edge);
                        } else if ((Objects.equals(pVar.getName(), "this") || pVar.getIsField())) {
                            if (!asmHelper.isStatic(currSGNode.getMethodAccess())
                                && Objects.equals(currSGNodeMethodIdentifier, currVariableMethodIdentifier)) {
                                Set<ESGEdge> edges = handleGlobal(currSGNode, sgTargetNode, pVar);
                                esgEdges.putAll(currSGNodeIdx, edges);
                            } else {
                                // Todo: connect outer this of call site
                                // 1. find call site
                                // 2. connect outer this
                                System.out.println();
                            }
                        } else {
                            // Local variables
                            if(Objects.equals(currSGNodeMethodIdentifier, currVariableMethodIdentifier)) {
                                Set<ESGEdge> edges = handleLocal(currSGNode, sgTargetNode, pVar);
                                esgEdges.putAll(currSGNodeIdx, edges);
                            } else {
                                // TODO:
                                if (!(currSGNode instanceof SGCallNode) && asmHelper.isCallByValue(pVar.getDescriptor())) {
                                    // we basically want to create an edge that connects the variable in its own method
                                    String sgNodeMId = this.buildMethodIdentifier(pVar.getClassName(), pVar.getMethodName());
                                    String sgTargetNodeMId = this.buildMethodIdentifier(pVar.getClassName(), pVar.getMethodName());
                                    ESGEdge edge = new ESGEdge(
                                            currSGNodeIdx,
                                            currSGNodeTargetIdx,
                                            sgNodeMId,
                                            sgTargetNodeMId,
                                            pVar,
                                            pVar
                                    );
                                    esgEdges.put(currSGNodeIdx, edge);
                                }
                            }
                        }
                    }
                }
            }
        }

        public Map<String, Map<UUID, ProgramVariable>> updateActiveScope(
                Map<String, Map<UUID, ProgramVariable>> domain,
                SGNode currSGNode
        ) {
            Map<String, Map<UUID, ProgramVariable>> newActiveScope = new HashMap<>();
            if(currSGNode instanceof SGCallNode) {
                SGCallNode sgCallNode = (SGCallNode) currSGNode;
                String calledSGNodeMethodId = this.buildMethodIdentifier(
                        sgCallNode.getCalledClassName(),
                        sgCallNode.getCalledMethodName()
                );
                callSequence.add(calledSGNodeMethodId);
            }

            if(currSGNode instanceof SGReturnSiteNode) {
                callSequence.remove(callSequence.size() - 1);
            }

            for(String methodIdentifier : callSequence) {
                newActiveScope.computeIfAbsent(methodIdentifier, k -> domain.get(methodIdentifier));
            }

            return newActiveScope;
        }

        public void debugActiveScope(
                Map<String, Map<UUID, ProgramVariable>> activeScope,
                Integer currSGNodeIdx
        ){
            if(log.isDebugEnabled()) {
                StringBuilder sb = new StringBuilder();
                sb.append("\nCALL SEQUENCE: ").append(callSequence).append("\n");
                sb.append("\nACTIVE SCOPE: ").append(mainMethodId).append("\n");

                for(Map.Entry<String, Map<UUID, ProgramVariable>> domainMethodEntry : activeScope.entrySet()) {
                    sb.append(domainMethodEntry.getKey()).append("\n");
                    sb.append(JDFCUtils.prettyPrintMap(domainMethodEntry.getValue()));
                }

                JDFCUtils.logThis(sb.toString(), String.valueOf(currSGNodeIdx));
            }
        }

        public Set<ESGEdge> handleGlobal(SGNode sgNode, SGNode sgTargetNode, ProgramVariable pVar) {
            Set<ESGEdge> edges = new HashSet<>();
            String sgNodeMId = this.buildMethodIdentifier(sgNode.getClassName(), sgNode.getMethodName());
            String sgTargetNodeMId = this.buildMethodIdentifier(sgTargetNode.getClassName(), sgTargetNode.getMethodName());
            if(sgNode instanceof SGCallNode) {
                ProgramVariable match = ((SGCallNode) sgNode).getDefinitionsMap().get(pVar);
                if(match != null) {
                    // if match exists connect to subroutine
                    callerToCalleeDefMap.computeIfAbsent(sgNode.getIndex(), k -> new HashMap<>());
                    callerToCalleeDefMap.get(sgNode.getIndex()).put(pVar, match);
                    calleeToCallerDefMap.computeIfAbsent(((SGCallNode) sgNode).getExitNodeIdx(), k -> new HashMap<>());
                    calleeToCallerDefMap.get(((SGCallNode) sgNode).getExitNodeIdx()).put(match, pVar);
                    edges.add(new ESGEdge(
                            sgNode.getIndex(),
                            sgTargetNode.getIndex(),
                            sgNodeMId,
                            sgTargetNodeMId,
                            pVar,
                            match
                    ));
                } else {
                    // field that has no match
                    // "this" when a static method is invoked
                    String pVarMId = this.buildMethodIdentifier(pVar.getClassName(), pVar.getMethodName());
                    if (pVarMId.equals(mainMethodId)) {
                        // similar to ZERO
                        edges.add(new ESGEdge(
                                sgNode.getIndex(),
                                sgTargetNode.getIndex(),
                                sgNodeMId,
                                sgNodeMId,
                                pVar,
                                pVar
                        ));
                    }
                }
            } else if(sgNode instanceof SGEntryNode) {
                if(!liveVariables.get(pVar) || sgNode.getIndex() == 0) {
                    edges.add(new ESGEdge(
                            sgNode.getIndex(),
                            sgTargetNode.getIndex(),
                            mainMethodId,
                            sgTargetNodeMId,
                            ZERO,
                            pVar
                    ));
                } else {
                    edges.add(new ESGEdge(
                            sgNode.getIndex(),
                            sgTargetNode.getIndex(),
                            sgNodeMId,
                            sgTargetNodeMId,
                            pVar,
                            pVar
                    ));
                }
            } else if (sgNode instanceof SGExitNode) {
                if(calleeToCallerDefMap.get(sgNode.getIndex()) != null) {
                    ProgramVariable m = calleeToCallerDefMap.get(sgNode.getIndex()).get(pVar);
                    if(m != null) {
                        edges.add(new ESGEdge(
                                sgNode.getIndex(),
                                sgTargetNode.getIndex(),
                                sgNodeMId,
                                sgTargetNodeMId,
                                pVar,
                                m
                        ));
                    }
                }
            } else {
                ProgramVariable newDef = findMatch(sgNode.getDefinitions(), pVar);
                if(newDef == null) {
                    edges.add(new ESGEdge(
                            sgNode.getIndex(),
                            sgTargetNode.getIndex(),
                            sgNodeMId,
                            sgTargetNodeMId,
                            pVar,
                            pVar
                    ));
                } else {
                    edges.add(new ESGEdge(
                            sgNode.getIndex(),
                            sgTargetNode.getIndex(),
                            sgNodeMId,
                            sgTargetNodeMId,
                            newDef,
                            newDef
                    ));
                }
            }

            return edges;
        }

        private Set<ESGEdge> handleLocal(SGNode sgNode, SGNode sgTargetNode, ProgramVariable pVar) {
            Set<ESGEdge> edges = new HashSet<>();
            String sgNodeMId = this.buildMethodIdentifier(sgNode.getClassName(), sgNode.getMethodName());
            String sgTargetNodeMId = this.buildMethodIdentifier(sgTargetNode.getClassName(), sgTargetNode.getMethodName());
            if(sgNode instanceof SGCallNode) {
                ProgramVariable match = ((SGCallNode) sgNode).getDefinitionsMap().get(pVar);
                if(match != null) {
                    // match caller var to callee var
                    callerToCalleeDefMap.computeIfAbsent(sgNode.getIndex(), k -> new HashMap<>());
                    callerToCalleeDefMap.get(sgNode.getIndex()).put(pVar, match);
                    calleeToCallerDefMap.computeIfAbsent(((SGCallNode) sgNode).getExitNodeIdx(), k -> new HashMap<>());
                    calleeToCallerDefMap.get(((SGCallNode) sgNode).getExitNodeIdx()).put(match, pVar);
                    edges.add(new ESGEdge(
                            sgNode.getIndex(),
                            sgTargetNode.getIndex(),
                            sgNodeMId,
                            sgTargetNodeMId,
                            pVar,
                            match
                    ));
                }
            } else if(sgNode instanceof SGEntryNode) {
                if(!liveVariables.get(pVar) || sgNode.getIndex() == 0) {
                    if(sgNode.getDefinitions().contains(pVar)) {
                        // add definition if present
                        edges.add(new ESGEdge(
                                sgNode.getIndex(),
                                sgTargetNode.getIndex(),
                                mainMethodId,
                                sgTargetNodeMId,
                                ZERO,
                                pVar
                        ));
                    }
                } else { // matched variable already is defined
                    edges.add(new ESGEdge(
                            sgNode.getIndex(),
                            sgTargetNode.getIndex(),
                            sgNodeMId,
                            sgTargetNodeMId,
                            pVar,
                            pVar
                    ));
                }
            } else if (sgNode instanceof SGExitNode) {
                if(!asmHelper.isCallByValue(pVar.getDescriptor())) {
                    // if var is object
                    Collection<ProgramVariable> matches = ((SGExitNode) sgNode).getDefinitionsMap().get(pVar);
                    for (ProgramVariable match : matches) {
                        // match back do definition on caller site
                        edges.add(new ESGEdge(
                                sgNode.getIndex(),
                                sgTargetNode.getIndex(),
                                sgNodeMId,
                                sgTargetNodeMId,
                                pVar,
                                match
                        ));
                    }
                }
            } else {
                ProgramVariable newDef = findMatch(sgTargetNode.getDefinitions(), pVar);
                if(newDef == null) {
                    if(liveVariables.get(pVar)) {
                        edges.add(new ESGEdge(
                                sgNode.getIndex(),
                                sgTargetNode.getIndex(),
                                sgNodeMId,
                                sgTargetNodeMId,
                                pVar,
                                pVar
                        ));
                    }
                    else if (sgNode.getDefinitions().contains(pVar)) {
                        edges.add(new ESGEdge(
                                sgNode.getIndex(),
                                sgTargetNode.getIndex(),
                                sgNodeMId,
                                sgTargetNodeMId,
                                pVar,
                                pVar
                        ));
                    }
                } else {
                    if(liveVariables.get(pVar)) { // connect already existing variable definition
                        edges.add(new ESGEdge(
                                sgNode.getIndex(),
                                sgTargetNode.getIndex(),
                                sgNodeMId,
                                sgTargetNodeMId,
                                pVar,
                                pVar
                        ));
                    } else { // add new variable definition
                        edges.add(new ESGEdge(
                                sgNode.getIndex(),
                                sgTargetNode.getIndex(),
                                mainMethodId,
                                sgTargetNodeMId,
                                ZERO,
                                newDef
                        ));
                    }
                }
            }

            return edges;
        }

        private ESGEdge handleLiveOuterScopeLocals(SGNode sgNode, SGNode sgTargetNode, ProgramVariable pVar) {
            String pVarMId = this.buildMethodIdentifier(pVar.getClassName(), pVar.getMethodName());
            return new ESGEdge(
                    sgNode.getIndex(),
                    sgTargetNode.getIndex(),
                    pVarMId,
                    pVarMId,
                    pVar,
                    pVar
            );
        }

        private ESGEdge handleZero(SGNode sgNode, SGNode sgTargetNode, ProgramVariable pVar) {
            return new ESGEdge(
                    sgNode.getIndex(),
                    sgTargetNode.getIndex(),
                    mainMethodId,
                    mainMethodId,
                    pVar,
                    pVar
            );
        }

        private ProgramVariable findLastActiveDef(SGExitNode sgNode, ProgramVariable pVar) {
            // TODO: Can there be multiple last active defs present?
            return null;
        }

        private Set<ESGEdge> handleOtherScope(SGNode sgNode, SGNode sgTargetNode, ProgramVariable pVar) {
            Set<ESGEdge> edges = new HashSet<>();
            if(sgNode instanceof SGCallNode) {
                SGCallNode sgCallNode = (SGCallNode) sgNode;
                String calledMethodId = this.buildMethodIdentifier(sgCallNode.getCalledClassName(), sgCallNode.getCalledMethodName());
                String pVarMId = this.buildMethodIdentifier(pVar.buildClassNodeName(), pVar.getMethodName());
                if(Objects.equals(calledMethodId, pVarMId)) {
                    if(!sgCallNode.getDefinitionsMap().containsValue(pVar)
                            && sgTargetNode.getDefinitions().contains(pVar)) {
                        edges.add(new ESGEdge(
                                sgNode.getIndex(),
                                sgTargetNode.getIndex(),
                                mainMethodId,
                                calledMethodId,
                                ZERO,
                                pVar
                        ));
                    }
                } else {
                    if(liveVariables.get(pVar)) {
                        edges.add(new ESGEdge(
                                sgNode.getIndex(),
                                sgTargetNode.getIndex(),
                                pVarMId,
                                pVarMId,
                                pVar,
                                pVar
                        ));
                    }
                }
            }
            else if (sgNode instanceof SGEntryNode) {
                if(liveVariables.get(pVar)) {
                    String pVarMId = this.buildMethodIdentifier(pVar.buildClassNodeName(), pVar.getMethodName());
                    edges.add(new ESGEdge(
                            sgNode.getIndex(),
                            sgTargetNode.getIndex(),
                            pVarMId,
                            pVarMId,
                            pVar,
                            pVar
                    ));
                }
            }
            else if (sgNode instanceof SGExitNode) {
                if(liveVariables.get(pVar)) {
                    String pVarMId = this.buildMethodIdentifier(pVar.buildClassNodeName(), pVar.getMethodName());
                    edges.add(new ESGEdge(
                            sgNode.getIndex(),
                            sgTargetNode.getIndex(),
                            pVarMId,
                            pVarMId,
                            pVar,
                            pVar
                    ));
                }
            }
            else if (sgNode instanceof SGReturnSiteNode) {
                String pVarMId = this.buildMethodIdentifier(pVar.buildClassNodeName(), pVar.getMethodName());
                edges.add(new ESGEdge(
                        sgNode.getIndex(),
                        sgTargetNode.getIndex(),
                        pVarMId,
                        pVarMId,
                        pVar,
                        pVar
                ));
            }
            else {
                if(liveVariables.get(pVar)) {
                    String pVarMId = this.buildMethodIdentifier(pVar.buildClassNodeName(), pVar.getMethodName());
                    edges.add(new ESGEdge(
                            sgNode.getIndex(),
                            sgTargetNode.getIndex(),
                            pVarMId,
                            pVarMId,
                            pVar,
                            pVar
                    ));
                }
            }
            return edges;
        }

        private Map<ProgramVariable, Boolean> updateLiveVariables(Map<String, Map<UUID, ProgramVariable>> activeScope, SGNode sgNode) {
            Map<ProgramVariable, Boolean> updated = new HashMap<>();
            String sgNodeMId = this.buildMethodIdentifier(sgNode.getClassName(), sgNode.getMethodName());

            for(Map.Entry<String, Map<UUID, ProgramVariable>> mEntry : activeScope.entrySet()) {
                for(ProgramVariable p : activeScope.get(mEntry.getKey()).values()) {
                    if(callSequence.contains(mEntry.getKey())) {
                        if(Objects.equals(mEntry.getKey(), sgNodeMId)) {
                            if(sgNode.getCfgReachOut().contains(p)) {
                                updated.put(p, true);
                            } else {
                                updated.put(p, false);
                            }
                        } else {
                            updated.put(p, liveVariables.getOrDefault(p, false));
                        }
                    } else {
                        updated.put(p, false);
                    }
                }
            }

            return updated;
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


        public Map<String, Map<UUID, ProgramVariable>> createDomain() {
            Map<String, Map<UUID, ProgramVariable>> domain = new HashMap<>();
            for(SGNode sgNode : superGraph.getNodes().values()) {
                String sgNodeClassName = sgNode.getClassName();
                String sgNodeMethodName = sgNode.getMethodName();
                String sgNodeMethodIdentifier = this.buildMethodIdentifier(sgNodeClassName, sgNodeMethodName);
                domain.computeIfAbsent(sgNodeMethodIdentifier, k -> new HashMap<>());
                if(Objects.equals(sgNodeMethodIdentifier, mainMethodId)) {
                    domain.get(sgNodeMethodIdentifier).put(ZERO.getId(), ZERO);
                    liveVariables.put(ZERO, false);
                }
                for(ProgramVariable def : sgNode.getDefinitions()) {
                    domain.get(sgNodeMethodIdentifier).put(def.getId(), def);
                    liveVariables.put(def, false);
                }
            }
            return domain;
        }

        public void debugDomain(Map<String, Map<UUID, ProgramVariable>> domain) {
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

        public NavigableMap<Integer, Map<String, Map<UUID, ESGNode>>> createESGNodes(Map<String, Map<UUID, ProgramVariable>> domain) {
            NavigableMap<Integer, Map<String, Map<UUID, ESGNode>>> esgNodes = Maps.newTreeMap();

            // create nodes for SG nodes
            for(SGNode sgNode : superGraph.getNodes().values()) {
                int sgNodeIdx = sgNode.getIndex();

                esgNodes.computeIfAbsent(sgNodeIdx, k -> Maps.newTreeMap());
                esgNodes.get(sgNodeIdx).computeIfAbsent(mainMethodId, k -> Maps.newTreeMap());
                esgNodes.get(sgNodeIdx)
                        .get(mainMethodId)
                        .put(UUID.fromString("00000000-0000-0000-0000-000000000000"), new ESGNode.ESGZeroNode(sgNodeIdx, className, mainMethodName));

                for(Map.Entry<String, Map<UUID, ProgramVariable>> domainMethodEntry : domain.entrySet()) {
                    for(Map.Entry<UUID, ProgramVariable> pVarEntry : domainMethodEntry.getValue().entrySet()) {
                        esgNodes.get(sgNodeIdx).computeIfAbsent(domainMethodEntry.getKey(), k -> Maps.newTreeMap());

                        if(pVarEntry.getValue() instanceof ProgramVariable.ZeroVariable) {
                            esgNodes.get(sgNodeIdx)
                                    .get(mainMethodId)
                                    .put(UUID.fromString("00000000-0000-0000-0000-000000000000"), new ESGNode.ESGZeroNode(sgNodeIdx, className, mainMethodName));
                        } else {
                            esgNodes.get(sgNodeIdx)
                                    .get(domainMethodEntry.getKey())
                                    .put(pVarEntry.getKey(), new ESGNode(sgNodeIdx, pVarEntry.getValue()));
                        }
                    }
                }
            }

            return esgNodes;
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
