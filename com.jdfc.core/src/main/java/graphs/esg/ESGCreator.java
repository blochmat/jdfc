package graphs.esg;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import data.ClassData;
import data.DefUsePair;
import data.MethodData;
import data.ProgramVariable;
import graphs.esg.nodes.ESGNode;
import graphs.sg.SG;
import graphs.sg.nodes.*;
import lombok.extern.slf4j.Slf4j;
import utils.JDFCUtils;

import java.util.*;

@Slf4j
public class ESGCreator {

    private MethodData METHOD_DATA;

    private SG SUPER_GRAPH;

    private String MAIN_METHOD_CLASS_NAME;

    private String MAIN_METHOD_NAME;

    private String MAIN_METHOD_ID;

    private ProgramVariable ZERO;

    private List<String> CALL_SEQUENCE;

    private Map<ProgramVariable, Boolean> LIVE_VARIABLES;

    private NavigableMap<Integer, Map<ProgramVariable, ProgramVariable>> CALLER_TO_CALLEE_DEFINITION_MAP;

    private NavigableMap<Integer, Map<ProgramVariable, ProgramVariable>> CALLEE_TO_CALLER_DEFINITION_MAP;

    public void createESGsForClass(ClassData cData) {
        MAIN_METHOD_CLASS_NAME = cData.getClassMetaData().getClassFileRel();
        for(MethodData mData : cData.getMethodDataFromStore().values()) {
            METHOD_DATA = mData;
            SUPER_GRAPH = mData.getSg();
            MAIN_METHOD_NAME = mData.buildInternalMethodName();
            MAIN_METHOD_ID = this.buildMethodIdentifier(MAIN_METHOD_CLASS_NAME, MAIN_METHOD_NAME);
            ZERO = new ProgramVariable.ZeroVariable(MAIN_METHOD_CLASS_NAME, MAIN_METHOD_NAME);
            CALL_SEQUENCE = new ArrayList<>();
            CALL_SEQUENCE.add(MAIN_METHOD_ID);
            LIVE_VARIABLES = new HashMap<>();
            CALLER_TO_CALLEE_DEFINITION_MAP = new TreeMap<>();
            CALLEE_TO_CALLER_DEFINITION_MAP = new TreeMap<>();


            ESG esg = this.createESGForMethod();
            mData.setEsg(esg);
//            TabulationAlgorithm tabulationAlgorithm = new TabulationAlgorithm(esg);
//            Multimap<Integer, ProgramVariable> MVP = tabulationAlgorithm.execute();
//            String debug = String.format("%s :: %s\n%s",
//                    cData.getRelativePath(),
//                    mData.buildInternalMethodName(),
//                    JDFCUtils.prettyPrintMultimap(MVP));
//            JDFCUtils.logThis(debug, "MVP");
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
            CALL_SEQUENCE.add(calledSGNodeMethodId);
        }

        if(currSGNode instanceof SGReturnSiteNode) {
            CALL_SEQUENCE.remove(CALL_SEQUENCE.size() - 1);
        }

        for(String methodIdentifier : CALL_SEQUENCE) {
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
            sb.append("\nCALL SEQUENCE: ").append(CALL_SEQUENCE).append("\n");
            sb.append("\nACTIVE SCOPE: ").append(MAIN_METHOD_ID).append("\n");

            for(Map.Entry<String, Map<UUID, ProgramVariable>> domainMethodEntry : activeScope.entrySet()) {
                sb.append(domainMethodEntry.getKey()).append("\n");
                sb.append(JDFCUtils.prettyPrintMap(domainMethodEntry.getValue()));
            }

            JDFCUtils.logThis(sb.toString(), String.valueOf(currSGNodeIdx));
        }
    }

    public ProgramVariable findDefMatch(SGCallNode sgNode, ProgramVariable def) {
        List<ProgramVariable> usages = new ArrayList<>();
        for(DefUsePair pair : METHOD_DATA.getDUPairsFromStore().values()) {
            if(Objects.equals(pair.getDefFromStore(), def)) {
               usages.add(pair.getUseFromStore());
            }
        }

        for(ProgramVariable use : usages) {
            ProgramVariable defMatch = sgNode.getUseDefMap().get(use);
            if(defMatch != null) {
                return defMatch;
            }
        }
        return null;
    }

    public ProgramVariable findDefMatch(SGEntryNode sgNode, ProgramVariable def) {
        List<ProgramVariable> usages = new ArrayList<>();
        for(DefUsePair pair : METHOD_DATA.getDUPairsFromStore().values()) {
            if(Objects.equals(pair.getDefFromStore(), def)) {
                usages.add(pair.getUseFromStore());
            }
        }

        for(ProgramVariable use : usages) {
            ProgramVariable defMatch = sgNode.getUseDefMap().get(use);
            if(defMatch != null) {
                return defMatch;
            }
        }
        return null;
    }

    public Set<ESGEdge> handleGlobal(SGNode sgNode, SGNode sgTargetNode, ProgramVariable pVar) {
        Set<ESGEdge> edges = new HashSet<>();
        String sgNodeMId = this.buildMethodIdentifier(sgNode.getClassName(), sgNode.getMethodName());
        String sgTargetNodeMId = this.buildMethodIdentifier(sgTargetNode.getClassName(), sgTargetNode.getMethodName());
        if(sgNode instanceof SGCallNode) {
            ProgramVariable m = findDefMatch(((SGCallNode) sgNode), pVar);
            CALLER_TO_CALLEE_DEFINITION_MAP.computeIfAbsent(sgNode.getIndex(), k -> new HashMap<>());
            CALLER_TO_CALLEE_DEFINITION_MAP.get(sgNode.getIndex()).put(pVar, m);
            CALLEE_TO_CALLER_DEFINITION_MAP.computeIfAbsent(((SGCallNode) sgNode).getExitNodeIdx(), k -> new HashMap<>());
            CALLEE_TO_CALLER_DEFINITION_MAP.get(((SGCallNode) sgNode).getExitNodeIdx()).put(m, pVar);
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
        } else if(sgNode instanceof SGEntryNode) {
            if(!LIVE_VARIABLES.get(pVar) || sgNode.getIndex() == 0) {
                edges.add(new ESGEdge(
                        sgNode.getIndex(),
                        sgTargetNode.getIndex(),
                        MAIN_METHOD_ID,
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
            ProgramVariable m = CALLEE_TO_CALLER_DEFINITION_MAP.get(sgNode.getIndex()).get(pVar);
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
            ProgramVariable m = findDefMatch((SGCallNode) sgNode, pVar);
            if(m != null) {
                edges.add(new ESGEdge(
                        sgNode.getIndex(),
                        sgTargetNode.getIndex(),
                        sgNodeMId,
                        sgNodeMId,
                        pVar,
                        pVar
                ));
                edges.add(new ESGEdge(
                        sgNode.getIndex(),
                        sgTargetNode.getIndex(),
                        sgNodeMId,
                        sgTargetNodeMId,
                        pVar,
                        m
                ));
            } else {
                edges.add(new ESGEdge(
                        sgNode.getIndex(),
                        sgTargetNode.getIndex(),
                        sgNodeMId,
                        sgNodeMId,
                        pVar,
                        pVar
                ));
            }
        } else if(sgNode instanceof SGEntryNode) {
            if(!LIVE_VARIABLES.get(pVar) || sgNode.getIndex() == 0) {
                if(sgNode.getDefinitions().contains(pVar)) {
                    edges.add(new ESGEdge(
                            sgNode.getIndex(),
                            sgTargetNode.getIndex(),
                            MAIN_METHOD_ID,
                            sgTargetNodeMId,
                            ZERO,
                            pVar
                    ));
                }
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
            ProgramVariable m = ((SGExitNode) sgNode).getPVarMap().get(pVar);
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
        } else {
            ProgramVariable newDef = findMatch(sgTargetNode.getDefinitions(), pVar);
            if(newDef == null) {
                if(LIVE_VARIABLES.get(pVar)) {
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
                if(LIVE_VARIABLES.get(pVar)) {
                    edges.add(new ESGEdge(
                            sgNode.getIndex(),
                            sgTargetNode.getIndex(),
                            sgNodeMId,
                            sgTargetNodeMId,
                            pVar,
                            pVar
                    ));
                    edges.add(new ESGEdge(
                            sgNode.getIndex(),
                            sgTargetNode.getIndex(),
                            MAIN_METHOD_ID,
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
                MAIN_METHOD_ID,
                MAIN_METHOD_ID,
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
            String pVarMId = this.buildMethodIdentifier(pVar.getClassName(), pVar.getMethodName());
            if(Objects.equals(calledMethodId, pVarMId)) {
                if(!sgCallNode.getUseDefMap().containsValue(pVar)
                    && sgTargetNode.getDefinitions().contains(pVar)) {
                    edges.add(new ESGEdge(
                            sgNode.getIndex(),
                            sgTargetNode.getIndex(),
                            MAIN_METHOD_ID,
                            calledMethodId,
                            ZERO,
                            pVar
                    ));
                }
            } else {
                if(LIVE_VARIABLES.get(pVar)) {
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
            if(LIVE_VARIABLES.get(pVar)) {
                String pVarMId = this.buildMethodIdentifier(pVar.getClassName(), pVar.getMethodName());
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
            if(LIVE_VARIABLES.get(pVar)) {
                String pVarMId = this.buildMethodIdentifier(pVar.getClassName(), pVar.getMethodName());
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
            String pVarMId = this.buildMethodIdentifier(pVar.getClassName(), pVar.getMethodName());
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
            if(LIVE_VARIABLES.get(pVar)) {
                String pVarMId = this.buildMethodIdentifier(pVar.getClassName(), pVar.getMethodName());
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
                if(CALL_SEQUENCE.contains(mEntry.getKey())) {
                    if(Objects.equals(mEntry.getKey(), sgNodeMId)) {
                        if(sgNode.getCfgReachOut().contains(p)) {
                            updated.put(p, true);
                        } else {
                            updated.put(p, false);
                        }
                    } else {
                        updated.put(p, LIVE_VARIABLES.getOrDefault(p, false));
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
            sb.append("\nLIVE VARIABLES: ").append(MAIN_METHOD_ID).append("\n");

            for(Map.Entry<ProgramVariable, Boolean> liveEntry : LIVE_VARIABLES.entrySet()) {
                sb.append(liveEntry.getKey()).append(" => ").append(liveEntry.getValue()).append("\n");
            }

            JDFCUtils.logThis(sb.toString(), String.valueOf(currSgIdx));
        }
    }

    public ESG createESGForMethod() {
        //--- CREATE DOMAIN --------------------------------------------------------------------------------------------
        Map<String, Map<UUID, ProgramVariable>> domain = createDomain();

        //--- DEBUG DOMAIN ---------------------------------------------------------------------------------------------
        debugDomain(ImmutableMap.copyOf(domain));

        //--- CREATE NODES ---------------------------------------------------------------------------------------------
        NavigableMap<Integer, Map<String, Map<UUID, ESGNode>>> esgNodes = createESGNode(ImmutableMap.copyOf(domain));

        // --- DEBUG NODES ---------------------------------------------------------------------------------------------
        debugNodes(esgNodes);

        //--- CREATE EDGES ---------------------------------------------------------------------------------------------
        Multimap<Integer, ESGEdge> esgEdges = ArrayListMultimap.create();

        for(SGNode currSGNode : SUPER_GRAPH.getNodes().values()) {
            int currSGNodeIdx = currSGNode.getIndex();
            String currSGNodeMethodName = currSGNode.getMethodName();
            String currSGNodeMethodIdentifier = this.buildMethodIdentifier(
                    currSGNode.getClassName(), currSGNode.getMethodName());

            //--- UPDATE ACTIVE SCOPE ----------------------------------------------------------------------------------
            Map<String, Map<UUID, ProgramVariable>> activeScope = updateActiveScope(domain, currSGNode);

            //--- DEBUG ACTIVE DOMAIN ----------------------------------------------------------------------------------
            debugActiveScope(activeScope, currSGNodeIdx);

            //--- UPDATE LIVE VARIABLES --------------------------------------------------------------------------------
            LIVE_VARIABLES = updateLiveVariables(activeScope, currSGNode);

            //--- DEBUG LIVE VARIABLES ---------------------------------------------------------------------------------
            debugLiveVariables(currSGNodeIdx);

            // --- CREATE EDGES ----------------------------------------------------------------------------------------
            for(Map.Entry<String, Map<UUID, ProgramVariable>> activeDomainMethodSection : activeScope.entrySet()) {
                String currVariableMethodIdentifier = activeDomainMethodSection.getKey();
                Map<UUID, ProgramVariable> programVariables = activeDomainMethodSection.getValue();

                for (ProgramVariable pVar : programVariables.values()) {
                    Collection<Integer> currSGNodeTargets = SUPER_GRAPH.getEdges().get(currSGNodeIdx);
                    for (Integer currSGNodeTargetIdx : currSGNodeTargets) {
                        SGNode sgTargetNode = SUPER_GRAPH.getNodes().get(currSGNodeTargetIdx);

                        if(Objects.equals(pVar, ZERO)) {
                            ESGEdge edge = handleZero(currSGNode, sgTargetNode, pVar);
                            esgEdges.put(currSGNodeIdx, edge);
                        } else {
                            if(Objects.equals(currSGNodeMethodIdentifier, currVariableMethodIdentifier)) {
                                Set<ESGEdge> edges;
                                if(Objects.equals(pVar.getName(), "this") || pVar.getIsField()) {
                                    edges = handleGlobal(currSGNode, sgTargetNode, pVar);
                                } else {
                                    edges = handleLocal(currSGNode, sgTargetNode, pVar);
                                }
                                if(!edges.isEmpty()) {
                                    esgEdges.putAll(currSGNodeIdx, edges);
                                }
                            } else if (!Objects.equals(pVar.getName(), "this")
                                    && !pVar.getIsField()){
                                Set<ESGEdge> edges = handleOtherScope(currSGNode, sgTargetNode, pVar);
                                if(!edges.isEmpty()) {
                                    esgEdges.putAll(currSGNodeIdx, edges);
                                }
                            }
                        }

                    }
                }
            }
        }

        //--- DEGUG EDGES ----------------------------------------------------------------------------------------------
        if(log.isDebugEnabled()) {
            StringBuilder sb = new StringBuilder();
            sb.append(MAIN_METHOD_CLASS_NAME).append(" ");
            sb.append(MAIN_METHOD_NAME).append("\n");
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
        return new ESG(SUPER_GRAPH, esgNodes, esgEdges, domain, CALLER_TO_CALLEE_DEFINITION_MAP, CALLEE_TO_CALLER_DEFINITION_MAP);
    }

    public Map<String, Map<UUID, ProgramVariable>> createDomain() {
        Map<String, Map<UUID, ProgramVariable>> domain = new HashMap<>();
        for(SGNode sgNode : SUPER_GRAPH.getNodes().values()) {
            String sgNodeClassName = sgNode.getClassName();
            String sgNodeMethodName = sgNode.getMethodName();
            String sgNodeMethodIdentifier = this.buildMethodIdentifier(sgNodeClassName, sgNodeMethodName);
            domain.computeIfAbsent(sgNodeMethodIdentifier, k -> new HashMap<>());
            if(Objects.equals(sgNodeMethodIdentifier, MAIN_METHOD_ID)) {
                domain.get(sgNodeMethodIdentifier).put(ZERO.getId(), ZERO);
                LIVE_VARIABLES.put(ZERO, false);
            }
            for(ProgramVariable def : sgNode.getDefinitions()) {
                domain.get(sgNodeMethodIdentifier).put(def.getId(), def);
                LIVE_VARIABLES.put(def, false);
            }
        }
        return domain;
    }

    public void debugDomain(Map<String, Map<UUID, ProgramVariable>> domain) {
        if(log.isDebugEnabled()) {
            StringBuilder sb = new StringBuilder();
            sb.append(MAIN_METHOD_ID).append("\n");

            for(Map.Entry<String, Map<UUID, ProgramVariable>> domainMethodEntry : domain.entrySet()) {
                sb.append(domainMethodEntry.getKey()).append("\n");
                sb.append(JDFCUtils.prettyPrintMap(domainMethodEntry.getValue()));
                sb.append("\n");
            }

            JDFCUtils.logThis(sb.toString(), "ESGCreator_domain");
        }
    }

    public NavigableMap<Integer, Map<String, Map<UUID, ESGNode>>> createESGNode(Map<String, Map<UUID, ProgramVariable>> domain) {
        NavigableMap<Integer, Map<String, Map<UUID, ESGNode>>> esgNodes = Maps.newTreeMap();

        // create nodes for SG nodes
        for(SGNode sgNode : SUPER_GRAPH.getNodes().values()) {
            int sgNodeIdx = sgNode.getIndex();

            esgNodes.computeIfAbsent(sgNodeIdx, k -> Maps.newTreeMap());
            esgNodes.get(sgNodeIdx).computeIfAbsent(MAIN_METHOD_ID, k -> Maps.newTreeMap());
            esgNodes.get(sgNodeIdx)
                    .get(MAIN_METHOD_ID)
                    .put(UUID.fromString("00000000-0000-0000-0000-000000000000"), new ESGNode.ESGZeroNode(sgNodeIdx, MAIN_METHOD_CLASS_NAME, MAIN_METHOD_NAME));

            for(Map.Entry<String, Map<UUID, ProgramVariable>> domainMethodEntry : domain.entrySet()) {
                for(Map.Entry<UUID, ProgramVariable> pVarEntry : domainMethodEntry.getValue().entrySet()) {
                    esgNodes.get(sgNodeIdx).computeIfAbsent(domainMethodEntry.getKey(), k -> Maps.newTreeMap());

                    if(pVarEntry.getValue() instanceof ProgramVariable.ZeroVariable) {
                        esgNodes.get(sgNodeIdx)
                                .get(MAIN_METHOD_ID)
                                .put(UUID.fromString("00000000-0000-0000-0000-000000000000"), new ESGNode.ESGZeroNode(sgNodeIdx, MAIN_METHOD_CLASS_NAME, MAIN_METHOD_NAME));
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
            sb.append(MAIN_METHOD_CLASS_NAME).append(" ");
            sb.append(MAIN_METHOD_NAME).append("\n");

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
        return String.format("%s :: %s", className, methodName);
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
