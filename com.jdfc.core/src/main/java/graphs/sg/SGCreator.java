package graphs.sg;

import com.google.common.collect.*;
import data.ClassExecutionData;
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

@Slf4j
public class SGCreator {

    public static void createSGsForClass(ClassExecutionData cData) {
        for(MethodData mData : cData.getMethods().values()) {
            mData.setSg(SGCreator.createSGForMethod(cData, mData, new HashMap<>(), 0, new ArrayList<>()));
//            mData.getSg().calculateReachingDefinitions();
//            mData.calculateInterDefUsePairs();
        }
    }

    public static SG createSGForMethod(ClassExecutionData cData,
                                       MethodData mData,
                                       Map<String, CFG> cfgMap,
                                       int startIndex,
                                       List<String> callSequence) {
        String internalMethodName = mData.buildInternalMethodName();
        cfgMap.put(internalMethodName, mData.getCfg());
        NavigableMap<Integer, CFGNode> localCfgNodes = Maps.newTreeMap(mData.getCfg().getNodes());
        Multimap<Integer, Integer> localCfgEdges = ArrayListMultimap.create(mData.getCfg().getEdges());

        NavigableMap<Integer, SGNode> sgNodes = Maps.newTreeMap();
        Multimap<Integer, Integer> sgEdges = ArrayListMultimap.create();

        // returnSite
        Map<SGCallNode, SGReturnSiteNode> sgReturnSiteNodeMap = new HashMap<>();
        Map<Integer, Integer> sgReturnSiteIndexMap = new HashMap<>();

        // callers
        Multimap<String, SGCallNode> sgCallersMap = ArrayListMultimap.create();

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
                sgNodes.put(index + shift, new SGEntryNode(index + shift, cfgNode));
                int finalShift = shift;
                List<Integer> edges = localCfgEdges.get(cfgNodeIdx).stream().map(x -> x + finalShift).collect(Collectors.toList());
                sgEdges.putAll(index + shift, edges);
                index++;
            } else if (cfgNode instanceof CFGExitNode) {
                SGExitNode sgExitNode = new SGExitNode(index + shift, cfgNode);
                sgNodes.put(index + shift, sgExitNode);
                int finalShift = shift;
//                domainVarMap.computeIfAbsent(sgExitNode.getIndex(), k -> HashBiMap.create());
//                domainVarMap.get(sgExitNode.getIndex()).put(cEntry.getValue().getIndex(), cEntry.getKey());
                List<Integer> edges = localCfgEdges.get(cfgNodeIdx).stream().map(x -> x + finalShift).collect(Collectors.toList());
                sgEdges.putAll(index + shift, edges);
                index++;
            } else if (cfgNode instanceof CFGCallNode) {
                CFGCallNode cfgCallNode = (CFGCallNode) cfgNode;
                MethodData calledMethodData = cData.getMethodByInternalName(cfgCallNode.getCalledMethodName());

                if (calledMethodData != null) {
                    // method is defined in same class
                    CFGEntryNode cfgEntryNode = calledMethodData.getCfg().getEntryNode();

                    if (cfgEntryNode != null) {
                        Map<Integer, ProgramVariable> pVarsCall = cfgCallNode.getPVarMap();
                        Map<Integer, ProgramVariable> pVarsEntry = cfgEntryNode.getPVarMap();

                        if(pVarsCall != null && pVarsEntry != null) {
                            // Create program variable mapping
                            BiMap<ProgramVariable, ProgramVariable> pVarMap = HashBiMap.create();
                            for(Map.Entry<Integer, ProgramVariable> cEntry : pVarsCall.entrySet()) {
                                pVarMap.put(cEntry.getValue(), pVarsEntry.get(cEntry.getKey()));
                            }

                            // Create domain variable mapping
                            Map<Integer, DomainVariable> dVarsCall = cfgCallNode.getDVarMap();
                            Map<Integer, DomainVariable> dVarsEntry = cfgEntryNode.getDVarMap();
                            BiMap<DomainVariable, DomainVariable> dVarMap = HashBiMap.create();
                            for(Map.Entry<Integer, DomainVariable> cEntry : dVarsCall.entrySet()) {
                                if(cEntry.getValue().getIndex() != 0) {
                                    JDFCUtils.logThis(JDFCUtils.prettyPrintMap(dVarsCall), "SGCreator_dVarsCall");
                                    JDFCUtils.logThis(JDFCUtils.prettyPrintMap(dVarsEntry), "SGCreator_dVarsEntry");
                                    dVarMap.put(cEntry.getValue(), calledMethodData.getCfg().getDomain().get(cEntry.getKey()));
                                    JDFCUtils.logThis(JDFCUtils.prettyPrintMap(dVarMap), "SGCreator_domainVarMap");
                                }
                            }

                            // Add call node
                            SGCallNode sgCallNode = new SGCallNode(index + shift, (CFGCallNode) cfgNode, pVarMap, dVarMap);
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
                            String calledMethodName = calledMethodData.buildInternalMethodName();

                            if(Collections.frequency(callSequence, calledMethodName) < 3) {
                                sgCallersMap.put(calledMethodData.buildInternalMethodName(), sgCallNode);
                                callSequence.add(calledMethodName);
                                calledSG = SGCreator.createSGForMethod(
                                        cData,
                                        calledMethodData,
                                        cfgMap,
                                        index + shift,
                                        callSequence);
                            } else {
                                sgCallNode.setCalledSGPresent(false);
                            }

                            if (calledSG != null) {
                                // Add all nodes, edges
                                sgNodes.putAll(calledSG.getNodes());
                                sgEdges.putAll(calledSG.getEdges());

                                // Add pVarMap and dVarMap to entryNode
                                SGEntryNode sgEntryNode = (SGEntryNode)  sgNodes.get(index + shift);
                                sgEntryNode.setPVarMap(pVarMap);
                                sgEntryNode.setDVarMap(dVarMap.inverse());

                                // Update index shift
                                shift = shift + calledSG.getNodes().size();

                                // Add pVarMap and dVarMap to exit node
                                SGExitNode sgExitNode = (SGExitNode) sgNodes.get(index + shift - 1);
                                sgExitNode.setPVarMap(pVarMap);
                                sgExitNode.setDVarMap(dVarMap.inverse());

                                // Add returnSite node
                                SGReturnSiteNode returnSiteNode = new SGReturnSiteNode(
                                        index + shift,
                                        new CFGNode(
                                                cData.getRelativePath(),
                                                internalMethodName,
                                                Integer.MIN_VALUE,
                                                Integer.MIN_VALUE));
                                sgNodes.put(index + shift, returnSiteNode);
                                sgReturnSiteNodeMap.put(sgCallNode, returnSiteNode);
                                sgReturnSiteIndexMap.put(sgCallNodeIdx, index + shift);

                                // Connect exit and return site node
                                sgEdges.put(index + shift - 1, index + shift);

                                // Connect call and return site node
//                                sgEdges.put(sgCallNodeIdx, index + shift);

                                // Connect return site node with next node
                                sgEdges.put(index + shift, index + shift + 1);
                                shift++;
                            }
                        }
                    }
                } else {
                    // Add call node
                    SGCallNode sgCallNode = new SGCallNode(index + shift, (CFGCallNode) cfgNode);
                    sgNodes.put(index + shift, sgCallNode);
                    int finalShift = shift;
                    List<Integer> edges = localCfgEdges.get(cfgNodeIdx).stream().map(x -> x + finalShift).collect(Collectors.toList());
                    sgEdges.putAll(index + shift, edges);
                    index++;
                }
            } else {
                sgNodes.put(index + shift, new SGNode(index + shift, cfgNode));
                int finalShift = shift;
                List<Integer> edges = localCfgEdges.get(cfgNodeIdx).stream().map(x -> x + finalShift).collect(Collectors.toList());
                sgEdges.putAll(index + shift, edges);
                index++;
            }
        }

        SGCreator.addPredSuccRelation(sgNodes, sgEdges);

        if(log.isDebugEnabled()) {
            // Log all relative paths of files in the classpath
            File transformFile = JDFCUtils.createFileInDebugDir("5_createSGsForClass.txt", false);
            try (FileWriter writer = new FileWriter(transformFile, true)) {
                writer.write("Class: " + cData.getRelativePath());
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

        return new SG(
                cData.getRelativePath(),
                internalMethodName,
                cfgMap,
                sgNodes,
                sgEdges,
                sgReturnSiteNodeMap,
                sgReturnSiteIndexMap,
                sgCallersMap);
    }

    private static void addPredSuccRelation(NavigableMap<Integer, SGNode> nodes, Multimap<Integer, Integer> edges) {
        for (Map.Entry<Integer, Integer> edge : edges.entries()) {
            final SGNode first = nodes.get(edge.getKey());
            final SGNode second = nodes.get(edge.getValue());
            first.getSucc().add(second);
            second.getPred().add(first);
        }
    }
}