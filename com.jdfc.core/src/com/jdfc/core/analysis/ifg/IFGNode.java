package com.jdfc.core.analysis.ifg;

import com.jdfc.commons.data.Pair;

import java.util.Set;

public class IFGNode extends CFGNode{

    private final String methodNameDesc;
    private CFGNode callNode;
    private CFGNode returnNode;
    private int parameterCount;

    IFGNode(int pIndex, String pMethodNameDesc, int pParameterCount) {
        super(pIndex);
        methodNameDesc = pMethodNameDesc;
        parameterCount = pParameterCount;
    }

    public String getMethodNameDesc() {
        return methodNameDesc;
    }

    public CFGNode getCallNode() {
        return callNode;
    }

    public void setCallNode(CFGNode callNode) {
        this.callNode = callNode;
    }

    public CFGNode getReturnNode() {
        return returnNode;
    }

    public void setReturnNode(CFGNode returnNode) {
        this.returnNode = returnNode;
    }

    public int getParameterCount() {
        return parameterCount;
    }
}
