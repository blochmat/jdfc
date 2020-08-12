package com.jdfc.core.analysis.cfg;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.AnalyzerException;

import static org.objectweb.asm.Opcodes.ASM6;

public class CFGEdgeAnalyzationVisitor extends MethodVisitor {

    private final String owner;
    private final MethodNode methodNode;
    private final Multimap<Integer, Integer> edges;

    CFGEdgeAnalyzationVisitor(final String pOwner, final MethodNode pMethodNode) {
        super(ASM6);
        owner = pOwner;
        methodNode = pMethodNode;
        edges = ArrayListMultimap.create();
    }

    @Override
    public void visitEnd() {
        try {
            CFGEdgeAnalyzer cfgEdgeAnalyzer = new CFGEdgeAnalyzer();
            cfgEdgeAnalyzer.analyze(owner, methodNode);
            edges.putAll(cfgEdgeAnalyzer.getEdges());
        } catch (AnalyzerException e) {
            e.printStackTrace();
        }
    }

    Multimap<Integer, Integer> getEdges() {
        return edges;
    }
}
