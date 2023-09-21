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

        Stack<AbstractMap.SimpleImmutableEntry<Integer, CFGNode>> nodeStack = new Stack<>();
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
        List<String> callSequence = new ArrayList<>();
        int index = 0;
        int addedNodesSum = 0;

        public SG createSGForMethod(ClassData cData, MethodData mData) {

            // Put current cfg into map and copy nodes and edges
            String internalMethodName = mData.buildInternalMethodName();
            this.addCFG(mData);

//        NavigableMap<Integer, CFGNode> cfgNodesCopy = Maps.newTreeMap(mData.getCfg().getNodes());
//        Multimap<Integer, Integer> cfgEdgesCopy = ArrayListMultimap.create(mData.getCfg().getEdges());
//        if (index != 0) {
//            // we are in a called procedure and need to update all indexes according to the start index
//            this.updateCFGIndices(index, cfgNodesCopy, cfgEdgesCopy);
//        }

            // create nodes
            while (!nodeStack.isEmpty()) {
                AbstractMap.SimpleImmutableEntry<Integer, CFGNode> stackEntry = this.nodeStack.pop();
                int cfgIndex = stackEntry.getKey();
                CFGNode cfgNode = stackEntry.getValue();
                Collection<Integer> targets = this.getEdgeTargets(cfgIndex, cfgNode.getMethodName());

                if (cfgNode instanceof CFGEntryNode) {
                    SGEntryNode sgEntryNode = new SGEntryNode(index, cfgNode);
                    if (!sgCallNodeIdxStack.isEmpty()) {
                        // node is part of subroutine
                        this.sgEntryNodeIdxStack.push(index);
                        SGCallNode sgCallNode = (SGCallNode) sgNodes.get(sgCallNodeIdxStack.peek());
                        sgEntryNode.setPVarMap(sgCallNode.getPVarMap());
                        sgEntryNode.setDVarMap(sgCallNode.getDVarMap().inverse());
                    }
                    this.addSGNode(sgEntryNode, targets);
                } else if (cfgNode instanceof CFGExitNode) {
                    SGExitNode sgExitNode = new SGExitNode(index, cfgNode);
                    if (!sgEntryNodeIdxStack.isEmpty()) {
                        // node is part of subroutine
                        int sgExitNodeIdx = index;
                        SGEntryNode sgEntryNode = (SGEntryNode) sgNodes.get(sgEntryNodeIdxStack.peek());
                        sgExitNode.setPVarMap(sgEntryNode.getPVarMap());
                        sgExitNode.setDVarMap(sgEntryNode.getDVarMap().inverse());
                        // Connect exit to return site node
                        Collection<Integer> exitTargets = Collections.singletonList(cfgIndex + 1);
                        this.addSGNode(sgExitNode, exitTargets);
                        indexShiftStack.pop();

                        // Create return site node
                        int sgReturnSiteNodeIdx = index;
                        SGReturnSiteNode sgReturnSiteNode = new SGReturnSiteNode(
                                index,
                                new CFGNode(
                                        cData.getClassMetaData().getClassNodeName(),
                                        internalMethodName,
                                        Sets.newLinkedHashSet(),
                                        Sets.newLinkedHashSet(),
                                        Integer.MIN_VALUE,
                                        Integer.MIN_VALUE,
                                        Sets.newLinkedHashSet(),
                                        Sets.newLinkedHashSet(),
                                        cfgNode.getReach(),
                                        cfgNode.getReachOut()));
                        Collection<Integer> returnSiteTargets = Collections.singletonList(index + 1);
                        this.addSGNode(new SGReturnSiteNode(index, cfgNode), returnSiteTargets);
                        this.addedNodesSum = addedNodesSum + (index - sgEntryNodeIdxStack.peek());

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
                    SGCallNode sgCallNode = new SGCallNode(index, (CFGCallNode) cfgNode);
                    this.sgCallNodeIdxStack.push(index);
                    // Add called method cfg if possible
                    CFGCallNode cfgCallNode = (CFGCallNode) cfgNode;
                    MethodData calledMethodData = cData.getMethodByInternalName(cfgCallNode.getCalledMethodName());
                    String calledMethodName = calledMethodData.buildInternalMethodName();
                    if(Collections.frequency(callSequence, calledMethodName) < 3) {
                        sgCallersMap.put(calledMethodData.buildInternalMethodName(), index);
                        this.callSequence.add(calledMethodName);
                        this.addCFG(calledMethodData);
                    } else {
                        sgCallNode.setCalledSGPresent(false);
                    }

                    this.addSGNode(sgCallNode, targets);
                    this.indexShiftStack.push(index);

                    AbstractMap.SimpleImmutableEntry<Integer, CFGNode> pop = this.nodeStack.peek();
                    CFGEntryNode cfgEntryNode = (CFGEntryNode) pop.getValue();
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
                } else {
                    this.addSGNode(new SGNode(index, cfgNode), targets);
                }
            }

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

        private void pushNodes(Map<Integer, CFGNode> cfgNodes) {
            for (int i = cfgNodes.size() - 1; i > -1; i--) {
                nodeStack.push(new AbstractMap.SimpleImmutableEntry<>(i, cfgNodes.get(i)));
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