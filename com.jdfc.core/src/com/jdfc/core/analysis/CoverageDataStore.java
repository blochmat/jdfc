package com.jdfc.core.analysis;

import com.jdfc.commons.data.ExecutionDataNode;
import com.jdfc.core.analysis.cfg.CFG;
import com.jdfc.commons.data.ExecutionData;
import com.jdfc.core.analysis.internal.data.ClassExecutionData;
import com.jdfc.core.analysis.internal.data.PackageExecutionData;

import java.io.File;
import java.nio.file.Path;
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

    public void setupClassDataNode(String pClassName, Map<String, CFG> pMethodCFGs){
        ClassExecutionData classNodeData = (ClassExecutionData) findClassDataNode(pClassName).getData();
        classNodeData.setMethodCFGs(pMethodCFGs);
        classNodeData.calculateDefUsePairs();
    }

    public ExecutionDataNode<ExecutionData> findClassDataNode(String pClassName) {
        ArrayList<String> nodePath = new ArrayList<>(Arrays.asList(pClassName.split("/")));
        return root.getChildDataRecursive(nodePath);
    }

    public void addNodesFromDirRecursive(File pFile,
                                         ExecutionDataNode<ExecutionData> pExecutionDataNode,
                                         Path pBaseDir,
                                         String suffix) {
        File[] fileList = Objects.requireNonNull(pFile.listFiles());
        for (File f : fileList){
            if(f.isDirectory()) {
                PackageExecutionData pkgData = new PackageExecutionData();
                ExecutionDataNode<ExecutionData> newPkgExecutionDataNode = new ExecutionDataNode<>(pkgData);
                pExecutionDataNode.addChild(f.getName(), newPkgExecutionDataNode);
                addNodesFromDirRecursive(f, newPkgExecutionDataNode, pBaseDir, suffix);
            } else if (f.isFile() && f.getName().endsWith(suffix)) {
                String relativePath = pBaseDir.relativize(f.toPath()).toString();
                String relativePathWithoutType = relativePath.split("\\.")[0];
                // Add className to classList of storage. Thereby we determine, if class needs to be instrumented
                classList.add(relativePathWithoutType);

                String nameWithoutType = f.getName().split("\\.")[0];
                ClassExecutionData classNodeData = new ClassExecutionData();
                pExecutionDataNode.addChild(nameWithoutType, classNodeData);
            }
        }
    }
}
