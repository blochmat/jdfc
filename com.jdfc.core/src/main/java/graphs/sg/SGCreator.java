package graphs.sg;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import data.ClassExecutionData;
import data.MethodData;
import graphs.cfg.CFG;
import graphs.cfg.nodes.CFGEntryNode;
import graphs.cfg.nodes.CFGExitNode;
import graphs.cfg.nodes.CFGNode;
import graphs.sg.nodes.SGCallNode;
import graphs.sg.nodes.SGEntryNode;
import graphs.sg.nodes.SGExitNode;
import graphs.sg.nodes.SGNode;
import org.objectweb.asm.Opcodes;

import java.util.*;

public class SGCreator {

    public static Map<UUID, SG> createSGsForClass(ClassExecutionData cData) {
        Map<UUID, SG> sgs = new HashMap<>();
        for(MethodData mData : cData.getMethods().values()) {
            sgs.put(mData.getId(), SGCreator.createSGForMethod(mData, 0));
        }

        return sgs;
    }

    public static SG createSGForMethod(MethodData mData, int depth) {
        NavigableMap<Integer, SGNode> nodes = Maps.newTreeMap();
        Multimap<Integer, Integer> edges = ArrayListMultimap.create();
        int index = 0;

        CFG cfg = mData.getCfg();
        for(Map.Entry<Integer, CFGNode> nodeEntry : cfg.getNodes().entrySet()) {
            Integer idx = nodeEntry.getKey();
            CFGNode node = nodeEntry.getValue();
            if (node instanceof CFGEntryNode) {
                SGNode n = new SGEntryNode(node);
                nodes.put(index, n);
                index++;
            } else if (node instanceof CFGExitNode) {
                SGNode n = new SGExitNode(node);
                nodes.put(index, n);
                index++;
            } else {
                switch(node.getOpcode()) {
                    case Opcodes.INVOKEVIRTUAL:
                    case Opcodes.INVOKESPECIAL:
                    case Opcodes.INVOKESTATIC:
                    case Opcodes.INVOKEINTERFACE:
                    case Opcodes.INVOKEDYNAMIC:
                        // create call and return site node
                        SGNode callNode = new SGCallNode(node);
//                        SG calledSG = SGCreator.createSGForMethod()
                    default:
                        SGNode n = new SGNode(node);
                        nodes.put(index, n);
                        index++;
                }
            }
        }

        return null;
    }
}