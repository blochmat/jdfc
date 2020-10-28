package com.jdfc.core.analysis.ifg;

import com.jdfc.core.analysis.ifg.data.ProgramVariable;

public class IFGNode extends CFGNode{

    private final int lineNumber;
    private final ProgramVariable methodCaller;
    private final String methodNameDesc;
    private final int parameterCount;
    private CFGNode callNode;
    private CFG relatedCFG;

    IFGNode(final int pIndex,
            final int pLineNumber,
            final ProgramVariable pMethodCaller,
            final String pMethodNameDesc,
            final int pParameterCount) {
        super(pIndex);
        lineNumber = pLineNumber;
        methodCaller = pMethodCaller;
        methodNameDesc = pMethodNameDesc;
        parameterCount = pParameterCount;
    }

    public String getMethodNameDesc() {
        return methodNameDesc;
    }

    public CFGNode getCallNode() {
        return callNode;
    }

    public CFG getRelatedCFG() {
        return relatedCFG;
    }

    public int getParameterCount() {
        return parameterCount;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public ProgramVariable getMethodCaller() {
        return methodCaller;
    }

    public void setupMethodRelation(CFG pRelatedMethod) {
        this.relatedCFG = pRelatedMethod;
        this.callNode = pRelatedMethod.getNodes().firstEntry().getValue();
    }
}
