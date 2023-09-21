package graphs.sg;

import com.google.common.collect.*;
import data.ClassData;
import data.DomainVariable;
import data.MethodData;
import data.ProgramVariable;
import graphs.cfg.CFG;
import graphs.cfg.nodes.CFGCallNode;
import graphs.cfg.nodes.CFGEntryNode;
import graphs.cfg.nodes.CFGExitNode;
import graphs.cfg.nodes.CFGNode;
import graphs.sg.nodes.*;
import lombok.extern.slf4j.Slf4j;
import utils.JDFCUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static utils.Constants.JUMP_OPCODES;

@Slf4j
public class SGCreator {


    public void createSGsForClass(ClassData cData) {
        for(MethodData mData : cData.getMethodDataFromStore().values()) {
            MethodSGCreator methodSGCreator = new MethodSGCreator();
            SG sg = methodSGCreator.createSGForMethod(cData, mData);
            mData.setSg(sg);
        }
    }

    private static class MethodSGCreator {

        Stack<AbstractMap.SimpleImmutableEntry<Integer, CFGNode>> workStack = new Stack<>();
        Stack<String> methodCallStack = new Stack<>();
        Stack<Integer> indexShiftStack = new Stack<>();
        Stack<Integer> sgCallNodeIdxStack = new Stack<>();
        Stack<Integer> sgEntryNodeIdxStack = new Stack<>();

        // setup sg structures for method
        NavigableMap<Integer, SGNode> sgNodes = Maps.newTreeMap();
        Multimap<Integer, Integer> sgEdges = ArrayListMultimap.create();
        // keep track of callers
        Multimap<String, Integer> sgCallersMap = ArrayListMultimap.create();
        // keep track of return sites
        Map<Integer, Integer> sgReturnSiteIndexMap = new HashMap<>();
        Map<String, CFG> cfgMap = new HashMap<>();
        Map<String, Integer> addedNodesSumMap = new HashMap<>();
        List<String> callSequence = new ArrayList<>();
        int index = 0;

        private boolean nextIsEntry(int sgNodeIndex) {
            SGNode sgNode = sgNodes.get(sgNodeIndex + 1);
            return Objects.nonNull(sgNode) && sgNode instanceof SGEntryNode;
        }

        private boolean nextIsReturnSite(int sgNodeIndex) {
            SGNode sgNode = sgNodes.get(sgNodeIndex + 1);
            return Objects.nonNull(sgNode) && sgNode instanceof SGReturnSiteNode;
        }

        public SG createSGForMethod(ClassData cData, MethodData mData) {
            String internalMethodName = mData.buildInternalMethodName();
            this.addCFG(mData);
            this.createNodes(cData);
            this.createEdges();
            this.addPredSuccRelation(sgNodes, sgEdges);

            if(log.isDebugEnabled()) {
                // Log all relative paths of files in the classpath
                File transformFile = JDFCUtils.createFileInDebugDir("5_createSGsForClass.txt", false);
                try (FileWriter writer = new FileWriter(transformFile, true)) {
                    writer.write("Class: " + cData.getClassMetaData().getClassFileRel() + "\n");
                    writer.write("Method: " + internalMethodName);
                    writer.write(JDFCUtils.prettyPrintMap(mData.getLocalVariableTable()));
                    if(mData.getCfg() != null) {
                        writer.write(JDFCUtils.prettyPrintMap(sgNodes));
                        writer.write(JDFCUtils.prettyPrintMultimap(sgEdges));
                    }
                    writer.write("\n");
                    writer.write("\n");
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }
            }

            return new SG(cData.getClassMetaData().getClassFileRel(),
                    internalMethodName,
                    cfgMap,
                    sgNodes,
                    sgEdges,
                    sgReturnSiteIndexMap,
                    sgCallersMap);
        }

        private void createEdges() {
            for (Map.Entry<Integer, SGNode> sgNodeEntry : this.sgNodes.entrySet()) {
                int sgNodeIdx = sgNodeEntry.getKey();
                SGNode sgNode = sgNodeEntry.getValue();
                Collection<Integer> cfgEdgeTargets = cfgMap.get(sgNode.getMethodName()).getEdges().get(sgNode.getCfgIndex());

                // Shift edge targets according to current index and inserted cfg nodes
                if (sgNode instanceof SGCallNode && this.nextIsEntry(sgNodeIdx)) {
                    indexShiftStack.push(sgNodeIdx + 1);
                    sgEdges.put(sgNodeIdx, sgNodeIdx + 1);
                } else if (sgNode instanceof SGExitNode && this.nextIsReturnSite(sgNodeIdx)) {
                    sgEdges.put(sgNodeIdx, sgNodeIdx + 1);
                } else if (sgNode instanceof SGReturnSiteNode) {
                    indexShiftStack.pop();
                    this.updateAddedNodesSumMap(sgNodeIdx);
                    this.sgEdges.put(sgNodeIdx, sgNodeIdx + 1);
                } else if (!JUMP_OPCODES.contains(sgNode.getOpcode())) {
                    Collection<Integer> sgEdgeTargets = this.computeSGEdgeTargets(sgNode, cfgEdgeTargets);
                    sgEdges.putAll(sgNodeIdx, sgEdgeTargets);
                }
            }

            // create jumps
            for (Map.Entry<Integer, SGNode> sgNodeEntry : this.sgNodes.entrySet()) {
                int sgNodeIdx = sgNodeEntry.getKey();
                SGNode sgNode = sgNodeEntry.getValue();
                Collection<Integer> cfgEdgeTargets = cfgMap.get(sgNode.getMethodName()).getEdges().get(sgNode.getCfgIndex());

                if (JUMP_OPCODES.contains(sgNode.getOpcode())) {
                    Collection<Integer> updated = new ArrayList<>();
                    for (Integer cfgEdgeTarget : cfgEdgeTargets) {
                        int jumpDistance = cfgEdgeTarget - sgNode.getCfgIndex();
                        if (jumpDistance > 1) {
                            int jumpTargetIndex = 0;
                            for (int i = 1; i <= jumpDistance; i++) {
                                jumpTargetIndex = sgNodeIdx + i;
                                SGNode jumpTarget = sgNodes.get(jumpTargetIndex);
                                if (jumpTarget instanceof SGEntryNode) {
                                    jumpDistance += cfgMap.get(jumpTarget.getMethodName()).getNodes().size() + 1;
                                }
                            }
                            updated.add(jumpTargetIndex);
                        } else {
                            updated.add(sgNodeIdx + 1);
                        }
                    }
                    sgEdges.putAll(sgNodeIdx, updated);
                }
            }
        }

        private Collection<Integer> computeSGEdgeTargets(SGNode sgNode, Collection<Integer> cfgEdgeTargets) {
            int addedNodesSum = this.getAddedNodesSum(sgNode);
            if (!indexShiftStack.isEmpty()) {
                return cfgEdgeTargets
                        .stream()
                        .map(t -> t + indexShiftStack.peek() + addedNodesSum)
                        .collect(Collectors.toList());
            } else {
                return cfgEdgeTargets
                        .stream()
                        .map(t -> t + addedNodesSum)
                        .collect(Collectors.toList());
            }
        }

        private void updateAddedNodesSumMap(int sgNodeIdx) {
            SGExitNode sgExitNode = (SGExitNode) sgNodes.get(sgNodeIdx - 1);
            SGReturnSiteNode sgReturnSiteNode = (SGReturnSiteNode) sgNodes.get(sgNodeIdx);
            int sum = this.getAddedNodesSum(sgExitNode) + this.getAddedNodesSum(sgReturnSiteNode)+ cfgMap.get(sgExitNode.getMethodName()).getNodes().size() + 1;
            addedNodesSumMap.put(sgReturnSiteNode.getMethodName(), sum);
            addedNodesSumMap.remove(sgExitNode.getMethodName());
        }

        private int getAddedNodesSum(SGNode sgNode) {
            return addedNodesSumMap.get(sgNode.getMethodName()) == null ? 0 : addedNodesSumMap.get(sgNode.getMethodName());
        }

        private void createNodes(ClassData cData) {
            while (!workStack.isEmpty()) {
                AbstractMap.SimpleImmutableEntry<Integer, CFGNode> stackEntry = this.workStack.pop();
                int cfgIndex = stackEntry.getKey();
                CFGNode cfgNode = stackEntry.getValue();
                Collection<Integer> targets = this.getEdgeTargets(cfgIndex, cfgNode.getMethodName());

                if (cfgNode instanceof CFGEntryNode) {
                    SGEntryNode sgEntryNode = new SGEntryNode(index, cfgIndex, cfgNode);
                    this.methodCallStack.push(sgEntryNode.getMethodName());
                    if (!sgCallNodeIdxStack.isEmpty()) {
                        // node is part of subroutine
                        this.sgEntryNodeIdxStack.push(index);
                        SGCallNode sgCallNode = (SGCallNode) sgNodes.get(sgCallNodeIdxStack.peek());
                        sgEntryNode.setPVarMap(sgCallNode.getPVarMap());
                        sgEntryNode.setDVarMap(sgCallNode.getDVarMap().inverse());
                    }
                    this.addSGNode(sgEntryNode, targets);
                } else if (cfgNode instanceof CFGExitNode) {
                    SGExitNode sgExitNode = new SGExitNode(index, cfgIndex, cfgNode);
                    methodCallStack.pop();
                    if (!sgEntryNodeIdxStack.isEmpty()) {
                        // node is part of subroutine
                        int sgExitNodeIdx = index;
                        SGEntryNode sgEntryNode = (SGEntryNode) sgNodes.get(sgEntryNodeIdxStack.peek());
                        sgExitNode.setPVarMap(sgEntryNode.getPVarMap());
                        sgExitNode.setDVarMap(sgEntryNode.getDVarMap().inverse());
                        // Connect exit to return site node
                        Collection<Integer> exitTargets = Collections.singletonList(cfgIndex + 1);
                        this.addSGNode(sgExitNode, exitTargets);

                        // Create return site node
                        int sgReturnSiteNodeIdx = index;
                        SGReturnSiteNode sgReturnSiteNode = new SGReturnSiteNode(
                                index,
                                cfgIndex,
                                new CFGNode(
                                        cData.getClassMetaData().getClassNodeName(),
                                        methodCallStack.peek(),
                                        Sets.newLinkedHashSet(),
                                        Sets.newLinkedHashSet(),
                                        Integer.MIN_VALUE,
                                        Integer.MIN_VALUE,
                                        Sets.newLinkedHashSet(),
                                        Sets.newLinkedHashSet(),
                                        cfgNode.getReach(),
                                        cfgNode.getReachOut()));
                        Collection<Integer> returnSiteTargets = Collections.singletonList(index + 1);
                        this.addSGNode(sgReturnSiteNode, returnSiteTargets);

                        int sgCallNodeIdx = sgCallNodeIdxStack.pop();
                        int sgEntryNodeIdx = sgEntryNodeIdxStack.pop();

                        SGCallNode sgCallNode = (SGCallNode) sgNodes.get(sgCallNodeIdx);
                        sgReturnSiteIndexMap.put(sgCallNodeIdx, sgReturnSiteNodeIdx);
                        // Connect call and return site node
//                        sgEdges.put(sgCallNodeIdx, sgReturnSiteNodeIdx);

                        sgCallNode.setEntryNodeIdx(sgEntryNodeIdx);
                        sgCallNode.setExitNodeIdx(sgExitNodeIdx);
                        sgCallNode.setReturnSiteNodeIdx(sgReturnSiteNodeIdx);

                        sgEntryNode.setCallNodeIdx(sgCallNodeIdx);
                        sgEntryNode.setExitNodeIdx(sgExitNodeIdx);
                        sgEntryNode.setReturnSiteNodeIdx(sgReturnSiteNodeIdx);

                        sgExitNode.setCallNodeIdx(sgCallNodeIdx);
                        sgExitNode.setEntryNodeIdx(sgEntryNodeIdx);
                        sgExitNode.setReturnSiteNodeIdx(sgReturnSiteNodeIdx);

                        sgReturnSiteNode.setCallNodeIdx(sgCallNodeIdx);
                        sgReturnSiteNode.setEntryNodeIdx(sgEntryNodeIdx);
                        sgReturnSiteNode.setExitNodeIdx(sgExitNodeIdx);
                    } else {
                        this.addSGNode(sgExitNode, targets);
                    }
                } else if (cfgNode instanceof CFGCallNode) {
                    // Add call node
                    SGCallNode sgCallNode = new SGCallNode(index, cfgIndex, (CFGCallNode) cfgNode);
                    this.sgCallNodeIdxStack.push(index);
                    // Add called method cfg if possible
                    CFGCallNode cfgCallNode = (CFGCallNode) cfgNode;
                    MethodData calledMethodData = cData.getMethodByInternalName(cfgCallNode.getCalledMethodName());
                    String calledMethodName = calledMethodData.buildInternalMethodName();
                    if(Collections.frequency(callSequence, calledMethodName) < 3) {
                        sgCallersMap.put(calledMethodName, index);
                        this.callSequence.add(calledMethodName);
                        this.addCFG(calledMethodData);
                    } else {
                        sgCallNode.setCalledSGPresent(false);
                    }

                    this.addSGNode(sgCallNode, targets);

                    AbstractMap.SimpleImmutableEntry<Integer, CFGNode> nextEntry = this.workStack.peek();
                    if (nextEntry.getValue() instanceof CFGEntryNode) {
                        CFGEntryNode cfgEntryNode = (CFGEntryNode) nextEntry.getValue();
                        Map<Integer, ProgramVariable> pVarsCall = cfgCallNode.getPVarMap();
                        Map<Integer, ProgramVariable> pVarsEntry = cfgEntryNode.getPVarMap();
                        if(pVarsCall != null && pVarsEntry != null) {
                            // Create program variable mapping
                            BiMap<ProgramVariable, ProgramVariable> pVarMap = HashBiMap.create();
                            for (Map.Entry<Integer, ProgramVariable> cEntry : pVarsCall.entrySet()) {
                                pVarMap.put(cEntry.getValue(), pVarsEntry.get(cEntry.getKey()));
                            }

                            // Create domain variable mapping
                            Map<Integer, DomainVariable> dVarsCall = cfgCallNode.getDVarMap();
                            BiMap<DomainVariable, DomainVariable> dVarMap = HashBiMap.create();
                            for (Map.Entry<Integer, DomainVariable> cEntry : dVarsCall.entrySet()) {
                                if (cEntry.getValue().getIndex() != 0) {
                                    dVarMap.put(cEntry.getValue(), calledMethodData.getCfg().getDomain().get(cEntry.getKey()));
                                }
                            }

                            sgCallNode.getPVarMap().putAll(pVarMap);
                            sgCallNode.getDVarMap().putAll(dVarMap);
                        }
                    }
                } else {
                    this.addSGNode(new SGNode(index, cfgIndex, cfgNode), targets);
                }
            }
        }

        private void pushNodes(Map<Integer, CFGNode> cfgNodes) {
            for (int i = cfgNodes.size() - 1; i > -1; i--) {
                workStack.push(new AbstractMap.SimpleImmutableEntry<>(i, cfgNodes.get(i)));
            }
        }

        private void addCFG(MethodData methodData) {
            this.cfgMap.put(methodData.buildInternalMethodName(), methodData.getCfg());
            this.pushNodes(methodData.getCfg().getNodes());
        }

        private void addSGNode(SGNode sgNode, Collection<Integer> targets) {
            sgNodes.put(index, sgNode);
//            List<Integer> edges;
//            if(indexShiftStack.isEmpty()) {
//                edges = targets.stream().map(x -> x + addedNodesSum).collect(Collectors.toList());
//            } else {
//                edges = targets.stream().map(x -> x + indexShiftStack.peek() + addedNodesSum).collect(Collectors.toList());
//            }
//            sgEdges.putAll(index, edges);
            index++;
        }

        private Collection<Integer> getEdgeTargets(int cfgIndex, String internalMethodName) {
            return this.cfgMap.get(internalMethodName).getEdges().get(cfgIndex);
        }

        private void addPredSuccRelation(NavigableMap<Integer, SGNode> nodes, Multimap<Integer, Integer> edges) {
            for (Map.Entry<Integer, Integer> edge : edges.entries()) {
                final SGNode first = nodes.get(edge.getKey());
                final SGNode second = nodes.get(edge.getValue());
                first.getSucc().add(second);
                second.getPred().add(first);
            }
        }
    }
}