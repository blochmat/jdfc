package graphs.sg;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import data.ClassExecutionData;
import data.MethodData;
import data.ProgramVariable;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.stream.Collectors;

@Slf4j
public class SGCreator {

    public static void createSGsForClass(ClassExecutionData cData) {
        for(MethodData mData : cData.getMethods().values()) {
            mData.setSg(SGCreator.createSGForMethod(cData, mData, 0, 0));
            mData.getSg().calculateReachingDefinitions();
            mData.calculateInterDefUsePairs();
        }
    }

    public static SG createSGForMethod(ClassExecutionData cData, MethodData mData, int startIndex, int depth) {
        String internalMethodName = mData.buildInternalMethodName();
        NavigableMap<Integer, CFGNode> localCfgNodes = Maps.newTreeMap(mData.getCfg().getNodes());
        Multimap<Integer, Integer> localCfgEdges = ArrayListMultimap.create(mData.getCfg().getEdges());

        NavigableMap<Integer, SGNode> sgNodes = Maps.newTreeMap();
        Multimap<Integer, Integer> sgEdges = ArrayListMultimap.create();
        Map<SGCallNode, SGReturnSiteNode> sgCallReturnNodeMap = new HashMap<>();
        Map<Integer, Integer> sgCallReturnIndexMap = new HashMap<>();
        Map<SGCallNode, String> sgCallNodeMethodMap = new HashMap<>();
        Multimap<String, SGCallNode> sgMethodCallNodesMap = ArrayListMultimap.create();

        int index = startIndex; // increase for node from own cfg
        int shift = 0; // increase for node from other cfg

        if (index != 0) {
            // we are in a called procedure and need to update all indexes according to the start index
            // Copy cfg nodes
            NavigableMap<Integer, CFGNode> tempNodes = Maps.newTreeMap();
            tempNodes.putAll(localCfgNodes);
            localCfgNodes.clear();

            // Put cfg nodes with updated sg index
            for(Map.Entry<Integer, CFGNode> entry : tempNodes.entrySet()) {
                localCfgNodes.put(entry.getKey()+index, entry.getValue());
            }

            // Copy edges
            Multimap<Integer, Integer> tempEdges = ArrayListMultimap.create();
            tempEdges.putAll(localCfgEdges);
            localCfgEdges.clear();

            // Put edges
            for(Map.Entry<Integer, Integer> entry : tempEdges.entries()) {
                localCfgEdges.put(entry.getKey()+index, entry.getValue()+index);
            }
        }

        // iterate though local cfg and create according sg nodes
        for(Map.Entry<Integer, CFGNode> nodeEntry : localCfgNodes.entrySet()) {
            Integer cfgNodeIdx = nodeEntry.getKey();
            CFGNode cfgNode = nodeEntry.getValue();

            if (cfgNode instanceof CFGEntryNode) {
                sgNodes.put(index + shift, new SGEntryNode(internalMethodName, cfgNode));
                int finalShift = shift;
                List<Integer> edges = localCfgEdges.get(cfgNodeIdx).stream().map(x -> x + finalShift).collect(Collectors.toList());
                sgEdges.putAll(index + shift, edges);
                index++;
            } else if (cfgNode instanceof CFGExitNode) {
                sgNodes.put(index + shift, new SGExitNode(internalMethodName, cfgNode));
                int finalShift = shift;
                List<Integer> edges = localCfgEdges.get(cfgNodeIdx).stream().map(x -> x + finalShift).collect(Collectors.toList());
                sgEdges.putAll(index + shift, edges);
                index++;
            } else if (cfgNode instanceof CFGCallNode) {
                CFGCallNode cfgCallNode = (CFGCallNode) cfgNode;

                // called method is in class -> insert cfg
                // called method is not in class -> insert only call node

                MethodData calledMethodData = cData.getMethodByShortInternalName(cfgCallNode.getShortInternalMethodName());
                // Is called method defined in another class?
                if (calledMethodData != null) {
                    // Map program variables
                    Map<Integer, ProgramVariable> pVarsCall = cfgCallNode.getPVarArgs();
                    if (pVarsCall == null) {
                        JDFCUtils.logThis("CFG " + calledMethodData.buildInternalMethodName(), "test");
                    }
                    CFGEntryNode entryNode = calledMethodData.getCfg().getEntryNode();
                    if (entryNode == null) {
                        JDFCUtils.logThis("Nodes " + calledMethodData.buildInternalMethodName(), "test");
                    }

                    if (entryNode != null) {
                        Map<Integer, ProgramVariable> pVarsEntry = entryNode.getPVarArgs();
                        Map<ProgramVariable, ProgramVariable> pVarMap = new HashMap<>();
                        for(Map.Entry<Integer, ProgramVariable> cEntry : pVarsCall.entrySet()) {
                            pVarMap.put(cEntry.getValue(), pVarsEntry.get(cEntry.getKey()));
                        }

                        // Add call node
                        SGCallNode sgCallNode = new SGCallNode(calledMethodData.buildInternalMethodName(), cfgNode, pVarMap);
                        sgNodes.put(index + shift, sgCallNode);

                        // Save callNode index
                        int sgCallNodeIdx = index + shift;

                        // Connect call and entry node
                        int finalShift = shift;
                        List<Integer> edges = localCfgEdges.get(cfgNodeIdx).stream().map(x -> x + finalShift).collect(Collectors.toList());
                        sgEdges.putAll(index + shift, edges);
                        index++;

                        // Create sg for called procedure
                        SG calledSG = null;
                        if(depth < 2) {
                            sgMethodCallNodesMap.put(calledMethodData.buildInternalMethodName(), sgCallNode);
                            calledSG = SGCreator.createSGForMethod(cData, calledMethodData, index + shift, ++depth);
                        }
//                        else {
//                            sgMethodCallNodesMap.put(calledMethodData.buildInternalMethodName(), sgCallNode);
//                            calledSG = SGCreator.createSGForMethod(cData, calledMethodData, index, 0);
//                        }
                        if (calledSG != null) {
                            // Add all nodes, edges
                            sgNodes.putAll(calledSG.getNodes());
                            sgEdges.putAll(calledSG.getEdges());

                            // Update index shift
                            shift = shift + calledSG.getNodes().size();

                            // Connect exit and return site node
                            sgEdges.put(index + shift - 1, index + shift);

                            // Add return node
                            SGReturnSiteNode sgReturnSiteNode = new SGReturnSiteNode(internalMethodName,
                                    new CFGNode(Integer.MIN_VALUE, Integer.MIN_VALUE),
                                    pVarMap);
                            sgNodes.put(index + shift, sgReturnSiteNode);
                            sgCallReturnNodeMap.put(sgCallNode, sgReturnSiteNode);
                            sgCallReturnIndexMap.put(sgCallNodeIdx, index + shift);
                            // Connect return site node with next node
                            sgEdges.put(index + shift, index + shift + 1);
                            shift++;
                        }
                    }
                } else {
                    // Add call node
                    SGCallNode sgCallNode = new SGCallNode(cfgCallNode.getShortInternalMethodName(), cfgNode);
                    sgNodes.put(index + shift, sgCallNode);
                    int finalShift = shift;
                    List<Integer> edges = localCfgEdges.get(cfgNodeIdx).stream().map(x -> x + finalShift).collect(Collectors.toList());
                    sgEdges.putAll(index + shift, edges);
                    index++;
                }
            } else {
                sgNodes.put(index + shift, new SGNode(internalMethodName, cfgNode));
                int finalShift = shift;
                List<Integer> edges = localCfgEdges.get(cfgNodeIdx).stream().map(x -> x + finalShift).collect(Collectors.toList());
                sgEdges.putAll(index + shift, edges);
                index++;
            }
        }

        if(log.isDebugEnabled()) {
            // Log all relative paths of files in the classpath
            File transformFile = JDFCUtils.createFileInDebugDir("5_createSGsForClass.txt", false);
            try (FileWriter writer = new FileWriter(transformFile, true)) {
                writer.write("Class: " + cData.getRelativePath());
                writer.write("Method: " + mData.buildInternalMethodName());
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

        SGCreator.addPredSuccRelation(cData.getRelativePath(), mData.buildInternalMethodName(), sgNodes, sgEdges);
        return new SGImpl(internalMethodName, sgNodes, sgEdges);
    }

    public static void addPredSuccRelation(String className, String methodName, NavigableMap<Integer, SGNode> nodes, Multimap<Integer, Integer> edges) {
        JDFCUtils.logThis(className+ " " + methodName+ "\n", "predSucc_edges");
        for (Map.Entry<Integer, Integer> edge : edges.entries()) {
            JDFCUtils.logThis(edge.getKey() + " " + edge.getValue() + "\n", "predSucc_edges");
            final SGNode first = nodes.get(edge.getKey());
            final SGNode second = nodes.get(edge.getValue());
            first.addSuccessor(second);
            second.addPredecessor(first);
        }
    }
}