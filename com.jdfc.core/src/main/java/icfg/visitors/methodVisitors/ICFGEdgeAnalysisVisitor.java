package icfg.visitors.methodVisitors;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.SourceInterpreter;
import org.objectweb.asm.tree.analysis.SourceValue;

import java.util.Map;

import static org.objectweb.asm.Opcodes.ASM5;


public class ICFGEdgeAnalysisVisitor extends MethodVisitor {

    private final MethodNode methodNode;
    private final Multimap<Double, Double> edges;

    ICFGEdgeAnalysisVisitor(final MethodNode pMethodNode) {
        super(ASM5);
        methodNode = pMethodNode;
        edges = ArrayListMultimap.create();
    }

    @Override
    public void visitEnd() {
        try {
            CFGEdgeAnalyzer cfgEdgeAnalyzer = new CFGEdgeAnalyzer();
            cfgEdgeAnalyzer.analyze(methodNode.name, methodNode);
            edges.putAll(cfgEdgeAnalyzer.getEdges());

            System.out.printf("Method: %s%n", methodNode.name);
            for (Map.Entry<Double, Double> edge : edges.entries()){
                System.out.printf("Edge: (%f,%f)%n", edge.getKey(), edge.getValue());
            }
        } catch (AnalyzerException e) {
            e.printStackTrace();
        }
    }

    Multimap<Double, Double> getEdges() {
        return edges;
    }

    private static class CFGEdgeAnalyzer extends Analyzer<SourceValue> {

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
