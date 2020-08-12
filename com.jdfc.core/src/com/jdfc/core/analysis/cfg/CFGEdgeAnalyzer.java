package com.jdfc.core.analysis.cfg;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.SourceInterpreter;
import org.objectweb.asm.tree.analysis.SourceValue;

public class CFGEdgeAnalyzer extends Analyzer<SourceValue> {

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
