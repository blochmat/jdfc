package icfg;

import icfg.data.ProgramVariable;
import icfg.nodes.ICFGNode;

public class IFGNode extends ICFGNode {

    private final String methodOwner;
    private final int lineNumber;
    private final ProgramVariable methodCaller;
    private final String methodNameDesc;
    private final int parameterCount;
    private ICFGNode relatedCallSiteNode;
    private CFG relatedCFG;

    public int getOpcode() {
        return super.getOpcode();
    }

    public String getMethodOwner() {
        return methodOwner;
    }

    IFGNode(final int pIndex,
            final int pLineNumber,
            final int pOpcode,
            final String pMethodOwner,
            final ProgramVariable pMethodCaller,
            final String pMethodNameDesc,
            final int pParameterCount) {
        super(pIndex, pOpcode);
        lineNumber = pLineNumber;
        methodOwner = pMethodOwner;
        methodCaller = pMethodCaller;
        methodNameDesc = pMethodNameDesc;
        parameterCount = pParameterCount;
    }

    public String getMethodNameDesc() {
        return methodNameDesc;
    }

    public ICFGNode getRelatedCallSiteNode() {
        return relatedCallSiteNode;
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

    public void setupMethodRelation(CFG pRelatedMethod) {
        this.relatedCFG = pRelatedMethod;
        if(pRelatedMethod.getNodes().isEmpty()) {
            this.relatedCallSiteNode = null;
        } else {
            this.relatedCallSiteNode = pRelatedMethod.getNodes().firstEntry().getValue();
        }

    }

    public String toString() {
        return String.format(
                "IFG Node: %d %d (%d predecessors, %d successors), \n" +
                        "   Method Owner: %s, \n" +
                        "   LineNumber: %s, \n" +
                        "   Caller: %s, \n" +
                        "   MethodNameDesc: %s, \n" +
                        "   ParameterCount: %s, \n" +
                        "   Call Node: %s, \n" +
                        "   Related CFG: %s",
                getIndex(), getOpcode(), getPredecessors().size(), getSuccessors().size(),
                methodOwner, lineNumber, methodCaller, methodNameDesc,
                parameterCount, relatedCallSiteNode, relatedCFG);
    }
}
