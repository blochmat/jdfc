package com.jdfc.core.analysis.ifg;

import java.util.Set;

public class IFGNode extends CFGNode{

    private CFGNode callNode;
    private CFGNode returnNode;

    IFGNode(int pIndex) {
        super(pIndex);
    }

    IFGNode(Set<ProgramVariable> pDefinitions, Set<ProgramVariable> pUses, int pIndex) {
        super(pDefinitions, pUses, pIndex);
    }

    IFGNode(Set<ProgramVariable> pDefinitions, Set<ProgramVariable> pUses, int pIndex, Set<CFGNode> pPredecessors, Set<CFGNode> pSuccessors) {
        super(pDefinitions, pUses, pIndex, pPredecessors, pSuccessors);
    }


}
