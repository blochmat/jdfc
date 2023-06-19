package graphs.sg;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import data.ClassExecutionData;
import data.MethodData;
import graphs.cfg.CFG;
import graphs.cfg.nodes.CFGCallNode;
import graphs.cfg.nodes.CFGEntryNode;
import graphs.cfg.nodes.CFGExitNode;
import graphs.cfg.nodes.CFGNode;
import graphs.sg.nodes.*;
import lombok.extern.slf4j.Slf4j;
import utils.JDFCUtils;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class SGCreator {

    public static Map<UUID, SG> createSGsForClass(ClassExecutionData cData) {
        Map<UUID, SG> sgs = new HashMap<>();
        for(MethodData mData : cData.getMethods().values()) {
            sgs.put(mData.getId(), SGCreator.createSGForMethod(cData, mData, 0, 0));
        }

        return sgs;
    }

    public static SG createSGForMethod(ClassExecutionData cData, MethodData mData, int startIndex, int depth) {
        String internalMethodName = mData.buildInternalMethodName();
        NavigableMap<Integer, CFGNode> cfgNodes = mData.getCfg().getNodes();
        Multimap<Integer, Integer> cfgEdges = mData.getCfg().getEdges();

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
            tempNodes.putAll(cfgNodes);
            cfgNodes.clear();

            // Put cfg nods with updated sg index
            for(Map.Entry<Integer, CFGNode> entry : tempNodes.entrySet()) {
                cfgNodes.put(entry.getKey()+index, entry.getValue());
            }

            // Copy edges
            Multimap<Integer, Integer> tempEdges = ArrayListMultimap.create();
            tempEdges.putAll(cfgEdges);
            cfgEdges.clear();

            // Put edges
            for(Map.Entry<Integer, Integer> entry : tempEdges.entries()) {
                cfgEdges.put(entry.getKey()+index, entry.getValue()+index);
            }
        }

        CFG cfg = mData.getCfg();
        for(Map.Entry<Integer, CFGNode> nodeEntry : cfg.getNodes().entrySet()) {
            Integer cfgNodeIdx = nodeEntry.getKey();
            CFGNode cfgNode = nodeEntry.getValue();
            if (cfgNode instanceof CFGEntryNode) {
                sgNodes.put(index + shift, new SGEntryNode(internalMethodName, cfgNode));
                int finalShift = shift;
                List<Integer> edges = cfg.getEdges().get(cfgNodeIdx).stream().map(x -> x + finalShift).collect(Collectors.toList());
                sgEdges.putAll(index + shift, edges);
                index++;
            } else if (cfgNode instanceof CFGExitNode) {
                sgNodes.put(index + shift, new SGExitNode(internalMethodName, cfgNode));
                int finalShift = shift;
                List<Integer> edges = cfg.getEdges().get(cfgNodeIdx).stream().map(x -> x + finalShift).collect(Collectors.toList());
                sgEdges.putAll(index + shift, edges);
                index++;
            } else if (cfgNode instanceof CFGCallNode) {
                // Add call node
                CFGCallNode cfgCallNode = (CFGCallNode) cfgNode;
                MethodData calledMethodData = cData.getMethodByShortInternalName(cfgCallNode.getShortInternalMethodName());
                SGCallNode sgCallNode = new SGCallNode(calledMethodData.buildInternalMethodName(), cfgNode);
                sgNodes.put(index + shift, sgCallNode);

                // Save callNode index
                int sgCallNodeIdx = index + shift;

                // Connect call and entry node
                int finalShift = shift;
                List<Integer> edges = cfg.getEdges().get(cfgNodeIdx).stream().map(x -> x + finalShift).collect(Collectors.toList());
                sgEdges.putAll(index + shift, edges);
                index++;

                // Create sg for called procedure
                SG calledSG;
//                if(mData.getId().equals(calledMethodData.getId()) && depth < 2) {
                    sgMethodCallNodesMap.put(calledMethodData.buildInternalMethodName(), sgCallNode);
                    calledSG = SGCreator.createSGForMethod(cData, calledMethodData, index, depth++);
//                } else {
//                    sgMethodCallNodesMap.put(calledMethodData.buildInternalMethodName(), sgCallNode);
//                    calledSG = SGCreator.createSGForMethod(cData, calledMethodData, index, 0);
//                }

                // Add all nodes, edges
                sgNodes.putAll(calledSG.getNodes());
                sgEdges.putAll(calledSG.getEdges());

                // Update index shift
                shift = calledSG.getNodes().size();

                // Connect exit and return site node
                sgEdges.put(index + shift - 1, index + shift);

                // Add return node
                SGReturnSiteNode sgReturnSiteNode = new SGReturnSiteNode(internalMethodName, new CFGNode(Integer.MIN_VALUE, Integer.MIN_VALUE));
                sgNodes.put(index + shift, sgReturnSiteNode);
                sgCallReturnNodeMap.put(sgCallNode, sgReturnSiteNode);
                sgCallReturnIndexMap.put(sgCallNodeIdx, index + shift);
                // Connect return site node with next node
                sgEdges.put(index + shift, index + shift + 1);
                shift++;

                // Todo: match parameters


            } else {
                sgNodes.put(index + shift, new SGNode(internalMethodName, cfgNode));
                int finalShift = shift;
                List<Integer> edges = cfg.getEdges().get(cfgNodeIdx).stream().map(x -> x + finalShift).collect(Collectors.toList());
                sgEdges.putAll(index + shift, edges);
                index++;
            }
        }

        JDFCUtils.logThis(internalMethodName + "\n" + JDFCUtils.prettyPrintMap(sgNodes), "nodes");
        JDFCUtils.logThis(internalMethodName + "\n" + JDFCUtils.prettyPrintMultimap(sgEdges), "edges");

        return new SGImpl(internalMethodName, sgNodes, sgEdges);
    }
}