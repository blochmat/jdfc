package icfg.visitors.methodVisitors;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.SourceInterpreter;
import org.objectweb.asm.tree.analysis.SourceValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

import static org.objectweb.asm.Opcodes.ASM5;


public class ICFGEdgeAnalysisVisitor extends MethodVisitor {
    private final Logger logger = LoggerFactory.getLogger(ICFGEdgeAnalysisVisitor.class);
    private final MethodNode methodNode;
    private final Set<Double> crNodes;
    private final Multimap<Double, Double> edges;

    ICFGEdgeAnalysisVisitor(final MethodNode pMethodNode, final Set<Double> crNodes) {
        super(ASM5);
        logger.debug(String.format("Visit %s", pMethodNode.name));
        methodNode = pMethodNode;
        this.crNodes = crNodes;
        edges = ArrayListMultimap.create();
    }

    @Override
    public void visitEnd() {
        logger.debug("visitEnd");
        try {
            CFGEdgeAnalyzer cfgEdgeAnalyzer = new CFGEdgeAnalyzer(methodNode, crNodes);
            cfgEdgeAnalyzer.analyze(methodNode.name, methodNode);
            edges.putAll(cfgEdgeAnalyzer.getEdges());
        } catch (AnalyzerException e) {
            logger.debug(e.getMessage());
            e.printStackTrace();
        }
    }

    Multimap<Double, Double> getEdges() {
        return edges;
    }

    private static class CFGEdgeAnalyzer extends Analyzer<SourceValue> {

        private final Logger logger = LoggerFactory.getLogger(CFGEdgeAnalyzer.class);
        private final Set<Double> crNodes;
        private final MethodNode methodNode;
        private final Multimap<Double, Double> edges;

        CFGEdgeAnalyzer(MethodNode methodNode, Set<Double> crNodes) {
            super(new SourceInterpreter());
            this.methodNode = methodNode;
            this.crNodes = crNodes;
            edges = ArrayListMultimap.create();
        }

        Multimap<Double, Double> getEdges() {
            return edges;
        }

        @Override
        protected void newControlFlowEdge(int insnIndex, int successorIndex) {
            // Entry node to first node edge
            if(!edges.containsEntry((double) Integer.MIN_VALUE, (double) insnIndex) && insnIndex == 0) {
                edges.put((double) Integer.MIN_VALUE, (double) insnIndex);
            }

            if (crNodes.contains((double) insnIndex)) {
                // This node is a call node
                edges.put((double) insnIndex + 0.25, (double) insnIndex + 0.75);
                edges.put((double) insnIndex + 0.75, (double) successorIndex);
            } else if (crNodes.contains((double) successorIndex)) {
                // The next node is a call node
                edges.put((double) insnIndex, (double) successorIndex + 0.25);
            } else if (!edges.containsKey((double) insnIndex) || !edges.containsValue((double) successorIndex)) {
                // Normal nodes
                edges.put((double) insnIndex, (double) successorIndex);
            }

            // Connect procedure returns to exit nodes
            int insnOpcode = methodNode.instructions.get(insnIndex).getOpcode();
            int succOpcode = methodNode.instructions.get(successorIndex).getOpcode();
            if (172 <= insnOpcode && insnOpcode <= 177) {
                edges.put((double) insnIndex, (double) Integer.MAX_VALUE);
            }

            if (172 <= succOpcode && succOpcode <= 177) {
                edges.put((double) successorIndex, (double) Integer.MAX_VALUE);
            }

            super.newControlFlowEdge(insnIndex, successorIndex);
        }

        @Override
        protected boolean newControlFlowExceptionEdge(int insnIndex, int successorIndex) {
            if (!edges.containsKey((double) insnIndex) || !edges.containsValue((double) successorIndex)) {
                edges.put((double) insnIndex, (double) successorIndex);
            }
            return super.newControlFlowExceptionEdge(insnIndex, successorIndex);
        }
    }
}
