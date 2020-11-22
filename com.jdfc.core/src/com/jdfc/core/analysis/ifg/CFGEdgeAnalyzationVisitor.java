package com.jdfc.core.analysis.ifg;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.SourceInterpreter;
import org.objectweb.asm.tree.analysis.SourceValue;

import static org.objectweb.asm.Opcodes.ASM5;


public class CFGEdgeAnalyzationVisitor extends MethodVisitor {

    private final String owner;
    private final MethodNode methodNode;
    private final Multimap<Integer, Integer> edges;

    CFGEdgeAnalyzationVisitor(final String pOwner, final MethodNode pMethodNode) {
        super(ASM5);
        owner = pOwner;
        methodNode = pMethodNode;
        edges = ArrayListMultimap.create();
    }

    @Override
    public void visitEnd() {
        try {
            CFGEdgeAnalyzer cfgEdgeAnalyzer = new CFGEdgeAnalyzer();
            cfgEdgeAnalyzer.analyze(methodNode.name, methodNode);
            edges.putAll(cfgEdgeAnalyzer.getEdges());
        } catch (AnalyzerException e) {
            e.printStackTrace();
        }
    }

    Multimap<Integer, Integer> getEdges() {
        return edges;
    }

    private static class CFGEdgeAnalyzer extends Analyzer<SourceValue> {

        private final Multimap<Integer, Integer> edges;

        CFGEdgeAnalyzer() {
            super(new SourceInterpreter());
            edges = ArrayListMultimap.create();
        }

        Multimap<Integer, Integer> getEdges() {
            return edges;
        }

        @Override
        protected void newControlFlowEdge(int insnIndex, int successorIndex) {
            if (!edges.containsKey(insnIndex) || !edges.containsValue(successorIndex)) {
                edges.put(insnIndex, successorIndex);
            }
            super.newControlFlowEdge(insnIndex, successorIndex);
        }

        @Override
        protected boolean newControlFlowExceptionEdge(int insnIndex, int successorIndex) {
            if (!edges.containsKey(insnIndex) || !edges.containsValue(successorIndex)) {
                edges.put(insnIndex, successorIndex);
            }
            return super.newControlFlowExceptionEdge(insnIndex, successorIndex);
        }
    }
}
