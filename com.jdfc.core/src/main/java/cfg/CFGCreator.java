package cfg;

import cfg.data.LocalVariable;
import cfg.visitors.classVisitors.CFGLocalVariableClassVisitor;
import cfg.visitors.classVisitors.CFGNodeClassVisitor;
import com.google.common.base.Preconditions;
import data.ClassExecutionData;
import data.ProgramVariable;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Creates {@link CFG}s for each method of a class file.
 */
public class CFGCreator {

    private static final Logger logger = LoggerFactory.getLogger(CFGCreator.class);

    private CFGCreator() {
    }

    /**
     * Creates the {@link CFG}s for each method of a class and returns a map of method name to {@link
     * CFG}.
     *
     * <p>The key of the map is in the format
     *
     * <pre>
     *   name: descriptor[; signature][; exceptions]
     * </pre>
     * <p>
     * Hence, a default constructor will have the key <code>&lt;init&gt;: ()V</code> and a method
     * "foo" that takes an int and a String and returns a double array will have the key <code> foo:
     * (ILjava/lang/String;)[D</code>.
     *
     * <p>This method is the only method to start the generation of {@link CFG}s. The full creation
     * process of the graphs is then done internally and only the final graphs will be given back to
     * the user.
     *
     * @param pClassReader The class reader instance on the class
     * @param pClassNode   An empty class node to start the analysis with
     *
     */
    public static void createICFGsForClass(final ClassReader pClassReader,
                                           final ClassNode pClassNode,
                                           final ClassExecutionData pClassExecutionData) {
        logger.debug("createICFGsForClass");
        Preconditions.checkNotNull(pClassReader, "We need a non-null class reader to generate CFGs from.");
        Preconditions.checkNotNull(pClassNode, "We need a non-null class node to generate CFGs from.");
        Preconditions.checkNotNull(pClassExecutionData,
                "We need a non-null class execution data to generate CFGs from.");

        // Get local variable information for all methods in the class
        final CFGLocalVariableClassVisitor localVariableVisitor = new CFGLocalVariableClassVisitor(pClassNode, pClassExecutionData);
        pClassReader.accept(localVariableVisitor, 0);

        final Set<ProgramVariable> fields = localVariableVisitor.getFields();
        final Map<String, Map<Integer, LocalVariable>> localVariableTables =
                localVariableVisitor.getLocalVariableTables();

        final Map<String, CFG> methodICFGs = new HashMap<>();

        // Build CFG for all methods in the class
        final CFGNodeClassVisitor CFGNodeClassVisitor =
                new CFGNodeClassVisitor(pClassNode, pClassExecutionData, methodICFGs, localVariableTables);
        pClassReader.accept(CFGNodeClassVisitor, 0);


        logger.debug("CFG CREATION DONE");

//        // TODO: Connect ICFGS
//        for(Map.Entry<String, CFG> icfgEntry : methodICFGs.entrySet()) {
//            logger.debug(String.format("Connecting %s with called procedures", icfgEntry.getKey()));
//            // for every method
//            Map<Double, CFGNode> nodes = icfgEntry.getValue().getNodes();
//            for(Map.Entry<Double, CFGNode> nodeEntry : nodes.entrySet()) {
//                // check every nodeEntry
//                if (nodeEntry.getValue() instanceof ICFGCallNode) {
//                    logger.debug(String.format("Call nodeEntry found with index %f", nodeEntry.getKey()));
//                    // Context-insensitive
//                    ICFGCallNode cNode = (ICFGCallNode) nodeEntry.getValue();
//                    // find target method
//                    double cNodeIndex = nodeEntry.getKey();
//                    double rNodeIndex = Math.floor(cNodeIndex) + 0.9;
//                    String targetName = cNode.getMethodName();
//                    CFG targetCFG = methodICFGs.get(targetName);
//                    Map<Double, CFGNode> targetNodes = targetCFG.getNodes();
//
//                    // TODO: We need all graphs in one structure in order to build an ICFG
//
//                    // Context-sensitive
////                    // copy all nodes of target procedure into current procedure with updated indices
////                    int n = targetNodes.size();
////                    LinkedList<Double> indexList = JDFCUtils.splitInterval(cNodeIndex, rNodeIndex, n);
////
////                    logger.debug("IndexList");
////                    logger.debug(indexList.toString());
////
////                    // create updated subgraph
////                    NavigableMap<Double, ICFGNode> tempNodes = new TreeMap<>();
////                    // index mapping
////                    Map<Double, Double> indexMap = new HashMap<>();
////                    for(Map.Entry<Double, ICFGNode> targetEntry : targetNodes.entrySet()) {
////                        double index = indexList.pop();
////                        indexMap.put(targetEntry.getKey(), index);
////                        tempNodes.put(index, targetEntry.getValue());
////                    }
////
////                    logger.debug("IndexMap");
////                    logger.debug(JDFCUtils.prettyPrintMap(indexMap));
////
////                    // update edges
////                    Multimap<Double, Double> edges = targetICFG.getEdges();
////                    Multimap<Double, Double> tempEdges = ArrayListMultimap.create();
////                    for(Map.Entry<Double, Double> edgeEntry : edges.entries()) {
////                        Double k = indexMap.get(edgeEntry.getKey());
////                        for(Double x : edges.get(edgeEntry.getKey())) {
////                            tempEdges.put(k, indexMap.get(x));
////                        }
////                    }
////
////                    logger.debug("tempEdges");
////                    logger.debug(JDFCUtils.prettyPrintMultimap(tempEdges));
//                }
//            }
//
//        }
    }
}
