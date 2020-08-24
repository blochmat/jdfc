package com.jdfc.maven;

public class ExecutionDataTree {

    private PCMNode root;

    public ExecutionDataTree(){
        this.root = new PCMNode(ExecutionDataNodeType.ROOT);
    }

    public PCMNode getRoot() {
        return root;
    }
}
