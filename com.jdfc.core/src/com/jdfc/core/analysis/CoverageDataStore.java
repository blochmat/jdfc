package com.jdfc.core.analysis;

import com.jdfc.commons.data.ExecutionDataNode;
import com.jdfc.core.analysis.cfg.CFG;
import com.jdfc.commons.data.ExecutionData;
import com.jdfc.core.analysis.internal.data.ClassExecutionData;
import com.jdfc.core.analysis.internal.data.PackageExecutionData;

import java.io.Serializable;
import java.util.*;

/** A storage singleton for package, class and finally method {@link CFG}s. */
public class CoverageDataStore {

    private static CoverageDataStore singleton;
    private ExecutionDataNode<ExecutionData> root;
    private List<String> classList;

    private CoverageDataStore(){
        PackageExecutionData packageNodeData = new PackageExecutionData();
        this.root = new ExecutionDataNode<>(packageNodeData);
        this.classList = new ArrayList<>();
    }

    public static synchronized CoverageDataStore getInstance(){
        if(singleton == null) {
            singleton =  new CoverageDataStore();
        }
        return singleton;
    }

    public ExecutionDataNode<ExecutionData> getRoot() {
        return root;
    }

    public List<String> getClassList(){
        return classList;
    }

    public void setClassList(List<String> pClassList){
        this.classList = pClassList;
    }

    public void setRoot(ExecutionDataNode<ExecutionData> pNode) {
        this.root = pNode;
    }

    public void setupClassDataNode(String pClassName, Map<String, CFG> pMethodCFGs){
        ClassExecutionData classNodeData = findClassDataNode(pClassName);
        classNodeData.setMethodCFGs(pMethodCFGs);
        classNodeData.calculateDefUsePairs();
    }

    public ClassExecutionData findClassDataNode(String pClassName) {
        ArrayList<String> nodePath = new ArrayList<>(Arrays.asList(pClassName.split("/")));
        ExecutionDataNode<ExecutionData> executionDataNode = root.getChildDataRecursive(nodePath);
        return (ClassExecutionData) executionDataNode.getData();
    }
}
