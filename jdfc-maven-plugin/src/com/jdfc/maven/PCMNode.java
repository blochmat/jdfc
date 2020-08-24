package com.jdfc.maven;

import java.util.HashMap;
import java.util.Map;

public class PCMNode extends ExecutionDataNode{

    private ExecutionDataNodeType type;
    private Map<String, ExecutionDataNode> children;

    public PCMNode(ExecutionDataNodeType pType) {
        super();
        this.type = pType;
        this.children = new HashMap<>();
    }

    public ExecutionDataNodeType getType() {
        return type;
    }

    public Map<String, ExecutionDataNode> getChildren() {
        return children;
    }

    public void setChildren(Map<String, ExecutionDataNode> pChildren){
        this.children = pChildren;
    }
}
