package com.jdfc.core.analysis;

import com.jdfc.commons.data.Node;
import com.jdfc.core.analysis.cfg.CFG;
import com.jdfc.commons.data.NodeData;
import com.jdfc.core.analysis.internal.data.ClassNodeData;

import java.util.*;

/** A storage singleton for package, class and finally method {@link CFG}s. */
public enum CoverageDataStore {
    INSTANCE;

    private Node<NodeData> root;
    private List<String> classList;

    public Node<NodeData> getRoot() {
        return root;
    }
    public void setRoot(Node<NodeData> root) {
        this.root = root;
    }

    public List<String> getClassList(){
        return classList;
    }

    public void setClassList(List<String> pClassList){
        this.classList = pClassList;
    }

    public void setupClassDataNode(String pClassName, Map<String, CFG> pMethodCFGs){
        ClassNodeData classNodeData = findClassDataNode(pClassName);
        classNodeData.setMethodCFGs(pMethodCFGs);
        classNodeData.calculateDefUsePairs();
    }

    public ClassNodeData findClassDataNode(String pClassName) {
        ArrayList<String> nodePath = new ArrayList<>(Arrays.asList(pClassName.split("/")));
        Node<NodeData> node = root.getChildDataRecursive(nodePath);
        return (ClassNodeData) node.getData();
    }
}
