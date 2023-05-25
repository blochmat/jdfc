package cfg.visitors.methodVisitors;

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

import static org.objectweb.asm.Opcodes.ASM5;


public class CFGEdgeAnalysisVisitor extends MethodVisitor {
    private final Logger logger = LoggerFactory.getLogger(CFGEdgeAnalysisVisitor.class);
    private final MethodNode methodNode;
    private final Multimap<Double, Double> edges;

    CFGEdgeAnalysisVisitor(final MethodNode pMethodNode) {
        super(ASM5);
        logger.debug(String.format("Visit %s", pMethodNode.name));
        methodNode = pMethodNode;
        edges = ArrayListMultimap.create();
    }

    @Override
    public void visitEnd() {
        logger.debug("visitEnd");
        try {
            CFGEdgeAnalyzer cfgEdgeAnalyzer = new CFGEdgeAnalyzer();
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
        private final Multimap<Double, Double> edges;

        CFGEdgeAnalyzer() {
            super(new SourceInterpreter());
            edges = ArrayListMultimap.create();
        }

        Multimap<Double, Double> getEdges() {
            return edges;
        }

        @Override
        protected void newControlFlowEdge(int insnIndex, int successorIndex) {
            if (!edges.containsKey((double) insnIndex) || !edges.containsValue((double) successorIndex)) {
                // Normal nodes
                edges.put((double) insnIndex, (double) successorIndex);
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
